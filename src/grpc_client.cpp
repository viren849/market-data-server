#include "grpc_client.h"
#include <iostream>

GrpcTickPublisher::GrpcTickPublisher(const std::string& target) {
    auto channel = grpc::CreateChannel(
        target, grpc::InsecureChannelCredentials());

    stub_ = marketdata::MarketDataService::NewStub(channel);

    writer_ = stub_->StreamTicks(&context_, nullptr);

    std::cout << "[gRPC] Connected to " << target << std::endl;
}

GrpcTickPublisher::~GrpcTickPublisher() {
    close();
}

bool GrpcTickPublisher::publish(const marketdata::Tick& tick) {
    if (!writer_) return false;

    if (!writer_->Write(tick)) {
        std::cerr << "[gRPC] write failed\n";
        return false;
    }
    return true;
}

void GrpcTickPublisher::close() {
    if (!writer_) return;

    writer_->WritesDone();
    grpc::Status status = writer_->Finish();

    if (status.ok()) {
        std::cout << "[gRPC] Stream closed cleanly\n";
    } else {
        std::cerr << "[gRPC] Stream error: "
                  << status.error_message() << std::endl;
    }

    writer_.reset();
}
