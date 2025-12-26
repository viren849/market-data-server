#include "server.h"
#include "proto/tick.pb.h"

#include <algorithm>
#include <arpa/inet.h>
#include <cctype>
#include <chrono>
#include <cmath>
#include <iostream>
#include <netinet/in.h>
#include <thread>
#include <unistd.h>
#include <vector>

// ---------- Utilities ----------

// Read exactly N bytes from TCP
bool read_full(int fd, void *buf, size_t len) {
  char *p = static_cast<char *>(buf);
  while (len > 0) {
    ssize_t n = read(fd, p, len);
    if (n <= 0)
      return false;
    p += n;
    len -= n;
  }
  return true;
}

// Server-side clock (ns)
uint64_t now_ns() {
  return std::chrono::duration_cast<std::chrono::nanoseconds>(
             std::chrono::high_resolution_clock::now().time_since_epoch())
      .count();
}

// Length-prefixed frame reader
bool read_frame(int fd, std::vector<char> &out) {
  uint32_t len_net = 0;
  if (!read_full(fd, &len_net, sizeof(len_net)))
    return false;

  uint32_t len = ntohl(len_net);
  if (len == 0 || len > 1'000'000)
    return false;

  out.resize(len);
  return read_full(fd, out.data(), len);
}

// Validation (drop bad data safely)
bool validate_tick(const marketdata::Tick &t) {
  if (t.symbol().empty())
    return false;
  if (t.price() <= 0 || !std::isfinite(t.price()))
    return false;
  if (t.volume() <= 0)
    return false;
  if (t.exchange_timestamp() <= 0)
    return false;
  return true;
}

// Normalization (canonical form)
void normalize_tick(marketdata::Tick &t) {
  std::string s = t.symbol();

  // trim
  s.erase(s.begin(), std::find_if(s.begin(), s.end(), [](unsigned char c) {
            return !std::isspace(c);
          }));
  s.erase(std::find_if(s.rbegin(), s.rend(),
                       [](unsigned char c) { return !std::isspace(c); })
              .base(),
          s.end());

  // uppercase
  std::transform(s.begin(), s.end(), s.begin(),
                 [](unsigned char c) { return std::toupper(c); });

  t.set_symbol(s);
  t.set_ingest_timestamp(now_ns());
}

// ---------- Server ----------

TCPServer::TCPServer(int port) : port_(port), server_fd_(-1) {

  grpc_publisher_ = std::make_unique<GrpcTickPublisher>("localhost:50051");
}

void TCPServer::start() {
  // 1. Create a TCP socket (IPv4, Stream based)
  server_fd_ = socket(AF_INET, SOCK_STREAM, 0);

  // 2. Set SO_REUSEADDR to allow immediate restart on the same port
  int opt = 1;
  setsockopt(server_fd_, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

  // 3. Bind the socket to the specified port on all interfaces (INADDR_ANY)
  sockaddr_in addr{};
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = INADDR_ANY;
  addr.sin_port = htons(port_);

  bind(server_fd_, (sockaddr *)&addr, sizeof(addr));

  // 4. Start listening for incoming connections (backlog of 10)
  listen(server_fd_, 10);

  std::cout << "Server listening on port " << port_ << std::endl;

  // 5. Accept loop: consistently accept new clients and spawn threads
  while (true) {
    int client_fd = accept(server_fd_, nullptr, nullptr);
    // Dispatch client handling to a detached thread for concurrency
    std::thread(&TCPServer::handle_client, this, client_fd).detach();
  }
}

void TCPServer::handle_client(int client_fd) {
  std::vector<char> buffer;
  marketdata::Tick tick;

  while (true) {
    if (!read_frame(client_fd, buffer))
      break;

    uint64_t t0 = now_ns();
    bool ok = tick.ParseFromArray(buffer.data(), buffer.size());
    uint64_t t1 = now_ns();

    if (!ok) {
      std::cerr << "[DROP] protobuf parse failed\n";
      continue;
    }

    if (!validate_tick(tick)) {
      std::cerr << "[DROP] validation failed\n";
      continue;
    }

    // 3. Normalize: Canonical symbol format and add ingest timestamp
    normalize_tick(tick);

    // 4. Publish: Stream to downstream gRPC service (Phase 3)
    grpc_publisher_->publish(tick);

    std::cout << "[CANONICAL] "
              << "symbol=" << tick.symbol() << " price=" << tick.price()
              << " volume=" << tick.volume()
              << " exch_ts=" << tick.exchange_timestamp()
              << " ingest_ts=" << tick.ingest_timestamp() << std::endl;

    std::cout << "[METRIC] size_bytes=" << buffer.size()
              << " deserialize_ns=" << (t1 - t0) << std::endl;
  }

  close(client_fd);
}
