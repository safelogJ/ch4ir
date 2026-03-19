package com.safelogj.ch4ir.listeners;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.safelogj.ch4ir.AppController;
import com.safelogj.ch4ir.ExoRecorder;


@UnstableApi
public class ExoListener implements Player.Listener {
    private static final String PLAYER = "player";
    private static final String SPACE = " ";

    private final ExoRecorder mExoRecorder;
    private final ExoPlayer mPlayer;
    private final AppController appController;
    private final int playerId;

    public ExoListener(AppController appController, ExoRecorder mExoRecorder) {
        this.appController = appController;
        this.mExoRecorder = mExoRecorder;
        mPlayer = mExoRecorder.getPlayer();
        this.playerId = mExoRecorder.getPlayerIdx();
    }


    @Override
    public void onEvents(@NonNull Player playerInstance, Player.Events events) {
        for (int i = 0; i < events.size(); i++) {
            int event = events.get(i);
            switch (event) {
                case Player.EVENT_PLAYBACK_STATE_CHANGED:
                    int state = playerInstance.getPlaybackState();
                    if (state == Player.STATE_ENDED) {
                        printLog("EVENT_PLAYBACK_STATE_CHANGED + STATE_ENDED");
                        takeEndReached(); // остановка rtsp
                    } else if (state == Player.STATE_IDLE) {
                        printLog("EVENT_PLAYBACK_STATE_CHANGED + STATE_IDLE");
                    } else if (state == Player.STATE_BUFFERING) {
                        printLog("STATE_BUFFERING_");
                    }
                    break;

                case Player.EVENT_PLAYER_ERROR:
                    printLog("EVENT_PLAYER_ERROR_");
                    takeEventPlayerError();
                    break;

                case Player.EVENT_TRACKS_CHANGED:
                    printLog("EVENT_TRACKS_CHANGED");
                    break;

                case Player.EVENT_MEDIA_ITEM_TRANSITION:
                    printLog("EVENT_MEDIA_ITEM_TRANSITION");
                    break;

                case Player.EVENT_IS_PLAYING_CHANGED:
                    if (mExoRecorder.isLinkPlayingManuallyStart() && playerInstance.getCurrentPosition() > 1000
                    && mExoRecorder.isRtspLink()) {
                        printLog("RTSP выкинуло событие как бы дисконекта, позиция = " + playerInstance.getCurrentPosition());
                       return; // чтоб не "моргало" при дисконекте rtsp
                    }
                    if (!playerInstance.isPlaying()) {
                        takePlayingChangedPause(playerInstance);
                    } else {
                        takePlayingChangePlay(playerInstance);
                    }
                    break;

                case Player.EVENT_POSITION_DISCONTINUITY:
                    printLog("EVENT_POSITION_DISCONTINUITY_");
                    // takePositionChanged(event); // адаптируй под себя
                    break;

                case Player.EVENT_TIMELINE_CHANGED:
                 //   printLog("EVENT_TIMELINE_CHANGED_");
                    break;

                case Player.EVENT_RENDERED_FIRST_FRAME:
                    printLog("EVENT_RENDERED_FIRST_FRAME_");
                    takeRenderedFirstFrame(playerInstance);
                    break;
            }
        }

        mExoRecorder.drawActivityMenuAndFrameBorders();
    }

//    @Override
//    public void onPlayerError(@NonNull PlaybackException error) {
//        Player.Listener.super.onPlayerError(error);
//       printLog(error.getMessage());
//       printLog(error.getErrorCodeName());
//    }
//
//    @Override
//    public void onPlayerErrorChanged(@Nullable PlaybackException error) {
//        Player.Listener.super.onPlayerErrorChanged(error);
//    }

    private void takePlayingChangedPause(Player playerInstance) {
        if (mExoRecorder.isLinkPlayingManuallyStart()) {
            printLog("EVENT_IS_PLAYING_CHANGED == остановка из за дисконекта_или конец клипа в потоке");
         //   mExoRecorder.stopPlayLinkManually();
           // mExoRecorder.stopPlayLinkAuto();
           // mExoRecorder.startPlayLinkAuto();
        }
    }

    private void takeRenderedFirstFrame(Player playerInstance) {
        mExoRecorder.setWasFirstFrame(true);
    }

    private void takePlayingChangePlay(Player playerInstance) {
        printLog("EVENT_PLAYBACK_STATE_CHANGED + STATE_READY == " + playerInstance.isPlaying()  + "stage  = " + playerInstance.getCurrentPosition());
        PlayerView view = mExoRecorder.getPlayerView();
        if (view != null) {
            view.setKeepScreenOn(true);
        }
    }

    private void takeEndReached() {
        if (mExoRecorder.isLinkPlayingManuallyStart()) {
            mExoRecorder.stopPlayLinkManually();
        }
    }

    private void takeEventPlayerError() {
        if (mExoRecorder.isLinkPlayingManuallyStart()) {
            mExoRecorder.showErrorPage();
            mExoRecorder.stopPlayLinkManually();
        }
    }

    private void printLog(String message) {
        String log = PLAYER + playerId + SPACE + message;
        Log.d(AppController.LOG_TAG, log);
    }
}
