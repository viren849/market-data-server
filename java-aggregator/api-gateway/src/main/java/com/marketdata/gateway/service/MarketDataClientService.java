package com.marketdata.gateway.service;

import com.marketdata.gateway.model.AggregateDTO;
import com.marketdata.proto.Aggregate;
import com.marketdata.proto.AggregateRequest;
import com.marketdata.proto.MarketDataServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class MarketDataClientService {

    private final MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataStub;

    public MarketDataClientService(MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataStub) {
        this.marketDataStub = marketDataStub;
    }

    @CircuitBreaker(name = "marketData", fallbackMethod = "getAggregateFallback")
    @Retry(name = "marketData")
    @RateLimiter(name = "marketData")
    public AggregateDTO getAggregate(String symbol) {
        AggregateRequest request = AggregateRequest.newBuilder()
                .setSymbol(symbol)
                .build();
        
        Aggregate agg = marketDataStub.getAggregate(request);
        if (agg == null) return null;

        return new AggregateDTO(
                agg.getSymbol(),
                agg.getOpen(),
                agg.getHigh(),
                agg.getLow(),
                agg.getClose(),
                String.valueOf(agg.getVolume()),
                String.valueOf(agg.getTimestamp())
        );
    }

    // Fallback method must have same signature + Throwable
    public AggregateDTO getAggregateFallback(String symbol, Throwable t) {
        System.err.println("Fallback triggered for " + symbol + ": " + t.getMessage());
        // Return a safe default or empty object, or even null if acceptable
        // For now, let's return a dummy object to indicate offline mode
        return new AggregateDTO(
                symbol,
                0.0,
                0.0,
                0.0,
                0.0,
                "0",
                "0"
        );
    }
}
