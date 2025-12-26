package com.marketdata.gateway.service;

import com.marketdata.gateway.model.AggregateDTO;
import com.marketdata.proto.Aggregate;
import com.marketdata.proto.AggregateRequest;
import com.marketdata.proto.MarketDataServiceGrpc;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GrpcStreamManager {

    private final SimpMessagingTemplate messagingTemplate;
    private final MarketDataServiceGrpc.MarketDataServiceStub asyncStub;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, Boolean> activeStreams = new ConcurrentHashMap<>();

    public GrpcStreamManager(MarketDataServiceGrpc.MarketDataServiceStub asyncStub,
                             SimpMessagingTemplate messagingTemplate) {
        this.asyncStub = asyncStub;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void init() {
        // For demonstration, let's automatically subscribe to a few symbols on startup.
        // In a real app, this would be dynamic based on user subscriptions.
        startStream("AAPL");
        startStream("GOOGL");
        startStream("MSFT");
    }

    public void startStream(String symbol) {
        if (activeStreams.containsKey(symbol)) {
            return;
        }
        activeStreams.put(symbol, true);

        AggregateRequest request = AggregateRequest.newBuilder()
                .setSymbol(symbol)
                .build();

        asyncStub.streamAggregates(request, new StreamObserver<Aggregate>() {
            @Override
            public void onNext(Aggregate agg) {
                AggregateDTO dto = new AggregateDTO(
                        agg.getSymbol(),
                        agg.getOpen(),
                        agg.getHigh(),
                        agg.getLow(),
                        agg.getClose(),
                        String.valueOf(agg.getVolume()),
                        String.valueOf(agg.getTimestamp())
                );
                messagingTemplate.convertAndSend("/topic/market-data/" + symbol, dto);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Stream error for " + symbol + ": " + t.getMessage());
                activeStreams.remove(symbol);
                // Simple retry mechanism could go here
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed for " + symbol);
                activeStreams.remove(symbol);
            }
        });
    }
}
