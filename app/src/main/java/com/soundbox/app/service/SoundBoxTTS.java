package com.soundbox.app.service;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.soundbox.app.util.PrefsManager;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

/**
 * Manages Text-to-Speech for payment announcements.
 * Handles queuing, volume control, and repeat announcements.
 */
public class SoundBoxTTS implements TextToSpeech.OnInitListener {

    private static final String TAG = "SoundBoxTTS";
    private static final String UTTERANCE_ID_PREFIX = "soundbox_";

    private TextToSpeech tts;
    private boolean isReady = false;
    private final Context context;
    private final PrefsManager prefs;
    private final Queue<PendingAnnouncement> pendingQueue = new LinkedList<>();
    private int utteranceCounter = 0;

    private static class PendingAnnouncement {
        final String text;
        final int repeatCount;

        PendingAnnouncement(String text, int repeatCount) {
            this.text = text;
            this.repeatCount = repeatCount;
        }
    }

    public SoundBoxTTS(Context context) {
        this.context = context;
        this.prefs = new PrefsManager(context);
        this.tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language based on preference
            String lang = prefs.getLanguage();
            Locale locale;
            switch (lang) {
                case "hi_IN":
                    locale = new Locale("hi", "IN");
                    break;
                case "en_US":
                    locale = Locale.US;
                    break;
                default:
                    locale = new Locale("en", "IN");
                    break;
            }

            int result = tts.setLanguage(locale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default English
                tts.setLanguage(Locale.US);
            }

            // Use STREAM_MUSIC for loud output
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle params = new Bundle();
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
                tts.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build());
            }

            tts.setSpeechRate(prefs.getSpeechRate());
            tts.setPitch(1.0f);

            isReady = true;
            Log.d(TAG, "TTS initialized successfully");

            // Process any pending announcements
            processPendingQueue();
        } else {
            Log.e(TAG, "TTS initialization failed with status: " + status);
        }
    }

    /**
     * Announce the payment text. If TTS isn't ready yet, queues the announcement.
     */
    public void announce(String text) {
        int repeatCount = prefs.getRepeatCount();

        if (!isReady) {
            pendingQueue.add(new PendingAnnouncement(text, repeatCount));
            return;
        }

        // Maximize volume for the announcement
        setMaxVolume();

        speakWithRepeat(text, repeatCount);
    }

    private void speakWithRepeat(String text, int repeatCount) {
        tts.setSpeechRate(prefs.getSpeechRate());

        for (int i = 0; i < repeatCount; i++) {
            String utteranceId = UTTERANCE_ID_PREFIX + (utteranceCounter++);
            int queueMode = (i == 0) ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle params = new Bundle();
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
                tts.speak(text, queueMode, params, utteranceId);
            } else {
                java.util.HashMap<String, String> params = new java.util.HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                tts.speak(text, queueMode, params);
            }

            // Add a small pause between repeats
            if (i < repeatCount - 1) {
                tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, "pause_" + utteranceCounter);
            }
        }
    }

    private void setMaxVolume() {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int targetVolume = (int) (maxVolume * prefs.getVolume() / 100.0f);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set volume", e);
        }
    }

    private void processPendingQueue() {
        while (!pendingQueue.isEmpty()) {
            PendingAnnouncement pending = pendingQueue.poll();
            if (pending != null) {
                setMaxVolume();
                speakWithRepeat(pending.text, pending.repeatCount);
            }
        }
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            isReady = false;
        }
    }

    public boolean isReady() {
        return isReady;
    }
}
