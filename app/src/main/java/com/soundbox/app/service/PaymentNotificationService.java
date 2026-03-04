package com.soundbox.app.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.soundbox.app.model.PaymentInfo;
import com.soundbox.app.util.PaymentParser;
import com.soundbox.app.util.PrefsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Core service that listens for payment notifications from UPI apps
 * and announces them using TTS. This replaces a physical SoundBox device.
 */
public class PaymentNotificationService extends NotificationListenerService {

    private static final String TAG = "PaymentNotifService";
    public static final String ACTION_PAYMENT_RECEIVED = "com.soundbox.app.PAYMENT_RECEIVED";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_APP = "app_name";
    public static final String EXTRA_RAW_TEXT = "raw_text";
    public static final String EXTRA_TIMESTAMP = "timestamp";

    private SoundBoxTTS soundBoxTTS;
    private PrefsManager prefs;
    private long lastAnnouncementTime = 0;
    private String lastAnnouncedText = "";

    // Minimum gap between announcements to avoid duplicates (3 seconds)
    private static final long DUPLICATE_THRESHOLD_MS = 3000;

    // Store recent payments for UI
    private static final List<PaymentInfo> recentPayments = new ArrayList<>();
    private static OnPaymentListener paymentListener;

    public interface OnPaymentListener {
        void onPaymentReceived(PaymentInfo payment);
    }

    public static void setPaymentListener(OnPaymentListener listener) {
        paymentListener = listener;
    }

    public static List<PaymentInfo> getRecentPayments() {
        return new ArrayList<>(recentPayments);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        soundBoxTTS = new SoundBoxTTS(this);
        prefs = new PrefsManager(this);
        Log.d(TAG, "PaymentNotificationService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (soundBoxTTS != null) {
            soundBoxTTS.shutdown();
        }
        Log.d(TAG, "PaymentNotificationService destroyed");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        // Check if service is enabled
        if (!prefs.isServiceEnabled()) return;

        String packageName = sbn.getPackageName();
        Notification notification = sbn.getNotification();
        if (notification == null) return;

        // Extract notification text
        Bundle extras = notification.extras;
        if (extras == null) return;

        String title = extras.containsKey(Notification.EXTRA_TITLE)
                ? String.valueOf(extras.get(Notification.EXTRA_TITLE))
                : null;
        String text = extras.containsKey(Notification.EXTRA_TEXT)
                ? String.valueOf(extras.get(Notification.EXTRA_TEXT))
                : null;
        String bigText = extras.containsKey(Notification.EXTRA_BIG_TEXT)
                ? String.valueOf(extras.get(Notification.EXTRA_BIG_TEXT))
                : null;

        // Prefer big text as it usually has more detail
        String notificationText = bigText != null ? bigText : text;

        Log.d(TAG, "Notification from: " + packageName + " | Title: " + title + " | Text: " + notificationText);

        // Check if this is from a supported UPI app
        boolean isFromSupportedApp = PaymentParser.isSupportedApp(packageName);

        if (isFromSupportedApp) {
            // Check if this specific app is enabled in settings
            if (!prefs.isAppEnabled(packageName)) return;

            // Check if it looks like a payment notification
            if (PaymentParser.isPaymentNotification(title, notificationText)) {
                handlePaymentNotification(packageName, title, notificationText);
            }
        } else if (prefs.isAllNotificationsMode()) {
            // In "all notifications" mode, try to detect payment from any app
            if (PaymentParser.isPaymentNotification(title, notificationText)) {
                handlePaymentNotification(packageName, title, notificationText);
            }
        }
    }

    private void handlePaymentNotification(String packageName, String title, String text) {
        // Parse payment details
        PaymentInfo payment = PaymentParser.parse(packageName, title, text);

        // Duplicate detection
        String currentText = payment.getAnnouncementText();
        long now = System.currentTimeMillis();
        if (currentText.equals(lastAnnouncedText) && (now - lastAnnouncementTime) < DUPLICATE_THRESHOLD_MS) {
            Log.d(TAG, "Skipping duplicate announcement");
            return;
        }

        lastAnnouncedText = currentText;
        lastAnnouncementTime = now;

        // Store payment
        recentPayments.add(0, payment);
        if (recentPayments.size() > 50) {
            recentPayments.remove(recentPayments.size() - 1);
        }

        // Notify UI
        if (paymentListener != null) {
            paymentListener.onPaymentReceived(payment);
        }

        // Broadcast for any other receivers
        Intent broadcastIntent = new Intent(ACTION_PAYMENT_RECEIVED);
        broadcastIntent.putExtra(EXTRA_AMOUNT, payment.getAmount());
        broadcastIntent.putExtra(EXTRA_SOURCE, payment.getSource());
        broadcastIntent.putExtra(EXTRA_APP, payment.getAppName());
        broadcastIntent.putExtra(EXTRA_RAW_TEXT, payment.getRawText());
        broadcastIntent.putExtra(EXTRA_TIMESTAMP, payment.getTimestamp());
        sendBroadcast(broadcastIntent);

        // Build announcement text respecting user preferences
        String announcementText = buildAnnouncementText(payment);
        Log.d(TAG, "Announcing: " + announcementText);
        soundBoxTTS.announce(announcementText);
    }

    private String buildAnnouncementText(PaymentInfo payment) {
        StringBuilder sb = new StringBuilder();
        sb.append("Payment received");

        String amount = payment.getAmount();
        if (amount != null && !amount.isEmpty()) {
            sb.append(", rupees ").append(amount);
        }

        if (prefs.shouldAnnounceSource()) {
            String source = payment.getSource();
            if (source != null && !source.isEmpty()) {
                sb.append(", from ").append(source);
            }
        }

        if (prefs.shouldAnnounceApp()) {
            sb.append(", via ").append(payment.getAppName());
        }

        return sb.toString();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Not needed for our use case
    }
}
