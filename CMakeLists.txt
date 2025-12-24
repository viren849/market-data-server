cmake_minimum_required(VERSION 3.16)
project(market_data_server)

set(CMAKE_CXX_STANDARD 20)

add_executable(market_data_server
    src/main.cpp
    src/server.cpp
)

add_executable(fake_client
    clients/fake_client.cpp
)
