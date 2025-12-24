#include "server.h"

#include <iostream>
#include <thread>
#include <unistd.h>
#include <netinet/in.h>
#include <cstring>

TCPServer::TCPServer(int port)
    : port_(port), server_fd_(-1) {}

void TCPServer::start() {
    // 1️⃣ Create socket
    server_fd_ = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd_ < 0) {
        perror("socket");
        return;
    }

    // 2️⃣ Allow quick reuse of port
    int opt = 1;
    setsockopt(server_fd_, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    // 3️⃣ Bind
    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port_);

    if (bind(server_fd_, (sockaddr*)&addr, sizeof(addr)) < 0) {
        perror("bind");
        return;
    }

    // 4️⃣ Listen
    if (listen(server_fd_, 10) < 0) {
        perror("listen");
        return;
    }

    std::cout << "Server listening on port " << port_ << std::endl;

    // 5️⃣ Accept loop
    while (true) {
        int client_fd = accept(server_fd_, nullptr, nullptr);
        if (client_fd < 0) {
            perror("accept");
            continue;
        }

        std::thread(&TCPServer::handle_client, this, client_fd).detach();
    }
}

void TCPServer::handle_client(int client_fd) {
    char buffer[256];

    while (true) {
        ssize_t n = read(client_fd, buffer, sizeof(buffer));
        if (n <= 0) break;

        // Echo back
        write(client_fd, buffer, n);
    }

    close(client_fd);
}

