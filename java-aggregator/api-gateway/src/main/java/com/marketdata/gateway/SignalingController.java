package com.marketdata.gateway;

import com.marketdata.gateway.model.SignalingMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class SignalingController {

    @MessageMapping("/signal")
    @SendTo("/topic/signaling")
    public SignalingMessage signal(@Payload SignalingMessage message) {
        // In a real app, strict routing/auth logic would go here.
        //yha pe likh bhai 
        
        // For Phase 9, we simply relay to all in the "room" (topic).
        return message;
    }
}
