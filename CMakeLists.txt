cmake_minimum_required(VERSION 3.16)
project(market_data_server)

set(CMAKE_CXX_STANDARD 20)


find_package(absl REQUIRED)
find_package(gRPC REQUIRED)

include_directories(src)
include_directories(src/proto)
include_directories(src/grpc)

add_executable(market_data_server src/server.cpp src/grpc/marketdata.pb.cc src/proto/tick.pb.cc src/grpc/marketdata.grpc.pb.cc)

add_executable(fake_client
    clients/fake_client.cpp
    src/proto/tick.pb.cc
)

add_executable(benchmark_serialization src/benchmark_serialization.cpp src/grpc/marketdata.pb.cc src/proto/tick.pb.cc)
target_link_libraries(benchmark_serialization PRIVATE protobuf::libprotobuf)
target_link_libraries(market_data_server PRIVATE protobuf::libprotobuf gRPC::grpc++ gRPC::grpc++_reflection)

target_link_libraries(fake_client
    protobuf::libprotobuf
    absl::log
    absl::check
)
