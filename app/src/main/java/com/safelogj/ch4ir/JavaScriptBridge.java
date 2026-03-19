package com.safelogj.ch4ir;

import android.util.Log;
import android.webkit.JavascriptInterface;

import androidx.media3.common.util.UnstableApi;

@UnstableApi
public class JavaScriptBridge {
    public static final String EVENT_READY = "ready";
    public static final String EVENT_PLAY = "play";
    public static final String EVENT_PLAYING = "playing";
    public static final String EVENT_PAUSE = "pause";
    public static final String EVENT_ENDED = "ended";
    public static final String EVENT_ONLINE = "online";
    public static final String EVENT_OFFLINE = "offline";
    public static final String EVENT_SEEK = "seek";
    public static final String EVENT_PLAYBACK_BLOCKED = "playback_blocked";
    public static final String EVENT_CUSTOM_STOP = "twitch_playing_stop";

    private final ExoRecorder exoRecorder;

    public JavaScriptBridge(ExoRecorder exoRecorder) {
        this.exoRecorder = exoRecorder;
    }

    @JavascriptInterface
    public void onEvent(String eventName) {
        try {
            if (eventName != null) {
                switch (eventName) {
                    case EVENT_READY:
                        exoRecorder.setTwitchPlayingStatus(false, EVENT_READY);
                        exoRecorder.startCalculateScale();
                        break;
//                    case EVENT_PLAY:
//                        exoRecorder.setTwitchPlayingStatus(true, EVENT_PLAY);
//                        break;
                    case EVENT_PLAYING:
                        exoRecorder.setTwitchPlayingStatus(true, EVENT_PLAYING);
                        break;
                    case EVENT_PAUSE:
                        exoRecorder.setTwitchPlayingStatus(false, EVENT_PAUSE);
                        break;
//                    case EVENT_ENDED:
//                        break;
                    case EVENT_ONLINE:
                        exoRecorder.setTwitchPlayingStatus(false, EVENT_ONLINE);
                        break;
                    case EVENT_OFFLINE:
                        exoRecorder.setTwitchPlayingStatus(false, EVENT_OFFLINE);
                        break;
                    case EVENT_SEEK:
                        exoRecorder.setTwitchPlayingStatus(false, EVENT_SEEK);
                        break;
                    case EVENT_PLAYBACK_BLOCKED:
                        exoRecorder.setTwitchPlayingStatus(false, EVENT_PLAYBACK_BLOCKED);
                        break;
                    default:
                        Log.d(AppController.LOG_TAG, "Событие дефолтное: " + eventName);
                }
            }
        } catch (Exception e) {
            Log.d(AppController.LOG_TAG, e.getMessage(), e);
        }

    }
}
