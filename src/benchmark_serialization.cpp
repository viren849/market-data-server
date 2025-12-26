#include "marketdata.pb.h"
#include "tick.pb.h"
#include <chrono>
#include <iostream>
#include <vector>

using namespace marketdata;

int main() {
  // 1. Setup Data
  Tick tick;
  tick.set_symbol("AAPL");
  tick.set_price(150.25);
  tick.set_volume(1000);
  tick.set_exchange_timestamp(1625247890000);
  tick.set_ingest_timestamp(1625247890005);

  const int ITERATIONS = 1000000;
  std::string serialized_data;
  serialized_data.reserve(128);

  // 2. Benchmark Serialization
  auto start_ser = std::chrono::high_resolution_clock::now();
  for (int i = 0; i < ITERATIONS; ++i) {
    tick.SerializeToString(&serialized_data);
  }
  auto end_ser = std::chrono::high_resolution_clock::now();

  // 3. Benchmark Deserialization
  Tick out_tick;
  auto start_deser = std::chrono::high_resolution_clock::now();
  for (int i = 0; i < ITERATIONS; ++i) {
    out_tick.ParseFromString(serialized_data);
  }
  auto end_deser = std::chrono::high_resolution_clock::now();

  // 4. Report
  std::chrono::duration<double> diff_ser = end_ser - start_ser;
  std::chrono::duration<double> diff_deser = end_deser - start_deser;

  std::cout << "Iterations: " << ITERATIONS << "\n";
  std::cout << "Serialization Time: " << diff_ser.count() << " s ("
            << (ITERATIONS / diff_ser.count()) / 1000000.0 << " M ops/s)\n";
  std::cout << "Deserialization Time: " << diff_deser.count() << " s ("
            << (ITERATIONS / diff_deser.count()) / 1000000.0 << " M ops/s)\n";

  return 0;
}
