package com.projectapex.model;

public class WebhookEventRecord {
    private String eventId;
    private String eventType;
    private long receivedAt = System.currentTimeMillis();
    private String processedStatus = "PROCESSED"; // PROCESSED, SKIPPED_DUPLICATE, FAILED

    public WebhookEventRecord() {
    }

    public WebhookEventRecord(String eventId, String eventType, String processedStatus) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedStatus = processedStatus;
        this.receivedAt = System.currentTimeMillis();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getProcessedStatus() {
        return processedStatus;
    }

    public void setProcessedStatus(String processedStatus) {
        this.processedStatus = processedStatus;
    }
}
