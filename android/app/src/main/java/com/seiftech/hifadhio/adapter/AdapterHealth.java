package com.seiftech.hifadhio.adapter;

public class AdapterHealth {
    private final boolean healthy;
    private final String statusMessage;
    private final long lastCheckedAt;

    public AdapterHealth(boolean healthy, String statusMessage) {
        this.healthy = healthy;
        this.statusMessage = statusMessage;
        this.lastCheckedAt = System.currentTimeMillis();
    }

    public boolean isHealthy() {
        return healthy;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public long getLastCheckedAt() {
        return lastCheckedAt;
    }

    public static AdapterHealth ok(String message) {
        return new AdapterHealth(true, message);
    }

    public static AdapterHealth degraded(String message) {
        return new AdapterHealth(false, message);
    }

    @Override
    public String toString() {
        return (healthy ? "HEALTHY" : "DEGRADED") + ": " + statusMessage;
    }
}
