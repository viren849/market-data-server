package com.marketdata.gateway;

import com.marketdata.gateway.model.AggregateDTO;
import com.marketdata.gateway.service.MarketDataClientService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class MarketDataGraphQLController {

    private final MarketDataClientService clientService;

    public MarketDataGraphQLController(MarketDataClientService clientService) {
        this.clientService = clientService;
    }

    @QueryMapping
    public AggregateDTO marketData(@Argument String symbol) {
        // The service layer handles resilience and DTO conversion
        return clientService.getAggregate(symbol);
    }
}
