package com.projectapex.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionRecord {
    private String txId;
    private String bookingId;
    private String userId;
    private String type; // "PAYMENT", "REFUND", "CASH_COLLECTED"
    private Integer amount;
    private String gateway; // "Razorpay", "Offline_Cash"
    private String gatewayRef; // paymentId / orderId
    private String status; // "SUCCESS", "FAILED"
    private Long timestamp;

    public TransactionRecord() {}

    public TransactionRecord(String txId, String bookingId, String userId, String type, Integer amount, String gateway, String gatewayRef) {
        this.txId = txId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.gateway = gateway;
        this.gatewayRef = gatewayRef;
        this.status = "SUCCESS";
        this.timestamp = System.currentTimeMillis();
    }

    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getGatewayRef() { return gatewayRef; }
    public void setGatewayRef(String gatewayRef) { this.gatewayRef = gatewayRef; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
