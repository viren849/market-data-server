package com.marketdata.gateway.config;

import com.marketdata.proto.MarketDataServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Value("${market.data.grpc.host:localhost}")
    private String grpcHost;

    @Value("${market.data.grpc.port:50051}")
    private int grpcPort;

    @Bean
    public ManagedChannel managedChannel() {
        return ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
    }

    @Bean
    public MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataStub(ManagedChannel channel) {
        return MarketDataServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    public MarketDataServiceGrpc.MarketDataServiceStub marketDataAsyncStub(ManagedChannel channel) {
        return MarketDataServiceGrpc.newStub(channel);
    }
}
