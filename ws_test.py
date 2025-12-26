import asyncio
import websockets
import json

async def test_market_data():
    uri = "ws://localhost:8080/ws/marketdata?symbol=AAPL"
    try:
        async with websockets.connect(uri) as websocket:
            print(f"Connected to {uri}")
            while True:
                message = await websocket.recv()
                data = json.loads(message)
                print(f"Received update: {data}")
                # Exit after receiving a few messages to pass verification
                if data['volume'] > 0:
                    print("Verification Successful!")
                    break
    except Exception as e:
        print(f"Connection failed: {e}")

if __name__ == "__main__":
    asyncio.run(test_market_data())
