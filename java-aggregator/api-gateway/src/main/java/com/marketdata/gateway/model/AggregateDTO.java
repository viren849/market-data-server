package com.marketdata.gateway.model;

public record AggregateDTO(
    String symbol,
    double open,
    double high,
    double low,
    double close,
    String volume,
    String timestamp
) {}
