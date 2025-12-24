#include "server.h"

#include <iostream>
#include <thread>
#include <unistd.h>
#include <netinet/in.h>
#include <cstring>
#include <sstream>
#include <unordered_map>

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
    char buffer[512];

    while (true) {
        ssize_t n = read(client_fd, buffer, sizeof(buffer) - 1);
        if (n <= 0) break;

        buffer[n] = '\0';

        std::string msg(buffer);

        // Example:
        // "TICK symbol=AAPL price=100.25 volume=10 id=1\n"

        std::istringstream iss(msg);
        std::string token;

        // First token should be "TICK"
        iss >> token;
        if (token != "TICK") {
            std::cerr << "[WARN] Unknown message: " << msg << std::endl;
            continue;
        }

        std::unordered_map<std::string, std::string> fields;

        // Parse key=value pairs
        while (iss >> token) {
            auto pos = token.find('=');
            if (pos == std::string::npos) continue;

            std::string key = token.substr(0, pos);
            std::string value = token.substr(pos + 1);
            fields[key] = value;
        }

        // Extract fields safely
        std::string symbol = fields["symbol"];
        double price = std::stod(fields["price"]);
        int volume = std::stoi(fields["volume"]);
        int id = std::stoi(fields["id"]);

        // Structured output (server now understands data)
        std::cout << "[TICK RECEIVED] "
                  << "symbol=" << symbol
                  << " price=" << price
                  << " volume=" << volume
                  << " id=" << id
                  << std::endl;
    }

    close(client_fd);
}


