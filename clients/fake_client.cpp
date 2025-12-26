#include <iostream>
#include <unistd.h>
#include <arpa/inet.h>
#include <thread>
#include <chrono>
#include <vector>

// Protobuf generated header
#include "proto/tick.pb.h"

// ---------- Utility: current time in nanoseconds ----------
uint64_t now_ns() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(
        std::chrono::high_resolution_clock::now().time_since_epoch()
    ).count();
}

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

    // ---------- Protobuf Tick ----------
    marketdata::Tick tick;
    tick.set_symbol(" aApL ");   // intentionally messy (server will normalize)
    tick.set_price(100.25);
    tick.set_volume(10);

    // 4️⃣ Send fake ticks continuously
    while (true) {
        // Exchange timestamp (client-owned)
        tick.set_exchange_timestamp(now_ns());
        // DO NOT set ingest_timestamp (server-owned)

        // ---- Serialize Protobuf ----
        int payload_size = tick.ByteSizeLong();
        std::vector<char> payload(payload_size);

        if (!tick.SerializeToArray(payload.data(), payload_size)) {
            std::cerr << "Failed to serialize Tick\n";
            break;
        }

        // ---- Frame format ----
        // [4 bytes length][protobuf payload]
        uint32_t len_net = htonl(payload_size);

        // Send length prefix
        if (send(sock, &len_net, sizeof(len_net), 0) != sizeof(len_net)) {
            perror("send length");
            break;
        }

        // Send payload
        if (send(sock, payload.data(), payload_size, 0) != payload_size) {
            perror("send payload");
            break;
        }

        std::cout << "Sent Tick (size=" << payload_size << " bytes)\n";

        // Simulate market tick rate
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    close(sock);
    return 0;
}
