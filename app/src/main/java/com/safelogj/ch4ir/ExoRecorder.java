package com.safelogj.ch4ir;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ClientCertRequest;
import android.webkit.ConsoleMessage;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;

import com.safelogj.ch4ir.listeners.ExoListener;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@UnstableApi
public class ExoRecorder {

    public static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Pixel 3 XL) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36";
    public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
    private static final Map<Integer, String> SCALE_TEXT_CACHE = new HashMap<>();
    private final Map<String, String> extraHeaders = Map.of("User-Agent", MOBILE_USER_AGENT);
    private final ExoPlayer mPlayer;
    private static final String BLANK_PAGE = "<html><body style='background-color:black;'></body></html>";
    private static final String SCALE_PATTERN = "%.2f";
    private static final String SCALE_DEFAULT_TEXT = "1.00";
    private static final float SCALE_DEFAULT_VALUE = 1f;
    private static final String MIME_TYPE = "text/html";
    private static final String UTF_8 = "UTF-8";
    private static final String NEW_STRING = "\n";
    private static final String ANDROID_INTERFACE = "AndroidInterface";
    private static final String WINDOW_PLAYER_READY = "window.playerReady";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String TWITCH_MUTE_SCRIPT = "player.setMuted(true);";
    private static final String TWITCH_UNMUTE_SCRIPT = "player.setMuted(false);";
    private static final String TWITCH_SET_VOLUME_SCRIPT = "player.setVolume(%.2f);";
    private static final String TWITCH_GET_VOLUME_SCRIPT = "player.getVolume();";
    private static final float VOLUME_MUTE = 0f;
    private static final float VOLUME_FULL = 1f;
    private final AppController appController;
    private final int playerIdx;
    private final RtspMediaSource.Factory rtspTcpFactory = new RtspMediaSource.Factory().setForceUseRtpTcp(true);
    private final RtspMediaSource.Factory rtspUdpFactory = new RtspMediaSource.Factory();
    private final DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true);
    private final HlsMediaSource.Factory hlsFactory = new HlsMediaSource.Factory(httpDataSourceFactory);
    private final DashMediaSource.Factory dashFactory = new DashMediaSource.Factory(httpDataSourceFactory);

    private WeakReference<MainActivity> activityRef;
    private WeakReference<PlayerView> exoViewRef;
    private WeakReference<WebView> webViewRef;
    private Content playingContent;
    private boolean isLinkPlayingManuallyStart;
    private boolean wasFirstFrame;
    private boolean isTwitchPlaying;
    private float loadingScale = -1f;
    private float onStartPlayScale = -1f;
    private float twitchVolume = 0f;
    private String scaleText = SCALE_DEFAULT_TEXT;

    public ExoRecorder(AppController appController, int playerIdx) {
        this.appController = appController;
        this.playerIdx = playerIdx;
        mPlayer = new ExoPlayer.Builder(appController).setLoadControl(getLoadController()).build();
        mPlayer.addListener(new ExoListener(appController, this));
        playingContent = appController.getEmptyContent();
    }

    public ExoPlayer getPlayer() {
        return mPlayer;
    }

    public int getPlayerIdx() {
        return playerIdx;
    }

    public boolean isLinkPlayingManuallyStart() {
        return isLinkPlayingManuallyStart;
    }

    public String getScaleText() {
        return scaleText;
    }

    public void switchMuteByTwitchUser() {
            WebView webView = webViewRef != null ? webViewRef.get() : null;
            if (webView != null) {
                webView.evaluateJavascript(WINDOW_PLAYER_READY, value -> {
                    if (TRUE.equals(value)) {
                        saveTwitchVolume();
                        webView.evaluateJavascript("player.getMuted();", muted -> {
                            if (muted.equals(TRUE) && !isManuallyMuted()) {
                                mPlayer.setVolume(VOLUME_MUTE);
                            } else if (muted.equals(FALSE) && isManuallyMuted()) {
                                mPlayer.setVolume(VOLUME_FULL);
                            }
                            MainActivity activity = activityRef != null ? activityRef.get() : null;
                            if (activity != null) {
                                activity.drawMenuAndFrameBorders(playerIdx);
                            }

                        });
                    }
                });
            }
    }

    public boolean isTwitchPlaying() {
        return isTwitchPlaying;
    }

    public void startCalculateScale() {
        MainActivity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (loadingScale > 0) {
                    onStartPlayScale = loadingScale;
                    Log.d(AppController.LOG_TAG, "Стартовой масштаб назначен = " + loadingScale);
                    setScaleText(SCALE_DEFAULT_VALUE);
                } else {
                  //  onStartPlayScale = -1;
                    WebView view = webViewRef != null ? webViewRef.get() : null;
                    if (view != null) {
                        float hardScale = view.getScale();
                        onStartPlayScale = hardScale > 0 ? hardScale : -1;
                        Log.d(AppController.LOG_TAG, "Стартовой масштаб назначен жёстко = " + onStartPlayScale);
                    }
                }
            });
        }
    }

    public void setTwitchPlayingStatus(boolean twitchPlaying, String event) {
        MainActivity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            activity.runOnUiThread(() -> {
                Log.d(AppController.LOG_TAG, "Событие: " + event);
                isTwitchPlaying = twitchPlaying;
                if (event.equals(JavaScriptBridge.EVENT_READY)) {
                    setWasFirstFrame(true);
                    setTwitchPlayerMute(isManuallyMuted());
                }
                activity.drawMenuAndFrameBorders(playerIdx);
            });
        }
    }

    public void setScaleText(float scaleValue) {
        int key = Math.round(scaleValue * 100);
        String cached = SCALE_TEXT_CACHE.get(key);
        if (cached == null) {
            cached = String.format(Locale.US, SCALE_PATTERN, scaleValue);
            SCALE_TEXT_CACHE.put(key, cached);
        }
        scaleText = cached;
        MainActivity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            activity.setScaleText(cached);
        }
    }

    public PlayerView getPlayerView() {
        return exoViewRef != null ?  exoViewRef.get() : null;
    }

    public void setActivity(WeakReference<MainActivity> activityRef) {
        this.activityRef = activityRef;
    }

    public void cleanUp() {
        activityRef = null;
        exoViewRef = null;
        webViewRef = null;
    }

    public void setWebView(WeakReference<WebView> webViewRef) {
        this.webViewRef = webViewRef;
        if (webViewRef != null) {
            WebView webView = webViewRef.get();
            if (webView != null) {
                initWebClient(webView);
            }
        }
    }

    public void setLinkPlayingManuallyStart(boolean linkPlayingManuallyStart) {
        isLinkPlayingManuallyStart = linkPlayingManuallyStart;
    }

    public void setExoView(WeakReference<PlayerView> exoViewRef) {
        this.exoViewRef = exoViewRef;
    }


    public void drawActivityMenuAndFrameBorders() {
        MainActivity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            activity.drawMenuAndFrameBorders(playerIdx);
        }
    }

    public void onStartRestartPlayer() {
        if (isLinkPlayingManuallyStart()) {
            startPlayLinkAuto();
        }
    }

    public void onStopRestartPlayer() {
        if (isLinkPlayingManuallyStart()) {
            stopPlayLinkAuto();
        }
    }

    public void startPlayLinkManually(Content content) {
        setLinkPlayingManuallyStart(true);
        playingContent = content;
        setWasFirstFrame(false);
        startPlayLink();
        drawBtnForTwitchEvent();
    }

    public void startPlayLinkAuto() {
        startPlayLink();
    }

    public void stopPlayLinkManually() {
        setLinkPlayingManuallyStart(false);
        stopPlayLink();
        drawBtnForTwitchEvent();
    }

    public void stopPlayLinkAuto() {
        stopPlayLink();
    }

    public boolean isManuallyMuted() {
        return mPlayer.getVolume() == VOLUME_MUTE;
    }

    public void switchMuteByButton() {
        float volume = mPlayer.getVolume();
        if (volume == VOLUME_MUTE) {
            mPlayer.setVolume(VOLUME_FULL);
        } else {
            mPlayer.setVolume(VOLUME_MUTE);
        }

        setTwitchPlayerMute(volume != VOLUME_MUTE);
       // Log.e(AppController.LOG_TAG, "Твичу отправлен с кнопки команда мут = " + (volume != VOLUME_MUTE));
        MainActivity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            activity.drawMenuAndFrameBorders(playerIdx);
        }
    }


    private void setTwitchPlayerMute(boolean needMuted) {
        WebView webView = webViewRef != null ? webViewRef.get() : null;
        if (webView != null) {
            webView.evaluateJavascript(WINDOW_PLAYER_READY, value -> {
                //   Log.e(AppController.LOG_TAG, "Твичу пришла команда мут.анму, а твич реди статус = " + value);
                if (TRUE.equals(value)) {
                    if (needMuted) {
                        saveTwitchVolume();
                        webView.evaluateJavascript(TWITCH_MUTE_SCRIPT, null);
                        //   Log.e(AppController.LOG_TAG, "твичу пришла команда сделать мут" );
                    } else {
                        webView.evaluateJavascript(TWITCH_UNMUTE_SCRIPT, null);
                        //   Log.e(AppController.LOG_TAG, "твичу пришла команда сделать анмут" );
                        if (twitchVolume == 0f) {
                            webView.evaluateJavascript(String.format(Locale.ENGLISH, TWITCH_SET_VOLUME_SCRIPT, 0.1f), null);
                            //  Log.e(AppController.LOG_TAG, "При анмуте громкость была 0: установлена 0.1" );
                        } else {
                            webView.evaluateJavascript(String.format(Locale.ENGLISH, TWITCH_SET_VOLUME_SCRIPT, twitchVolume), null);
                            // Log.e(AppController.LOG_TAG, "При анмуте установлена громкость" + twitchVolume);
                        }
                    }
                }
            });
        }

    }

    private void saveTwitchVolume() {
        WebView webView = webViewRef != null ? webViewRef.get() : null;
        if (webView != null) {
            webView.evaluateJavascript(TWITCH_GET_VOLUME_SCRIPT, volume -> {
                try {
                    twitchVolume = Float.parseFloat(volume);
                    // Log.e(AppController.LOG_TAG, "Сохранена громкость: " + twitchVolume);
                } catch (Exception e) {
                    Log.e(AppController.LOG_TAG, "Ошибка при чтении громкости: " + e.getMessage());
                }
            });
        }
    }

    public void setWasFirstFrame(boolean wasFirstFrame) {
        this.wasFirstFrame = wasFirstFrame;
    }

    public String getPlayingInfo() {
        return playingContent.getInfo();
    }

    public boolean isRtspLink() {
        return playingContent.getLinkType() == Content.RTSP_LINK_TYPE;
    }

    public boolean isTwitchLink() {
        return playingContent.getLinkType() == Content.TWITCH_LINK_DESKTOP_TYPE || playingContent.getLinkType() == Content.TWITCH_LINK_MOBILE_TYPE;
    }


    private void startPlayLink() {
        if (playingContent.getLinkType() == Content.RTSP_LINK_TYPE) {
            playRtspLink();
        } else if (playingContent.getLinkType() == Content.M3U8_LINK_TYPE) {
            playM3U8Link();
        } else if (playingContent.getLinkType() == Content.MPD_LINK_TYPE) {
            playMpdLink();
        } else if (!appController.isWebViewError() && (playingContent.getLinkType() == Content.TWITCH_LINK_DESKTOP_TYPE
                || playingContent.getLinkType() == Content.TWITCH_LINK_MOBILE_TYPE)) {
            playTwitchLink();
        } else {
            stopPlayLinkManually();
            sendErrorLinkFormat();
        }
    }

    private void stopPlayLink() {
        setScaleText(SCALE_DEFAULT_VALUE);
        if (isTwitchLink()) {
            stopTwitchLink();
            WeakReference<WebView> localWebViewRef = webViewRef;
            WebView webView = localWebViewRef != null ? localWebViewRef.get() : null;

            if (webView != null) {
                WeakReference<WebView> safeWebViewRef = new WeakReference<>(webView);
                webView.post(() -> {
                    WebView safeWebView = safeWebViewRef.get();
                    if (safeWebView != null) {
                        while (safeWebView.zoomOut()) {
                            // ...
                        }
                    }
                });
            }

        } else {
            mPlayer.clearMediaItems();
            mPlayer.stop();
            MainActivity activity = activityRef != null ? activityRef.get() : null;
            if (activity != null) {
                activity.resetScale(playerIdx);
            }
        }
    }

    private void playRtspLink() {
        RtspMediaSource mediaSource = rtspTcpFactory.createMediaSource(MediaItem.fromUri(playingContent.getRealLink()));
        try {
            mPlayer.setMediaSource(mediaSource);
            mPlayer.prepare();
            mPlayer.play();
        } catch (Exception e) {
            mediaSource = rtspUdpFactory.createMediaSource(MediaItem.fromUri(playingContent.getRealLink()));
            mPlayer.setMediaSource(mediaSource);
            mPlayer.prepare();
            mPlayer.play();
        }
        MainActivity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            activity.hideWebView(playerIdx);
        }
    }

    private void playM3U8Link() {
        Uri uri = Uri.parse(playingContent.getRealLink());
        MediaItem mediaItem = MediaItem.fromUri(uri);
        HlsMediaSource mediaSource;
        String userAgent = playingContent.getUserAgent();
        if (userAgent.isEmpty()) {
            mediaSource = hlsFactory.createMediaSource(mediaItem);
        } else {
            mediaSource = getMediaSourceWithUserAgent(mediaItem, userAgent);
        }

        mPlayer.setMediaSource(mediaSource);
        mPlayer.prepare();
        mPlayer.play();
        MainActivity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            activity.hideWebView(playerIdx);
        }
    }

    private HlsMediaSource getMediaSourceWithUserAgent(MediaItem mediaItem, String userAgent) {
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent(userAgent);
        return new HlsMediaSource.Factory(httpFactory)
                .createMediaSource(mediaItem);
    }

    private void playMpdLink() {
        Uri uri = Uri.parse(playingContent.getRealLink());
        MediaItem mediaItem = MediaItem.fromUri(uri);
        DashMediaSource mediaSource = dashFactory.createMediaSource(mediaItem);
        mPlayer.setMediaSource(mediaSource);
        mPlayer.prepare();
        mPlayer.play();
        MainActivity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            activity.hideWebView(playerIdx);
        }
    }


    private void playTwitchLink() {
        WebView webView = webViewRef != null ? webViewRef.get() : null;
        if (webView != null) {
         //   webView.onResume();          // Возобновляет обработчики
//            webView.resumeTimers();      // Запускает таймеры обратно
            webView.loadUrl(playingContent.getRealLink(), extraHeaders);
        }
    }

    private void stopTwitchLink() {
        setTwitchPlayingStatus(false, JavaScriptBridge.EVENT_CUSTOM_STOP);
        setWasFirstFrame(false);
        WebView webView = webViewRef != null ? webViewRef.get() : null;
        if (webView != null) {
            webView.stopLoading();
            Log.d(AppController.LOG_TAG, "webView.stopLoading()");
            webView.loadDataWithBaseURL(null, BLANK_PAGE, MIME_TYPE, UTF_8, null);
            // webView.onPause();
            // webView.pauseTimers();
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    private void initWebClient(WebView webView) {
        try {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    // вызывается перед тем, как WebView начнёт загружать URL по ссылке, нажатой пользователем.
                    return super.shouldOverrideUrlLoading(view, request);
                }

                @Override
                public void onLoadResource(WebView view, String url) {
                    //   вызывается: Каждый раз, когда WebView начинает загружать любой ресурс (изображения, JS, CSS, и т.д.)
                    super.onLoadResource(view, url);
                }

                @Override
                public void onPageCommitVisible(WebView view, String url) {
                    // вызывается: Когда WebView отображает содержимое страницы (после завершения парсинга и рендера DOM).
                    super.onPageCommitVisible(view, url);
                }

                @Nullable
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    // вызывается: Перед загрузкой каждого ресурса (HTML, JS, CSS, изображение).
                    //Для чего: Можно перехватить и заменить/заблокировать ресурсы.
                    //  Log.d(AppController.LOG_TAG, "Событие WebView = shouldInterceptRequest" + request.getRequestHeaders());
                    return super.shouldInterceptRequest(view, request);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    // вызывается: При общей ошибке загрузки (например, нет интернета, DNS-фейл, SSL-ошибка и т.п.)
// ERROR_CONNECT = -6 ERROR_TIMEOUT = -8 ERROR_HOST_LOOKUP = -2 ERROR_UNSUPPORTED_AUTH_SCHEME = -3 ERROR_FAILED_SSL_HANDSHAKE = -11
                    super.onReceivedError(view, request, error);
                    Log.d(AppController.LOG_TAG, "Событие WebView = ReceivedError типа нет интернета = " + error.getErrorCode());
                    if ((!wasFirstFrame && isTwitchFatalError(error.getErrorCode())) || request.isForMainFrame()) {
                          showErrorPage();
                    }
                }

                @Override
                public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                    //  вызывается: Когда сервер вернул HTTP-ошибку (например 404, 500 и т.д.)
                    //Важно: WebView всё равно отобразит страницу — это не "фатальная" ошибка как onReceivedError.
                    super.onReceivedHttpError(view, request, errorResponse);
                    Log.d(AppController.LOG_TAG, "Событие WebView = ReceivedHttpError = " + errorResponse);

                    int statusCode = errorResponse.getStatusCode();
                    String reasonPhrase = errorResponse.getReasonPhrase();
                    Uri url = request.getUrl();

                    Log.e(AppController.LOG_TAG, "HTTP error " + statusCode + " (" + reasonPhrase + ") on URL: " + url);

                    // Пример: показать сообщение, если ошибка у основного документа
                    if (request.isForMainFrame()) {
                        // Покажи ошибку пользователю, или подгрузи локальную страницу
                    }
                }

                @Override
                public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                    super.onFormResubmission(view, dontResend, resend);
                    Log.d(AppController.LOG_TAG, "Событие WebView = onFormResubmission");
                }

                @Override
                public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                    super.doUpdateVisitedHistory(view, url, isReload);
                    Log.d(AppController.LOG_TAG, "Событие WebView = doUpdateVisitedHistory");
                }

                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                    // вызывается: Если у сайта проблемы с SSL-сертификатом.
                    //Важно: Ты должен вызвать handler.proceed() (игнорировать) или handler.cancel().
                    super.onReceivedSslError(view, handler, error);
                    Log.d(AppController.LOG_TAG, "Событие WebView = onReceivedSslError");
                }

                @Override
                public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                    // вызывается: Если сервер запрашивает HTTP-авторизацию (basic auth).
                    super.onReceivedHttpAuthRequest(view, handler, host, realm);
                    Log.d(AppController.LOG_TAG, "Событие WebView = onReceivedHttpAuthRequest");
                }

                @Override
                public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
                    // вызывается: Когда сервер запрашивает клиентский сертификат (TLS mutual auth). Редко используется, в основном в корпоративных сетях.
                    super.onReceivedClientCertRequest(view, request);
                    Log.d(AppController.LOG_TAG, "Событие WebView = onReceivedClientCertRequest");
                }

                @Override
                public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                    Log.d(AppController.LOG_TAG, "Событие WebView = shouldOverrideKeyEvent");
                    return super.shouldOverrideKeyEvent(view, event);

                }

                @Override
                public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                    super.onUnhandledKeyEvent(view, event);
                    Log.d(AppController.LOG_TAG, "Событие WebView = onUnhandledKeyEvent");
                }

                @Override
                public void onReceivedLoginRequest(WebView view, String realm, @Nullable String account, String args) {
                    // вызывается: Для автоматического логина с использованием учетных данных WebView (устаревший механизм, сейчас почти не используется).
                    super.onReceivedLoginRequest(view, realm, account, args);
                    Log.d(AppController.LOG_TAG, "Событие WebView = onReceivedLoginRequest");
                }

                @Override
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    Log.d(AppController.LOG_TAG, "Событие WebView = onRenderProcessGone = краш рендеринга");
                    return super.onRenderProcessGone(view, detail);
                }

                @Override
                public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType, SafeBrowsingResponse callback) {
                    // вызывается: Когда WebView определяет, что страница потенциально вредоносная (фишинг, вирусы).
                    super.onSafeBrowsingHit(view, request, threatType, callback);
                    Log.d(AppController.LOG_TAG, "Событие WebView = onSafeBrowsingHit");
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    Log.d(AppController.LOG_TAG, "Событие WebView = onPageStarted");
                    resetTwitchScale();
                }

                @Override
                public void onScaleChanged(WebView view, float oldScale, float newScale) {
                    super.onScaleChanged(view, oldScale, newScale);
                    Log.d(AppController.LOG_TAG, "масштабирование идёт ");

                    if (isLinkPlayingManuallyStart && !isTwitchLink()) {
                        MainActivity activity = activityRef != null ? activityRef.get() : null;
                        if (activity != null) {
                            activity.hideWebView(playerIdx);
                        }
                    }

                    if (newScale <= 0f || !isLinkPlayingManuallyStart) {
                        Log.d(AppController.LOG_TAG, "Старт масштаба меньше 0 или не проигрывается и инит = " + newScale);
                        return;
                    }

                    if (newScale > 0f && newScale <= 5.0f) {
                        loadingScale = newScale;
                        Log.d(AppController.LOG_TAG, "масштабирование идёт loadingScale = newScale = " + loadingScale);
                    }
                    if (onStartPlayScale > 0) {
                        float scale = newScale / onStartPlayScale;
                        if (scale < 1f) return;
                        setScaleText(scale);
                    } else {
                        Log.d(AppController.LOG_TAG, "onStartPlayScale не больше 0 =  " + onStartPlayScale);
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    MainActivity activity = activityRef != null ? activityRef.get() : null;
                    if (activity != null) {
                        activity.drawMenuAndFrameBorders(playerIdx);
                        activity.hideExoView(playerIdx);
                    }
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    String msg = consoleMessage.message();
//                    if(isTwitchFatalError(msg)) {
//                        appController.setWebViewError(true, playerIdx);
//                    }
                    Log.d("google",
                            msg + " -- From line "
                                    + consoleMessage.lineNumber() + " of "
                                    + consoleMessage.sourceId());
                    return true;
                }

            });

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setUserAgentString(MOBILE_USER_AGENT);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setSupportZoom(true);
            webView.addJavascriptInterface(new JavaScriptBridge(this), ANDROID_INTERFACE);
            webView.setLongClickable(false);
            webView.setHapticFeedbackEnabled(false);
            webView.setOnLongClickListener(v -> true);
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null); // вкл ускорение
            stopTwitchLink();
            appController.setWebViewError(false, playerIdx);
        } catch (Exception e) {
            appController.setWebViewError(true, playerIdx);
        }
    }

    private void resetTwitchScale() {
        onStartPlayScale = -1f;
        loadingScale = -1f;
    }

    public void showErrorPage() {
        if (isLinkPlayingManuallyStart && !wasFirstFrame) {
            if (isTwitchLink()) {
                setLinkPlayingManuallyStart(false);
                setTwitchPlayingStatus(false, JavaScriptBridge.EVENT_CUSTOM_STOP);
            }
            String errorPage = Content.getErrorPage(appController.getText(R.string.falied_open_link).toString(), playingContent.getUserLink());
            WebView webView = webViewRef != null ? webViewRef.get() : null;
            if (webView != null) {
                webView.loadDataWithBaseURL(null, errorPage, MIME_TYPE, UTF_8, null);
            }
        }
    }

    private void sendErrorLinkFormat() {
        StringBuilder builder = new StringBuilder(appController.getText(R.string.falied_link_format));
        builder.append(NEW_STRING).append(playingContent.getUserLink());
        Toast.makeText(appController, builder, Toast.LENGTH_LONG).show();
    }

    private DefaultLoadControl getLoadController() {
        return new DefaultLoadControl.Builder().setBufferDurationsMs(5000, 15000, 1000, 2000).build();
    }

    private boolean isTwitchFatalError(int errorCode) {
        return switch (errorCode) {
            case -2, -8, -6, -7, -11 -> true;
            default -> false;
        };
//        return msg.contains("PlaybackAccessToken: server error")
//                || msg.contains("[VideoPlayer] An unhandled exception")
//                || msg.contains("globalThis is not defined")
//                || msg.contains("WeakRef is not defined")
//                || msg.contains("Uncaught ReferenceError")
//                || msg.toLowerCase().contains("fatal error");
    }

    private void drawBtnForTwitchEvent() {
        if (isTwitchLink()) {
            MainActivity activity = activityRef != null ? activityRef.get() : null;
            if (activity != null) {
                activity.drawMenuAndFrameBorders(playerIdx);
            }
        }
    }
}
