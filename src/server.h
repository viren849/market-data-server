#pragma once

class TCPServer {
public:
    explicit TCPServer(int port);
    void start();

private:
    int port_;
    int server_fd_;

    void handle_client(int client_fd);
};
