package com.marketdata.aggregator;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class GrpcServer {

    public static void main(String[] args) throws Exception {
        int port = 50051;

        Server server = ServerBuilder
                .forPort(port)
                .addService(new MarketDataServiceImpl())
                .build();

        server.start();
        System.out.println("gRPC Aggregation Server started on port " + port);

        server.awaitTermination();
    }
}
