package com.soundbox.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * Manages app preferences for SoundBox settings.
 */
public class PrefsManager {

    private static final String KEY_SERVICE_ENABLED = "service_enabled";
    private static final String KEY_VOLUME = "announcement_volume";
    private static final String KEY_SPEECH_RATE = "speech_rate";
    private static final String KEY_REPEAT_COUNT = "repeat_count";
    private static final String KEY_ANNOUNCE_SOURCE = "announce_source";
    private static final String KEY_ANNOUNCE_APP = "announce_app";
    private static final String KEY_LANGUAGE = "tts_language";
    private static final String KEY_PAYTM_ENABLED = "paytm_enabled";
    private static final String KEY_PHONEPE_ENABLED = "phonepe_enabled";
    private static final String KEY_GPAY_ENABLED = "gpay_enabled";
    private static final String KEY_BHIM_ENABLED = "bhim_enabled";
    private static final String KEY_OTHER_UPI_ENABLED = "other_upi_enabled";
    private static final String KEY_ALL_NOTIFICATIONS = "all_notifications_mode";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isServiceEnabled() {
        return prefs.getBoolean(KEY_SERVICE_ENABLED, true);
    }

    public void setServiceEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply();
    }

    public int getVolume() {
        return prefs.getInt(KEY_VOLUME, 100);
    }

    public float getSpeechRate() {
        return prefs.getFloat(KEY_SPEECH_RATE, 0.9f);
    }

    public void setSpeechRate(float rate) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply();
    }

    public int getRepeatCount() {
        try {
            return Integer.parseInt(prefs.getString(KEY_REPEAT_COUNT, "1"));
        } catch (Exception e) {
            return 1;
        }
    }

    public boolean shouldAnnounceSource() {
        return prefs.getBoolean(KEY_ANNOUNCE_SOURCE, true);
    }

    public boolean shouldAnnounceApp() {
        return prefs.getBoolean(KEY_ANNOUNCE_APP, false);
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "en_IN");
    }

    public boolean isPaytmEnabled() {
        return prefs.getBoolean(KEY_PAYTM_ENABLED, true);
    }

    public boolean isPhonePeEnabled() {
        return prefs.getBoolean(KEY_PHONEPE_ENABLED, true);
    }

    public boolean isGPayEnabled() {
        return prefs.getBoolean(KEY_GPAY_ENABLED, true);
    }

    public boolean isBhimEnabled() {
        return prefs.getBoolean(KEY_BHIM_ENABLED, true);
    }

    public boolean isOtherUpiEnabled() {
        return prefs.getBoolean(KEY_OTHER_UPI_ENABLED, true);
    }

    public boolean isAppEnabled(String packageName) {
        switch (packageName) {
            case PaymentParser.PKG_PAYTM:
            case PaymentParser.PKG_PAYTM_BUSINESS:
                return isPaytmEnabled();
            case PaymentParser.PKG_PHONEPE:
            case PaymentParser.PKG_PHONEPE_BUSINESS:
                return isPhonePeEnabled();
            case PaymentParser.PKG_GPAY:
                return isGPayEnabled();
            case PaymentParser.PKG_BHIM:
                return isBhimEnabled();
            default:
                return isOtherUpiEnabled();
        }
    }

    public boolean isAllNotificationsMode() {
        return prefs.getBoolean(KEY_ALL_NOTIFICATIONS, false);
    }
}
