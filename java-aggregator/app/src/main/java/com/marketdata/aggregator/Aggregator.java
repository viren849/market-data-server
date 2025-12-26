package com.marketdata.aggregator;

import com.marketdata.proto.Tick;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe Aggregation Engine.
 * Maintains real-time statistics (OHLCV) for market symbols using atomic operations for performance.
 *
 * This class ensures that high-velocity updates from multiple gRPC threads
 * are aggregated correctly.
 */
public class Aggregator {

    // Store per-symbol aggregate state as immutable Candle records
    private final Map<String, Candle> candles = new ConcurrentHashMap<>();
    
    // Listeners for real-time updates
    private final List<Consumer<Candle>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Process an incoming tick.
     * Thread-safe update of the symbol's statistics.
     *
     * @param tick The incoming market tick
     */
    public void onTick(Tick tick) {
        String symbol = tick.getSymbol();
        double price = tick.getPrice();
        long volume = tick.getVolume();
        long timestamp = tick.getExchangeTimestamp(); 

        candles.compute(symbol, (k, current) -> {
            if (current == null) {
                return new Candle(symbol, price, price, price, price, volume, timestamp);
            }
            
            // Accumulate stats
            double newHigh = Math.max(current.high(), price);
            double newLow = Math.min(current.low(), price);
            long newVolume = current.volume() + volume;
            
            return new Candle(symbol, current.open(), newHigh, newLow, price, newVolume, timestamp);
        });
        
        // Notify listeners of the new state
        Candle updated = candles.get(symbol);
        if (updated != null) {
            notifyListeners(updated);
        }
    }

    /**
     * Get a snapshot of the current state for a symbol.
     *
     * @param symbol Ticker symbol (e.g., "AAPL")
     * @return Candle Record containing the snapshot
     */
    public Candle getSnapshot(String symbol) {
        return candles.get(symbol);
    }
    
    /**
     * Register a listener to receive updates for all symbols.
     * @param listener The consumer to be called with the updated Candle
     */
    public void registerListener(Consumer<Candle> listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners(Candle candle) {
        for (Consumer<Candle> listener : listeners) {
            try {
                listener.accept(candle);
            } catch (Exception e) {
                // Ignore listener errors to prevent affecting the ingestion flow
            }
        }
    }
}
