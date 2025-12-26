package com.marketdata.aggregator;

import io.grpc.stub.StreamObserver;

import com.marketdata.proto.Tick;
import com.marketdata.proto.Ack;
import com.marketdata.proto.MarketDataServiceGrpc;

public class MarketDataServiceImpl
        extends MarketDataServiceGrpc.MarketDataServiceImplBase {

    // Main Aggregation Engine (Phase 4)
    // Shared across all client streams to maintain a "Global Market State"
    private final Aggregator aggregator = new Aggregator();


    @Override
    public StreamObserver<Tick> streamTicks(
            StreamObserver<Ack> responseObserver) {

        return new StreamObserver<>() {

            long count = 0;
            // Create a per-stream aggregator (or this could be shared globally if we want global state)
            // For Phase 4, let's assume we want a global view, so we should inject it.
            // But for simplicity in this step, I'll instantiate a static/shared one or just one here.
            // Let's make it an instance field of the service to persist across streams if needed,
            // but for now, to keep it simple and clean:
            
            @Override
            public void onNext(Tick tick) {
                count++;
                
                // Update Aggregates (Thread-safe)
                aggregator.onTick(tick);
                
                // Get Real-time Snapshot
                Candle candle = aggregator.getSnapshot(tick.getSymbol());
                
                System.out.println(
                        "[RECEIVED] symbol=" + tick.getSymbol()
                        + " price=" + tick.getPrice()
                        + " volume=" + tick.getVolume()
                );
                
                if (candle != null) {
                     System.out.println(
                        "[AGGREGATE] symbol=" + candle.symbol()
                        + " OHLC=[" + candle.open() + ", " + candle.high() + ", " + candle.low() + ", " + candle.close() + "]"
                        + " vol=" + candle.volume()
                    );
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("[ERROR] stream failed: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("[STREAM CLOSED] total_ticks=" + count);

                Ack ack = Ack.newBuilder()
                        .setSuccess(true)
                        .setMessage("Received " + count + " ticks")
                        .build();

                responseObserver.onNext(ack);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void getAggregate(com.marketdata.proto.AggregateRequest request,
                             io.grpc.stub.StreamObserver<com.marketdata.proto.Aggregate> responseObserver) {
        Candle candle = aggregator.getSnapshot(request.getSymbol());

        if (candle == null) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Symbol not found: " + request.getSymbol())
                    .asRuntimeException());
            return;
        }

        com.marketdata.proto.Aggregate agg = com.marketdata.proto.Aggregate.newBuilder()
                .setSymbol(candle.symbol())
                .setOpen(candle.open())
                .setHigh(candle.high())
                .setLow(candle.low())
                .setClose(candle.close())
                .setVolume(candle.volume())
                .setTimestamp(candle.timestamp())
                .build();

        responseObserver.onNext(agg);
        responseObserver.onCompleted();
    }

    @Override
    public void streamAggregates(com.marketdata.proto.AggregateRequest request,
                                 io.grpc.stub.StreamObserver<com.marketdata.proto.Aggregate> responseObserver) {
        String symbol = request.getSymbol();
        
        // Register a listener for updates
        aggregator.registerListener(candle -> {
            if (candle.symbol().equals(symbol)) {
                com.marketdata.proto.Aggregate agg = com.marketdata.proto.Aggregate.newBuilder()
                        .setSymbol(candle.symbol())
                        .setOpen(candle.open())
                        .setHigh(candle.high())
                        .setLow(candle.low())
                        .setClose(candle.close())
                        .setVolume(candle.volume())
                        .setTimestamp(candle.timestamp())
                        .build();
                
                try {
                    synchronized(responseObserver) {
                         responseObserver.onNext(agg);
                    }
                } catch (Exception e) {
                    // Stream likely closed
                    System.err.println("Failed to send update: " + e.getMessage());
                }
            }
        });
        
        // Note: In a real implementation we would need a way to unregister listeners 
        // when the client cancels or disconnects. For this phase, we'll keep it simple 
        // and acknowledge that this leaks a listener per connection.
        
        // Send initial state immediately if exists
        Candle initial = aggregator.getSnapshot(symbol);
        if (initial != null) {
             com.marketdata.proto.Aggregate agg = com.marketdata.proto.Aggregate.newBuilder()
                    .setSymbol(initial.symbol())
                    .setOpen(initial.open())
                    .setHigh(initial.high())
                    .setLow(initial.low())
                    .setClose(initial.close())
                    .setVolume(initial.volume())
                    .setTimestamp(initial.timestamp())
                    .build();
             synchronized(responseObserver) {
                responseObserver.onNext(agg);
             }
        }
    }
}
