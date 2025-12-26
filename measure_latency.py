import asyncio
import json
import time
import sys
import websockets

# Configuration
WS_URI = "ws://localhost:8080/ws"
SYMBOL = "AAPL"
COUNT = 100

async def measure_latency():
    print(f"Connecting to {WS_URI}...")
    try:
        # Note: Standard websockets lib doesn't support SockJS/STOMP easily out of the box.
        # However, for simple benchmarking, we might need a STOMP client or use a simpler WS connection 
        # if the server supported raw WS. 
        # Since we used SockJS/STOMP in Phase 7, we need a STOMP-compatible handshake.
        # Writing a full STOMP client in python for this snippet is complex.
        # ALTERNATIVE: Use the REST API if we had one for streaming, but we don't.
        #
        # Simplified approach: We will act as a raw websocket client attempting to handshake 
        # OR we'll just check the REST snapshot latency (simpler for now) as a proxy, 
        # BUT the task said "Publisher -> Client" which usually implies streaming.
        #
        # Let's try to do a minimal STOMP over WS handshake.
        async with websockets.connect(f"{WS_URI}/websocket") as websocket:
            # STOMP CONNECT
            connect_frame = "CONNECT\naccept-version:1.1,1.0\nheart-beat:0,0\n\n\0"
            await websocket.send(connect_frame)
            connected = await websocket.recv()
            if "CONNECTED" not in connected:
                print("Failed to connect STOMP")
                return

            # STOMP SUBSCRIBE
            sub_id = "sub-0"
            subscribe_frame = f"SUBSCRIBE\nid:{sub_id}\ndestination:/topic/market-data/{SYMBOL}\n\n\0"
            await websocket.send(subscribe_frame)

            print(f"Subscribed to {SYMBOL}. Measuring latency for {COUNT} messages...")
            
            latencies = []
            
            for _ in range(COUNT):
                msg = await websocket.recv()
                # Message is a STOMP frame. Body is after empty line.
                try:
                    head, body = msg.split("\n\n", 1)
                    body = body.rstrip('\0')
                    if not body: continue 
                    
                    data = json.loads(body)
                    
                    # Timestamps are strings in our new GraphQL/DTO schema, but let's see what JSON sends.
                    # DTO has String timestamp.
                    ts_server_str = data.get("timestamp")
                    if not ts_server_str: continue
                    
                    ts_server = int(ts_server_str)
                    ts_now = int(time.time() * 1000) # ms
                    
                    # Latency = Arrival Time - Ingest Time (approx, assuming clock sync or single machine)
                    # Note: Server timestamp is set when aggregating.
                    latency = ts_now - ts_server
                    latencies.append(latency)
                    
                    sys.stdout.write(f"\rCaptured: {len(latencies)}/{COUNT} | Last Latency: {latency}ms")
                    sys.stdout.flush()
                except Exception as e:
                    # Ignore keepalives or parse errors
                    pass
            
            print("\nDone.")
            if latencies:
                avg = sum(latencies) / len(latencies)
                print(f"Mean Latency: {avg:.2f} ms")
                print(f"Min: {min(latencies)} ms")
                print(f"Max: {max(latencies)} ms")

    except Exception as e:
        print(f"Error: {e}")
        print("Ensure 'server' and 'api-gateway' are running.")

if __name__ == "__main__":
    try:
        asyncio.run(measure_latency())
    except ImportError:
        print("Please run: pip install websockets")
