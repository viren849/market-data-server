package com.marketdata.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketdata.proto.Aggregate;
import com.marketdata.proto.AggregateRequest;
import com.marketdata.proto.MarketDataServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketDataWebSocketHandler extends TextWebSocketHandler {

    private final MarketDataServiceGrpc.MarketDataServiceStub asyncStub;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarketDataWebSocketHandler() {
        // Create an async stub for streaming
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        this.asyncStub = MarketDataServiceGrpc.newStub(channel);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket Connected: " + session.getId());

        // Parse query params to get symbol (e.g. ?symbol=AAPL)
        String query = session.getUri().getQuery();
        String symbol = "AAPL"; // Default
        if (query != null && query.contains("symbol=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("symbol=")) {
                    symbol = param.split("=")[1];
                    break;
                }
            }
        }
        
        System.out.println("Subscribing " + session.getId() + " to " + symbol);
        startSubscription(session, symbol);
    }

    private void startSubscription(WebSocketSession session, String symbol) {
        AggregateRequest request = AggregateRequest.newBuilder().setSymbol(symbol).build();

        asyncStub.streamAggregates(request, new StreamObserver<Aggregate>() {
            @Override
            public void onNext(Aggregate value) {
                if (!session.isOpen()) return;
                
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("symbol", value.getSymbol());
                    data.put("open", value.getOpen());
                    data.put("high", value.getHigh());
                    data.put("low", value.getLow());
                    data.put("close", value.getClose());
                    data.put("volume", value.getVolume());
                    data.put("timestamp", value.getTimestamp());

                    String json = objectMapper.writeValueAsString(data);
                    
                    // Sending must be synchronized
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(json));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("gRPC Stream Error: " + t.getMessage());
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException e) {
                    // ignore
                }
            }

            @Override
            public void onCompleted() {
                try {
                    session.close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    // ignore
                }
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        System.out.println("WebSocket Disconnected: " + session.getId());
    }
}
