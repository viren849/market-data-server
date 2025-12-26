# Network-Centric Real-Time Backend Project

A production-grade, multi-protocol market data system demonstrating networking fundamentals, API design patterns, and real-time streaming. The architecture evolves through 10 distinct phases, from raw TCP sockets to a complete distributed system with WebRTC collaboration.

## 🚀 Project Status: **Complete** (All 10 Phases Implemented)

## Architecture Overview

```
┌─────────────┐         ┌──────────────┐         ┌─────────────────┐
│ Fake Client │──TCP───▶│ C++ Server   │──gRPC──▶│ Java Aggregator │
└─────────────┘         │ (Port 50050) │         │ (Port 50051)    │
                        └──────────────┘         └─────────────────┘
                                                          │
                                                        gRPC
                                                          ▼
                        ┌──────────────────────────────────────────┐
                        │        API Gateway (Port 8080)            │
                        ├──────────────────────────────────────────┤
                        │ REST │ GraphQL │ WebSocket │ WebRTC      │
                        └──────────────────────────────────────────┘
                                          │
                                      HTTP/WS
                                          ▼
                                    ┌──────────┐
                                    │ Clients  │
                                    └──────────┘
```

## Project Phases

### ✅ Phase 1 — Raw Networking (C++)
**Objective:** TCP networking with raw socket communication.
- Multithreaded C++ server managing client connections
- Length-prefixed message framing
- `fake_client` to simulate market data

### ✅ Phase 2 — Serialization & Normalization
**Objective:** Structured data with validation.
- Protobuf (`Tick` and `Ack` messages)
- Deserialization in C++
- Data validation (positive prices, timestamps)

### ✅ Phase 3 — Internal Communication (gRPC)
**Objective:** Service decoupling via gRPC.
- `MarketDataService` gRPC contracts
- C++ client streams to Java server
- High-velocity tick streams

### ✅ Phase 4 — Stateful Aggregation Engine
**Objective:** Real-time in-memory aggregates.
- Thread-safe OHLC storage
- Real-time candle computation
- Atomic updates

### ✅ Phase 5 — API Gateway & REST APIs
**Objective:** External API exposure.
- Spring Boot Gateway
- REST API: `GET /api/v1/marketdata/{symbol}`
- gRPC-based backend integration

### ✅ Phase 6 — GraphQL APIs
**Objective:** Flexible, client-driven queries.
- GraphQL endpoint at `/graphql`
- GraphiQL UI at `/graphiql`
- DTO pattern for type safety

### ✅ Phase 7 — Real-Time Streaming
**Objective:** Live WebSocket updates.
- STOMP over WebSocket
- Broadcasting to `/topic/market-data/{symbol}`
- gRPC stream consumption

### ✅ Phase 8 — Reliability & Control
**Objective:** Production-ready resilience.
- Resilience4j integration
- Circuit Breakers
- Retry logic
- Rate Limiting

### ✅ Phase 9 — Collaboration (WebRTC)
**Objective:** Peer-to-peer communication.
- WebRTC signaling server
- SDP/ICE candidate relay
- Video/audio streaming support

### ✅ Phase 10 — Measurement & Proof
**Objective:** Performance validation.
- C++ micro-benchmark for Protobuf serialization
- End-to-end latency measurement
- Performance reporting

---

## Prerequisites

- **C++20 Compiler** (GCC 11+ or Clang 12+)
- **CMake** 3.16+
- **JDK** 17+
- **Gradle** 7.0+
- **Python** 3.8+ (for testing scripts)
- **Protobuf** & **gRPC** libraries

### macOS Installation
```bash
brew install cmake protobuf grpc openjdk@17
```

### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install -y build-essential cmake protobuf-compiler libprotobuf-dev libgrpc++-dev
```

---

## Building the Project

### 1. Build C++ Backend
```bash
# Generate build files
mkdir build && cd build
cmake ..

# Compile
make

# Verify executables
ls -lh market_data_server fake_client benchmark_serialization
```

### 2. Build Java Components
```bash
cd java-aggregator

# Build all modules
./gradlew build

# Or build specific modules
./gradlew :shared:build
./gradlew :app:build
./gradlew :api-gateway:build
```

---

## Running the System

### Local Development (All Services)

**Terminal 1: C++ Backend**
```bash
cd build
./market_data_server
# Listening on port 50050 (TCP) and streaming to port 50051 (gRPC)
```

**Terminal 2: Fake Data Generator**
```bash
cd build
./fake_client
# Sends mock tick data to C++ server
```

**Terminal 3: Java Aggregator**
```bash
cd java-aggregator
./gradlew :app:run
# Receives gRPC stream on port 50051
```

**Terminal 4: API Gateway**
```bash
cd java-aggregator
./gradlew :api-gateway:bootRun
# HTTP Server on port 8080
```

---

## Testing

### REST API
```bash
# Get market data for AAPL
curl http://localhost:8080/api/v1/marketdata/AAPL
```

### GraphQL
```bash
# Access GraphiQL UI
open http://localhost:8080/graphiql

# Sample Query:
# query {
#   marketData(symbol: "AAPL") {
#     symbol
#     open
#     high
#     low
#     close
#     volume
#     timestamp
#   }
# }
```

### WebSocket Streaming
```bash
# Use provided test client
open /path/to/artifacts/websocket_test.html
# or
python3 ws_test.py
```

### WebRTC Signaling
```bash
# Open in two browser tabs
open /path/to/artifacts/webrtc_test.html
# Click "Start Call" in one tab
```

### Performance Benchmarks
```bash
# C++ Serialization Benchmark
cd build
./benchmark_serialization

