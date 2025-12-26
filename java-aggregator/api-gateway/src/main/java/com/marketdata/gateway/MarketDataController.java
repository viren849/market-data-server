package com.marketdata.gateway;

import com.marketdata.proto.Aggregate;
import com.marketdata.proto.AggregateRequest;
import com.marketdata.proto.MarketDataServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/marketdata")
public class MarketDataController {

    private final MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataStub;

    public MarketDataController() {
        // Connect to the gRPC backend (assuming localhost:50051)
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        this.marketDataStub = MarketDataServiceGrpc.newBlockingStub(channel);
    }

    @GetMapping("/{symbol}")
    public Map<String, Object> getAggregate(@PathVariable String symbol) {
        AggregateRequest request = AggregateRequest.newBuilder()
                .setSymbol(symbol)
                .build();

        try {
            Aggregate agg = marketDataStub.getAggregate(request);
            
            // Convert to JSON-friendly map
            Map<String, Object> response = new HashMap<>();
            response.put("symbol", agg.getSymbol());
            response.put("open", agg.getOpen());
            response.put("high", agg.getHigh());
            response.put("low", agg.getLow());
            response.put("close", agg.getClose());
            response.put("volume", agg.getVolume());
            response.put("timestamp", agg.getTimestamp());
            
            return response;
        } catch (Exception e) {
            // Basic error handling
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }
}
