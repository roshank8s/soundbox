package com.soundbox.app.model;

public class PaymentInfo {
    private final String amount;
    private final String source;
    private final String appName;
    private final long timestamp;
    private final String rawText;

    public PaymentInfo(String amount, String source, String appName, long timestamp, String rawText) {
        this.amount = amount;
        this.source = source;
        this.appName = appName;
        this.timestamp = timestamp;
        this.rawText = rawText;
    }

    public String getAmount() { return amount; }
    public String getSource() { return source; }
    public String getAppName() { return appName; }
    public long getTimestamp() { return timestamp; }
    public String getRawText() { return rawText; }

    public String getAnnouncementText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Payment received");
        if (amount != null && !amount.isEmpty()) {
            sb.append(", rupees ").append(amount);
        }
        if (source != null && !source.isEmpty()) {
            sb.append(", from ").append(source);
        }
        sb.append(", via ").append(appName);
        return sb.toString();
    }

    public String getDisplayAmount() {
        if (amount != null && !amount.isEmpty()) {
            return "\u20B9 " + amount;
        }
        return "N/A";
    }
}