# End-to-End Latency (requires running system)
python3 measure_latency.py
```

---

## Deployment

### Local Deployment
✅ **Fully supported** (see "Running the System" above)

### Docker Deployment (Recommended for Production)

**Create Dockerfiles:**

`Dockerfile.cpp`:
```dockerfile
FROM gcc:latest
RUN apt-get update && apt-get install -y cmake protobuf-compiler libprotobuf-dev libgrpc++-dev
WORKDIR /app
COPY . .
RUN mkdir build && cd build && cmake .. && make
CMD ["./build/market_data_server"]
EXPOSE 50050 50051
```

`Dockerfile.java`:
```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY java-aggregator .
RUN ./gradlew build
CMD ["./gradlew", ":api-gateway:bootRun"]
EXPOSE 8080
```

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  cpp-server:
    build:
      context: .
      dockerfile: Dockerfile.cpp
    ports:
      - "50050:50050"
      - "50051:50051"
  
  api-gateway:
    build:
      context: .
      dockerfile: Dockerfile.java
    ports:
      - "8080:8080"
    environment:
      - MARKET_DATA_GRPC_HOST=cpp-server
      - MARKET_DATA_GRPC_PORT=50051
    depends_on:
      - cpp-server
```

**Run:**
```bash
docker-compose up --build
```

### Cloud Deployment

#### ☁️ **Can This Be Deployed Live? YES!**

**Deployment Options:**

1. **AWS (Recommended)**
   - **ECS/Fargate**: Deploy Docker containers
   - **Application Load Balancer**: For HTTP/WebSocket traffic
   - **CloudWatch**: For monitoring and logs
   - **Estimated Cost**: ~$50-100/month (small workload)

2. **Google Cloud Platform**
   - **Cloud Run**: Serverless containers
   - **Cloud Load Balancing**
   - **Cloud Logging**

3. **Azure**
   - **Azure Container Instances**
   - **Azure Application Gateway**

4. **Heroku** (Simplest for Demo)
   ```bash
   # Install Heroku CLI
   heroku create market-data-gateway
   heroku container:push web
   heroku container:release web
   heroku open
   ```

**Important Considerations for Production:**
- ⚠️ **WebSocket Support**: Ensure load balancer supports WebSocket (sticky sessions)
- ⚠️ **gRPC**: May need ALB/NLB with HTTP/2 support
- ⚠️ **WebRTC**: STUN/TURN servers needed for NAT traversal (e.g., Twilio TURN)
- 🔒 **Security**: Add HTTPS/TLS, authentication (JWT), rate limiting
- 📊 **Monitoring**: APM tools (New Relic, Datadog)
- 💾 **Persistence**: Add Redis/PostgreSQL for state if scaling horizontally

---

## Configuration

### API Gateway Properties
Located at: `java-aggregator/api-gateway/src/main/resources/application.properties`

```properties
# Server
server.port=8080

# gRPC Backend
market.data.grpc.host=localhost
market.data.grpc.port=50051

# GraphQL
spring.graphql.graphiql.enabled=true

# Resilience4j
resilience4j.circuitbreaker.instances.marketData.failureRateThreshold=50
resilience4j.retry.instances.marketData.maxAttempts=3
resilience4j.ratelimiter.instances.marketData.limitForPeriod=10
```

---

## Project Structure

```
market-data-server/
├── src/                          # C++ source files
│   ├── server.cpp               # TCP server
│   ├── grpc_client.cpp          # gRPC streaming client
│   ├── benchmark_serialization.cpp
│   ├── proto/                   # Generated Protobuf (Tick)
│   └── grpc/                    # Generated gRPC (MarketData)
├── proto/                       # Proto definitions
│   ├── tick.proto
│   └── marketdata.proto
├── clients/
│   └── fake_client.cpp          # Data generator
├── java-aggregator/
│   ├── shared/                  # Shared Protobuf/gRPC
│   ├── app/                     # Aggregation Engine
│   └── api-gateway/             # REST/GraphQL/WebSocket
├── measure_latency.py           # E2E latency script
├── CMakeLists.txt
└── README.md
```

---

## Troubleshooting

**Issue: `Connection refused` on gRPC**
- Ensure C++ server is running first
- Check port 50051 is not in use: `lsof -i :50051`

**Issue: `BUILD FAILED` (Gradle)**
- Clean build: `./gradlew clean build`
- Check JDK version: `java -version` (Must be 17+)

**Issue: WebSocket not connecting**
- Verify API Gateway is running on port 8080
- Check CORS settings in `WebSocketConfig`

**Issue: High latency in benchmarks**
- Reduce `ITERATIONS` in benchmark
- Check CPU usage
- Ensure no other heavy processes running

---

## Contributing

This is a learning/portfolio project demonstrating:
- Low-level networking (TCP, framing)
- High-level APIs (REST, GraphQL, WebSocket, WebRTC)
- Microservices communication (gRPC)
- Resilience patterns
- Performance measurement

Feel free to fork and extend!

---

## Contact & Portfolio

**Author**: Virender Kumar  
**Purpose**: Production-grade backend demonstration showcasing modern distributed systems architecture

---

## Next Steps (Future Enhancements) (its hard to do)

- [ ] Add Redis for distributed caching
- [ ] Implement Kafka for event streaming
- [ ] Add Prometheus/Grafana for metrics
- [ ] Implement authentication (OAuth2/JWT)
- [ ] Add database persistence (PostgreSQL)
- [ ] Horizontal scaling with Kubernetes
- [ ] Add frontend dashboard (React)
