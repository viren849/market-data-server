package com.marketdata.gateway.model;

public class SignalingMessage {
    private String type; // OFFER, ANSWER, ICE, JOIN
    private String senderId;
    private String targetId;
    private String sdp;
    private String candidate;

    public SignalingMessage() {
    }

    public SignalingMessage(String type, String senderId, String targetId, String sdp, String candidate) {
        this.type = type;
        this.senderId = senderId;
        this.targetId = targetId;
        this.sdp = sdp;
        this.candidate = candidate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }
}
