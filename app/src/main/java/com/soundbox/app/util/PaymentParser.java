package com.soundbox.app.util;

import com.soundbox.app.model.PaymentInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses payment notification text from various UPI apps to extract
 * amount, sender, and other payment details.
 */
public class PaymentParser {

    // Common patterns for payment amounts in Indian notifications
    private static final Pattern[] AMOUNT_PATTERNS = {
        // Rs. 500, Rs 500, Rs.500, INR 500, ₹500, ₹ 500
        Pattern.compile("(?:Rs\\.?|INR|\\u20B9)\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        // Received 500, received Rs 500
        Pattern.compile("(?:received|credited|got)\\s+(?:Rs\\.?|INR|\\u20B9)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE),
        // 500 received, 500 credited
        Pattern.compile("([\\d,]+(?:\\.\\d{1,2})?)\\s+(?:received|credited)", Pattern.CASE_INSENSITIVE),
        // Amount: 500
        Pattern.compile("(?:amount|amt)[:\\s]+(?:Rs\\.?|INR|\\u20B9)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE),
    };

    // Patterns to extract sender name
    private static final Pattern[] SENDER_PATTERNS = {
        // from John, from John Doe
        Pattern.compile("from\\s+([A-Za-z][A-Za-z\\s]{1,30}?)(?:\\s+(?:via|on|for|Rs|INR|\\u20B9|\\.|$))", Pattern.CASE_INSENSITIVE),
        // by John
        Pattern.compile("by\\s+([A-Za-z][A-Za-z\\s]{1,30}?)(?:\\s+(?:via|on|for|Rs|INR|\\u20B9|\\.|$))", Pattern.CASE_INSENSITIVE),
        // Sender: John
        Pattern.compile("(?:sender|paid by|sent by)[:\\s]+([A-Za-z][A-Za-z\\s]{1,30}?)(?:\\s*$|\\s+(?:via|on|for|\\.))", Pattern.CASE_INSENSITIVE),
    };

    // Package names for supported UPI apps
    public static final String PKG_PAYTM = "net.one97.paytm";
    public static final String PKG_PHONEPE = "com.phonepe.app";
    public static final String PKG_GPAY = "com.google.android.apps.nbu.paisa.user";
    public static final String PKG_BHIM = "in.org.npci.upiapp";
    public static final String PKG_AMAZON_PAY = "in.amazon.mShop.android.shopping";
    public static final String PKG_WHATSAPP = "com.whatsapp";
    public static final String PKG_CRED = "com.dreamplug.androidapp";
    public static final String PKG_PAYTM_BUSINESS = "com.paytm.business";
    public static final String PKG_PHONEPE_BUSINESS = "com.phonepe.business";

    // Bank app packages
    public static final String PKG_SBI = "com.sbi.upi";
    public static final String PKG_ICICI = "com.csam.icici.bank.imobile";
    public static final String PKG_HDFC = "com.snapwork.hdfc";
    public static final String PKG_AXIS = "com.upi.axispay";
    public static final String PKG_BOB = "com.bankofbaroda.upi";

    public static boolean isSupportedApp(String packageName) {
        if (packageName == null) return false;
        switch (packageName) {
            case PKG_PAYTM:
            case PKG_PHONEPE:
            case PKG_GPAY:
            case PKG_BHIM:
            case PKG_AMAZON_PAY:
            case PKG_WHATSAPP:
            case PKG_CRED:
            case PKG_PAYTM_BUSINESS:
            case PKG_PHONEPE_BUSINESS:
            case PKG_SBI:
            case PKG_ICICI:
            case PKG_HDFC:
            case PKG_AXIS:
            case PKG_BOB:
                return true;
            default:
                // Also support any notification that contains payment keywords
                return false;
        }
    }

    public static String getAppName(String packageName) {
        if (packageName == null) return "Unknown";
        switch (packageName) {
            case PKG_PAYTM: return "Paytm";
            case PKG_PHONEPE: return "PhonePe";
            case PKG_GPAY: return "Google Pay";
            case PKG_BHIM: return "BHIM";
            case PKG_AMAZON_PAY: return "Amazon Pay";
            case PKG_WHATSAPP: return "WhatsApp";
            case PKG_CRED: return "CRED";
            case PKG_PAYTM_BUSINESS: return "Paytm Business";
            case PKG_PHONEPE_BUSINESS: return "PhonePe Business";
            case PKG_SBI: return "SBI";
            case PKG_ICICI: return "ICICI";
            case PKG_HDFC: return "HDFC";
            case PKG_AXIS: return "Axis Pay";
            case PKG_BOB: return "Bank of Baroda";
            default: return "UPI";
        }
    }

    /**
     * Check if notification text looks like a payment notification.
     */
    public static boolean isPaymentNotification(String title, String text) {
        if (text == null && title == null) return false;
        String combined = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase();

        // Must contain a payment-related keyword
        boolean hasPaymentKeyword = combined.contains("received") ||
                combined.contains("credited") ||
                combined.contains("payment") ||
                combined.contains("paid") ||
                combined.contains("money") ||
                combined.contains("transferred") ||
                combined.contains("deposit") ||
                combined.contains("upi");

        // Must contain an amount indicator
        boolean hasAmount = combined.contains("rs") ||
                combined.contains("inr") ||
                combined.contains("\u20B9") ||
                combined.matches(".*\\d+.*");

        return hasPaymentKeyword && hasAmount;
    }

    /**
     * Parse payment details from notification text.
     */
    public static PaymentInfo parse(String packageName, String title, String text) {
        String combined = (title != null ? title + " " : "") + (text != null ? text : "");
        String amount = extractAmount(combined);
        String sender = extractSender(combined);
        String appName = getAppName(packageName);

        return new PaymentInfo(amount, sender, appName, System.currentTimeMillis(), combined);
    }

    private static String extractAmount(String text) {
        if (text == null) return null;
        for (Pattern pattern : AMOUNT_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).replace(",", "");
            }
        }
        return null;
    }

    private static String extractSender(String text) {
        if (text == null) return null;
        for (Pattern pattern : SENDER_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }
}
