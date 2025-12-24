#include <iostream>
#include <unistd.h>
#include <arpa/inet.h>
#include <cstring>
#include <thread>
#include <chrono>

int main(int argc, char* argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " <server_ip> <port>\n";
        return 1;
    }

    const char* server_ip = argv[1];
    int port = std::stoi(argv[2]);

    // 1️⃣ Create socket
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) {
        perror("socket");
        return 1;
    }

    // 2️⃣ Server address
    sockaddr_in server_addr{};
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(port);

    if (inet_pton(AF_INET, server_ip, &server_addr.sin_addr) <= 0) {
        perror("inet_pton");
        return 1;
    }

    // 3️⃣ Connect
    if (connect(sock, (sockaddr*)&server_addr, sizeof(server_addr)) < 0) {
        perror("connect");
        return 1;
    }

    std::cout << "Connected to server\n";

    // 4️⃣ Send fake ticks
    int tick_id = 1;
    while (true) {
        std::string tick =
            "TICK symbol=AAPL price=100.25 volume=10 id=" +
            std::to_string(tick_id++) + "\n";

        ssize_t sent = send(sock, tick.c_str(), tick.size(), 0);
        if (sent <= 0) {
            perror("send");
            break;
        }

        std::cout << "Sent: " << tick;

        std::this_thread::sleep_for(std::chrono::milliseconds(500));
    }

    close(sock);
    return 0;
}
