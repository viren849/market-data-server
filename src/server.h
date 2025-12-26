#include <memory>
#include "grpc_client.h"

class TCPServer {
public:
    explicit TCPServer(int port);
    void start();

private:
    int port_;
    int server_fd_;

    std::unique_ptr<GrpcTickPublisher> grpc_publisher_;

    void handle_client(int client_fd);
};
