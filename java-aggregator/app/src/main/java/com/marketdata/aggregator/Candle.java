package com.marketdata.aggregator;

/**
 * Represents a Market Candle (OHLCV) for a specific time interval.
 * This is an immutable record used to snapshot the market state.
 *
 * OHLCV stands for: Open, High, Low, Close, Volume.
 */
public record Candle(
    String symbol,
    double open,
    double high,
    double low,
    double close,
    long volume,
    long timestamp // Start time of this candle
) {}
