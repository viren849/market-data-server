#pragma once

#include <memory>
#include <grpcpp/grpcpp.h>
#include "grpc/marketdata.grpc.pb.h"

// Responsible ONLY for pushing ticks over gRPC
class GrpcTickPublisher {
public:
    explicit GrpcTickPublisher(const std::string& target);
    ~GrpcTickPublisher();

    bool publish(const marketdata::Tick& tick);
    void close();

private:
    std::unique_ptr<marketdata::MarketDataService::Stub> stub_;
    grpc::ClientContext context_;
    std::unique_ptr<grpc::ClientWriter<marketdata::Tick>> writer_;
};
