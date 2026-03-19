package com.safelogj.ch4ir;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.safelogj.ch4ir.databinding.ActivityMainBinding;
import com.safelogj.ch4ir.databinding.BottomSheetLayoutBinding;
import com.safelogj.ch4ir.databinding.DeleteLinkDialogBinding;
import com.safelogj.ch4ir.databinding.SendLinkDialogBinding;
import com.safelogj.ch4ir.listeners.ExoTouchZoomPanListener;
import com.safelogj.ch4ir.listeners.MenuTextDoubleTapListener;
import com.safelogj.ch4ir.listeners.TwitchTouchListener;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UnstableApi
public class MainActivity extends AppCompatActivity {

    private static final String QUESTION = " ?";
    private static final String EMPTY_STRING = "";
    private static final String SPACE = " ";
    @SuppressLint("AuthLeak")
    private static final String[] RTSP_EXAMPLES = {
            "twitch desktop: https://www.twitch.tv/channel",
            "twitch mobile: https://m.twitch.tv/channel",
            "m3u8: https://you.link/you/content.m3u8",
            "mpd: https://you.link/you/content.mpd",
            "Dahua: rtsp://user:password@my_site_ip:port/cam/realmonitor?channel=1&subtype=1",
            "Rvi: rtsp://user:password@my_site_ip:port/RVi/1/2",
            "Hikvision: rtsp://user:password@my_site_ip:port/Streaming/Channels/101",
            "TP-Link Tapo: rtsp://user:password@my_site_ip:port/stream1",
            "Uniview: rtsp://user:password@my_site_ip:port/media/video1",
            "Axis: rtsp://user:password@my_site_ip:port/axis-media/media.amp",
            "EZVIZ: rtsp://user:password@my_site_ip:port/h264/ch1/main/av_stream",
            "Foscam: rtsp://user:password@my_site_ip:port/videoMain",
            "Reolink: rtsp://user:password@my_site_ip:port/h264Preview_01_main",
            "Lorex: rtsp://user:password@my_site_ip:port/Streaming/Channels/102",
            "Vivotek: rtsp://user:password@my_site_ip:port/live.sdp",
            "Bosch: rtsp://user:password@my_site_ip:port/rtsp_tunnel",
            "Milesight: rtsp://user:password@my_site_ip:port/streaming/channels/101",
            "Avigilon: rtsp://user:password@my_site_ip:port/defaultPrimary",
            "Grandstream: rtsp://user:password@my_site_ip:port/0"
    };
    private static final String IP_TV = "https://github.com/iptv-org/iptv/tree/master/streams";
    private final PlayerView[] mExoViews = new PlayerView[AppController.PLAYERS_COUNT];
    private final ExoTouchZoomPanListener[] mTouchListeners = new ExoTouchZoomPanListener[AppController.PLAYERS_COUNT];
    private final FrameLayout[] mFrameViews = new FrameLayout[AppController.PLAYERS_COUNT];
    private final WebView[] mWebViews = new WebView[AppController.PLAYERS_COUNT];
    private final TwitchTouchListener[] mTwitchTouchListeners = new TwitchTouchListener[AppController.PLAYERS_COUNT];
    private final List<AlertDialog> openedDialogList = new ArrayList<>(10);
    private ActivityMainBinding mBinding;
    private BottomSheetLayoutBinding mMenuBinding;
    private BottomSheetDialog mBottomSheetDialog;
    private ExoRecorder[] exoPlayers;
    private AppController appController;
    private int mOpenedMenuPlayerId = -1;
    private int alertDialogHeight;
    private int alertDialogWidth;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ColorStateList mDialogCheckBoxTintList;
    private ClipboardManager clipboard;
    private boolean isFullScreen;
    private int mFullScreenedPlayer;
    private WeakReference<MainActivity> activityRef;
    private final ActivityResultCallback<ActivityResult> callbackForGeneralPermitURI = result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
                final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    Log.d(AppController.LOG_TAG, "Разрешение на URI сохранено: " + uri);
                } catch (SecurityException e) {
                    Log.d(AppController.LOG_TAG, "Ошибка получения разрешений на URI: " + e.getMessage(), e);
                }
                DocumentFile documentFile = DocumentFile.fromSingleUri(MainActivity.this, uri);
                if (documentFile == null || !documentFile.exists()) {
                    Log.d(AppController.LOG_TAG, "Файл не найден или путь неверен!");
                    return;
                }
                Map<String, Content> fileLinks = readLinksFromUserFile(uri);
                if (fileLinks.isEmpty()) {
                    showDialogEmptyFile();
                } else {
                    showDialogAddContentFromFile(fileLinks);
                }
            }
        }
    };
    private final ActivityResultLauncher<Intent> requestGeneralPermitURI =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), callbackForGeneralPermitURI);

    private final ActivityResultCallback<Boolean> callbackAskReadFilePermit = result -> {
        if (Boolean.TRUE == result) {
            requestGeneralPermitURI.launch(getIntentActionOpenDoc());
        }
    };
    private final ActivityResultLauncher<String> requestAskReadFilePermit =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), callbackAskReadFilePermit);

    public void drawMenuAndFrameBorders(int playerId) {
        if (playerId != mOpenedMenuPlayerId) {
            drawFrameBorders(playerId);
            return;
        }

        if (exoPlayers[playerId].getPlayer().isPlaying() || exoPlayers[playerId].isTwitchPlaying()) {
            mMenuBinding.menuPlayText.setVisibility(View.VISIBLE);
            mMenuBinding.menuPlayTextScroller.setVisibility(View.VISIBLE);
            mMenuBinding.menuPlayText.setText(exoPlayers[playerId].getPlayingInfo());
            mMenuBinding.openLinkOrStopBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.fill_stop_btn_48px));
            if (exoPlayers[playerId].isManuallyMuted()) {
                mFrameViews[playerId].setBackground(AppCompatResources.getDrawable(this, R.drawable.frame_background_border_mute));
            } else {
                mFrameViews[playerId].setBackground(AppCompatResources.getDrawable(this, R.drawable.frame_background_border_play));
            }

        } else {
            if (exoPlayers[playerId].isLinkPlayingManuallyStart()) {
                mMenuBinding.openLinkOrStopBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.fill_stop_btn_48px));
            } else {
                mMenuBinding.openLinkOrStopBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.play_bookmark_48px));
            }
            mMenuBinding.menuPlayText.setVisibility(View.GONE);
            mMenuBinding.menuPlayTextScroller.setVisibility(View.GONE);
            mFrameViews[playerId].setBackground(AppCompatResources.getDrawable(this, R.drawable.frame_background_border_free));
        }

        if (exoPlayers[playerId].isManuallyMuted()) {
            mMenuBinding.muteBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.volume_off_48px));
            mMenuBinding.muteBtn.getDrawable().setTint(getColor(R.color.yellow_mute));
        } else {
            mMenuBinding.muteBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.volume_up_48px));
            mMenuBinding.muteBtn.getDrawable().setTint(getColor(R.color.blue_50));
        }
    }

    public void setClipboardText() {
        String text = mMenuBinding.menuPlayText.getText().toString();
        ClipData clip = ClipData.newPlainText(null, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, getString(R.string.copied_text), Toast.LENGTH_SHORT).show();
        }
    }

    public void openMenu(View vlcView) {
        for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
            if (mExoViews[i] == vlcView || mWebViews[i] == vlcView) {
                mOpenedMenuPlayerId = i;
                drawMenuAndFrameBorders(mOpenedMenuPlayerId);
                mBottomSheetDialog.show();
                return;
            }
        }
    }

    public void setScaleText(String scale) {
        if (mBinding.scaleTextView != null) {
            mBinding.scaleTextView.setText(scale);
        }
    }

    public void resetScale(int index) {
        mTouchListeners[index].resetZoomPan();
    }

    public void fullScreenVLC(View view) {
        if (isFullScreen) {
            isFullScreen = false;
            setFullScreenOff();
        } else {
            isFullScreen = true;
            setFullScreenOn(view);
        }
    }

    public void hideWebView(int idx) {
        if (mWebViews[idx] != null) {
            mWebViews[idx].setVisibility(View.GONE);
        }
        mExoViews[idx].setVisibility(View.VISIBLE);
    }

    public void hideExoView(int idx) {
        mExoViews[idx].setVisibility(View.GONE);
        if (mWebViews[idx] != null) {
            mWebViews[idx].setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0 &&
                event.getAction() == MotionEvent.ACTION_SCROLL) {
            float scrollDelta = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (scrollDelta != 0) {
                float focusX = event.getRawX(); // ВАЖНО: getRawX/Y — координаты экрана
                float focusY = event.getRawY();
                int frameIdx = isPointInsideView(focusX, focusY);
                if (frameIdx != -1 && exoPlayers[frameIdx].isLinkPlayingManuallyStart()) {
                    if (exoPlayers[frameIdx].isTwitchLink() && mWebViews[frameIdx] != null) {
                        if (scrollDelta > 0) {
                            mWebViews[frameIdx].zoomIn();
                        } else {
                            mWebViews[frameIdx].zoomOut();
                        }
                    } else {
                        float zoomStep = 0.1f * Math.signum(scrollDelta);
                        mTouchListeners[frameIdx].onScrollFromMouse(zoomStep, focusX, focusY);
                    }
                    return true;
                }

            }
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @SuppressLint({"ClickableViewAccessibility", "InlinedApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityRef = new WeakReference<>(this);
        appController = (AppController) getApplication();
        appController.setHasRedirectedOnce(true);
        EdgeToEdge.enable(this);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        mMenuBinding = BottomSheetLayoutBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mBottomSheetDialog = new BottomSheetDialog(this, R.style.Theme_MyApp_BottomSheet);
        mBottomSheetDialog.setContentView(mMenuBinding.getRoot());
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) mMenuBinding.menuOuter.getParent());
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setPeekHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
        setEdgePadding();
        setLightBarColor(getWindow(), ContextCompat.getColor(this, R.color.black));
        exoPlayers = appController.getExoPlayers();
        setFrameViews();
        setExoViews();
        setWebViews();
        setExoViewsListener();
        setupPlayers();
        setMenuBtn1OpenLinkOrStop();
        setMenuSlideListeners(mBottomSheetDialog);
        mMenuBinding.addLinkBtn.setOnClickListener(view -> showAddLinkDialog());
        mMenuBinding.removeLinkBtn.setOnClickListener(view -> showRemoveLinkDialog());
        MenuTextDoubleTapListener menuTextDoubleTapListener = new MenuTextDoubleTapListener(activityRef);
        mMenuBinding.menuPlayText.setOnTouchListener(menuTextDoubleTapListener);
        mMenuBinding.muteBtn.setOnClickListener(view -> exoPlayers[mOpenedMenuPlayerId].switchMuteByButton());
        alertDialogHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);
        alertDialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        mDialogCheckBoxTintList = ContextCompat.getColorStateList(this, R.color.dialog_checkbox_selector);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mWebViews[0] != null) {
            mWebViews[0].resumeTimers();
        }
        for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
            if (exoPlayers[i] != null) {
                exoPlayers[i].onStartRestartPlayer();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
            if (exoPlayers[i] != null) {
                exoPlayers[i].onStopRestartPlayer();
            }
        }
        if (mWebViews[0] != null) {
            mWebViews[0].pauseTimers();
        }
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
            if (mTwitchTouchListeners[i] != null) {
                mTwitchTouchListeners[i].cleanup();
            }
            if (mExoViews[i] != null) {
                mExoViews[i].setOnTouchListener(null);
                mExoViews[i].setPlayer(null);
            }
            if (mTouchListeners[i] != null) {
                mTouchListeners[i].cleanup();
            }
            exoPlayers[i].cleanUp();
        }
        handler.removeCallbacksAndMessages(null);
        clearDialogsAndMenu();
        super.onDestroy();
    }

    private void setFrameViews() {
        mFrameViews[0] = mBinding.exoFrame1;
        mFrameViews[1] = mBinding.exoFrame2;
        mFrameViews[2] = mBinding.exoFrame3;
        mFrameViews[3] = mBinding.exoFrame4;
    }

    private void setExoViews() {
        mExoViews[0] = mBinding.exoView1;
        mExoViews[1] = mBinding.exoView2;
        mExoViews[2] = mBinding.exoView3;
        mExoViews[3] = mBinding.exoView4;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setWebViews() {
        for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
            try {
                mWebViews[i] = new WebView(this);
                mWebViews[i].setLayoutParams(new FrameLayout.LayoutParams(
                        MATCH_PARENT,
                        MATCH_PARENT
                ));
                mFrameViews[i].addView(mWebViews[i], 1);
                mTwitchTouchListeners[i] = new TwitchTouchListener(activityRef, exoPlayers[i]);
                mWebViews[i].setOnTouchListener(mTwitchTouchListeners[i]);

            } catch (Exception e) {
                appController.setWebViewError(true, i);
            }
        }
    }

    private void setupPlayers() {
        for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
            if (exoPlayers[i] != null) {
                exoPlayers[i].setActivity(activityRef);
                exoPlayers[i].setExoView(new WeakReference<>(mExoViews[i]));
                exoPlayers[i].setWebView(new WeakReference<>(mWebViews[i]));
                mExoViews[i].setPlayer(exoPlayers[i].getPlayer());
                mExoViews[i].setKeepScreenOn(true);
            }
        }
    }

    private void setMenuBtn1OpenLinkOrStop() {
        mMenuBinding.openLinkOrStopBtn.setOnClickListener(v -> {
            if (exoPlayers[mOpenedMenuPlayerId] != null) {
                if (exoPlayers[mOpenedMenuPlayerId].isLinkPlayingManuallyStart()) {
                    exoPlayers[mOpenedMenuPlayerId].stopPlayLinkManually();
                } else {
                    tryPlayBookmark();
                }
            }
        });
    }

    private void tryPlayBookmark() {
        Map<String, Content> streamLinks = appController.getStreamLinks();
        if (streamLinks.isEmpty()) {
            showAddLinkDialog();
            return;
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        TextView textTitle = new TextView(this);
        textTitle.setText(getString(R.string.choose_bookmark_play));
        textTitle.setTextSize(16);
        textTitle.setGravity(Gravity.CENTER);
        textTitle.setPadding(padding, 50, padding, 50);
        textTitle.setTextColor(getColor(R.color.purple_400));
        builder.setCustomTitle(textTitle);

        List<String> infoList = new ArrayList<>();
        List<String> keyList = new ArrayList<>();

        for (Map.Entry<String, Content> entry : streamLinks.entrySet()) {
            String key = entry.getKey();
            String info = entry.getValue().getInfo();
            infoList.add(info);
            keyList.add(key);
        }
        Collections.sort(infoList);
        Collections.sort(keyList);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, infoList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.teal_text));
                textView.setTextSize(16);
                textView.setLineSpacing(0f, 1f);
                return view;
            }
        };

        builder.setAdapter(adapter, (dialog, which) -> {
            Content content = appController.getStreamLinks().get(keyList.get(which));
            if (content != null && !content.getRealLink().isEmpty()) {
                exoPlayers[mOpenedMenuPlayerId].startPlayLinkManually(content);
            }
        });

        builder.setPositiveButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        openedDialogList.add(alertDialog);
        alertDialog.show();

        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        setDialogBtnStyle(positiveButton);

        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background_border_blue);
            window.setLayout(alertDialogWidth, alertDialogHeight);
        }
    }

    private void showAddLinkDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        ConstraintLayout customTitleLayout = new ConstraintLayout(this);
        customTitleLayout.setLayoutParams(new ConstraintLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        customTitleLayout.setPadding(padding, padding, padding, padding);

        TextView textTitleAddLink = new TextView(this);
        customTitleLayout.addView(textTitleAddLink);
        textTitleAddLink.setId(View.generateViewId());
        textTitleAddLink.setText(getString(R.string.add_bookmark));
        textTitleAddLink.setTextSize(16);
        textTitleAddLink.setGravity(Gravity.CENTER);
        textTitleAddLink.setTextColor(getColor(R.color.purple_400));

        ConstraintLayout.LayoutParams titleParams = new ConstraintLayout.LayoutParams(0, WRAP_CONTENT);
        titleParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        titleParams.horizontalWeight = 1f; // чтобы тянулся
        textTitleAddLink.setLayoutParams(titleParams);

        FloatingActionButton actionButton = new FloatingActionButton(this);
        customTitleLayout.addView(actionButton);
        actionButton.setId(View.generateViewId());
        actionButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.add_notes_48px));
        actionButton.getDrawable().setTint(getColor(R.color.blue_50));
        actionButton.setRippleColor(getColor(R.color.black));
        actionButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.black));
        actionButton.setLayoutParams(new ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

        ConstraintLayout.LayoutParams btnParams = (ConstraintLayout.LayoutParams) actionButton.getLayoutParams();
        btnParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        btnParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        btnParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        actionButton.setLayoutParams(btnParams);
        actionButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestAskReadFilePermit.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                return;
            }
            requestGeneralPermitURI.launch(getIntentActionOpenDoc());
        });

        builder.setCustomTitle(customTitleLayout);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(50, 0, 50, 0);

        TextInputLayout titleInputLayout = new TextInputLayout(this);
        titleInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        titleInputLayout.setPadding(10, 0, 10, 0);
        titleInputLayout.setHint(getString(R.string.channel_name));
        ColorStateList strokeColor = ContextCompat.getColorStateList(this, R.color.cian_900);
        ColorStateList defaultColor = ContextCompat.getColorStateList(this, R.color.gray_400);
        ColorStateList errorColor = ContextCompat.getColorStateList(this, R.color.dialog_red);
        int backgroundFieldColor = ContextCompat.getColor(this, R.color.transparent);
        int textFieldColor = ContextCompat.getColor(this, R.color.teal_text);
        titleInputLayout.setErrorTextColor(errorColor);
        titleInputLayout.setDefaultHintTextColor(defaultColor);
        if (defaultColor != null) {
            titleInputLayout.setBoxStrokeColorStateList(defaultColor);
        }
        TextInputEditText titleEditText = new TextInputEditText(this);
        titleEditText.setBackgroundColor(backgroundFieldColor);
        titleEditText.setTextColor(textFieldColor);
        titleInputLayout.addView(titleEditText);

        TextInputLayout bodyInputLayout = new TextInputLayout(this);
        bodyInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        bodyInputLayout.setPadding(10, 0, 10, 0);
        bodyInputLayout.setHint(getString(R.string.link));
        bodyInputLayout.setErrorTextColor(errorColor);
        bodyInputLayout.setDefaultHintTextColor(defaultColor);
        if (defaultColor != null) {
            bodyInputLayout.setBoxStrokeColorStateList(defaultColor);
        }
        TextInputEditText bodyEditText = new TextInputEditText(this);
        bodyEditText.setBackgroundColor(backgroundFieldColor);
        bodyEditText.setTextColor(textFieldColor);
        bodyInputLayout.addView(bodyEditText);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, 10, 0, 10);
        scrollView.addView(listContainer);

        for (int i = 0; i < RTSP_EXAMPLES.length; i++) {
            TextView exampleView = new TextView(this);
            exampleView.setText(RTSP_EXAMPLES[i]);
            exampleView.setPadding(10, 5, 10, 5);
            exampleView.setTextSize(14);
            exampleView.setTextColor(strokeColor);
            if (appController.isWebViewError() && i < 2) {
                exampleView.setPaintFlags(exampleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                exampleView.setTextColor(errorColor);
            }
            listContainer.addView(exampleView);
        }
        rootLayout.addView(titleInputLayout);
        rootLayout.addView(bodyInputLayout);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                MATCH_PARENT, 0, 1);
        rootLayout.addView(scrollView, scrollParams);

        builder.setView(rootLayout);
        builder.setPositiveButton(getString(R.string.okey), null);
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        builder.setNeutralButton(getString(R.string.help), null);

        AlertDialog dialog = builder.create();
        openedDialogList.add(dialog);
        dialog.setOnShowListener(d -> {
            Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            setDialogBtnStyle(negativeButton);
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            setDialogBtnStyle(positiveButton);
            positiveButton.setOnClickListener(v -> {
                Editable editableBookmark = titleEditText.getText();
                Editable editableLink = bodyEditText.getText();
                String bookmark = editableBookmark != null ? editableBookmark.toString().trim() : EMPTY_STRING;
                String link = editableLink != null ? editableLink.toString().trim() : EMPTY_STRING;

                if (bookmark.isEmpty()) {
                    titleInputLayout.setError(getString(R.string.dialog_empty_input));
                    if (link.isEmpty()) {
                        bodyInputLayout.setError(getString(R.string.dialog_empty_input));
                    } else {
                        bodyInputLayout.setError(null);
                    }
                } else {
                    titleInputLayout.setError(null);
                    if (link.isEmpty()) {
                        bodyInputLayout.setError(getString(R.string.dialog_empty_input));
                    } else {
                        int linkType = Content.checkLinkType(link);
                        if (linkType == Content.UNSUPPORTED_LINK_TYPE
                                || (appController.isWebViewError() && linkType == Content.TWITCH_LINK_DESKTOP_TYPE)
                                || (appController.isWebViewError() && linkType == Content.TWITCH_LINK_MOBILE_TYPE)) {
                            bodyInputLayout.setError(getString(R.string.falied_link_format));
                        } else {
                            bodyInputLayout.setError(null);
                            Content content = new Content();
                            content.setTitle(bookmark);
                            content.setUserLink(link);
                            String[] realLinkChannel = Content.buildRealLink(link, linkType);
                            content.setRealLink(realLinkChannel[0]);
                            content.setChannel(realLinkChannel[1]);
                            content.setInfo(Content.buildInfo(bookmark, link));
                            content.setLinkType(linkType);
                            appController.addContent(bookmark, content);
                            dialog.dismiss();
                        }
                    }
                }
            });
        });

        dialog.show();
        Button helpButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        setDialogBtnStyle(helpButton);
        helpButton.setOnClickListener(v -> {

            MaterialAlertDialogBuilder iptvLinkBuilder = new MaterialAlertDialogBuilder(this);
            TextView textTitle = new TextView(this);
            textTitle.setText(getString(R.string.disclamer_title));
            textTitle.setTextSize(16);
            textTitle.setGravity(Gravity.CENTER);
            textTitle.setPadding(padding, 50, padding, 50);
            textTitle.setTextColor(getColor(R.color.purple_400));
            iptvLinkBuilder.setCustomTitle(textTitle);

            TextView messageText = new TextView(this);
            messageText.setText(getString(R.string.disclamer_body));
            messageText.setTextColor(getColor(R.color.blue_100));
            messageText.setPadding(50, padding, 50, padding);
            messageText.setTextSize(16);
            messageText.setMovementMethod(new ScrollingMovementMethod());

            ScrollView scrollViewIP = new ScrollView(this);
            scrollViewIP.addView(messageText);
            iptvLinkBuilder.setView(scrollViewIP);

            iptvLinkBuilder.setPositiveButton(getString(R.string.okey), (dialogInterface, i) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(IP_TV));
                startActivity(intent);
            });
            iptvLinkBuilder.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss());
            AlertDialog iptvDialog = iptvLinkBuilder.create();
            openedDialogList.add(iptvDialog);
            handler.postDelayed(() -> {
                iptvDialog.show();
                Button posBtn = iptvDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                setDialogBtnStyle(posBtn);
                Button negBtn = iptvDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                setDialogBtnStyle(negBtn);
                Window window = iptvDialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawableResource(R.drawable.dialog_background_border_blue);
                    window.setLayout(alertDialogWidth, alertDialogHeight);
                }
            }, 110);
        });
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background_border_blue);
            window.setLayout(alertDialogWidth, alertDialogHeight);
        }
    }

    private void showRemoveLinkDialog() {
        DeleteLinkDialogBinding deleteLinkDialogBinding = DeleteLinkDialogBinding.inflate(getLayoutInflater());
        SendLinkDialogBinding sendLinkDialogBinding = SendLinkDialogBinding.inflate(getLayoutInflater());
        Map<String, Content> streamLinks = appController.getStreamLinks();
        if (streamLinks.isEmpty()) {
            return;
        }

        List<String> infoList = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Content> entry : streamLinks.entrySet()) {
            String key = entry.getKey();
            String info = entry.getValue().getInfo();
            infoList.add(info);
            keys.add(key);
        }
        List<String> selectedInfos = new ArrayList<>();
        List<String> selectedKeys = new ArrayList<>();
        Collections.sort(infoList);
        Collections.sort(keys);
        deleteLinkDialogBinding.deleteLinkDialogLinearLayoutV.removeAllViews();
        for (String info : infoList) {
            CheckBox checkBoxView = new CheckBox(this);
            checkBoxView.setText(info);
            checkBoxView.setTextSize(16);
            checkBoxView.setTextColor(getColor(R.color.teal_text));
            CompoundButtonCompat.setButtonTintList(checkBoxView, mDialogCheckBoxTintList);
            checkBoxView.setPadding(32, 2, 16, 2);
            deleteLinkDialogBinding.deleteLinkDialogLinearLayoutV.addView(checkBoxView);
        }
        ViewGroup parentDel = (ViewGroup) deleteLinkDialogBinding.getRoot().getParent();
        if (parentDel != null) {
            parentDel.removeView(deleteLinkDialogBinding.getRoot());
        }
        MaterialAlertDialogBuilder deleteDialogBuilder = new MaterialAlertDialogBuilder(this);

        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());

        TextView textTitle = new TextView(this);
        textTitle.setText(getString(R.string.choose_bookmark_delete));
        textTitle.setTextSize(16);
        textTitle.setGravity(Gravity.CENTER);
        textTitle.setPadding(padding, 50, padding, 50);
        textTitle.setTextColor(getColor(R.color.purple_400));
        deleteDialogBuilder.setCustomTitle(textTitle);
        deleteDialogBuilder.setView(deleteLinkDialogBinding.getRoot());

        deleteDialogBuilder.setPositiveButton(getString(R.string.delete), null);
        deleteDialogBuilder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        deleteDialogBuilder.setNeutralButton(getString(R.string.check_all_bookmark_for_del), null);
        AlertDialog alertDialog = deleteDialogBuilder.create();
        openedDialogList.add(alertDialog);

        alertDialog.setOnShowListener(dialog -> {
            Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            setDialogBtnStyle(positiveButton);
            positiveButton.setOnClickListener(v -> {
                selectedInfos.clear();
                selectedKeys.clear();

                int size = deleteLinkDialogBinding.deleteLinkDialogLinearLayoutV.getChildCount();
                for (int i = 0; i < size; i++) {
                    CheckBox box = (CheckBox) deleteLinkDialogBinding.deleteLinkDialogLinearLayoutV.getChildAt(i);
                    if (box.isChecked()) {
                        selectedInfos.add(box.getText().toString());
                        selectedKeys.add(keys.get(i));
                    }
                }

                if (selectedInfos.isEmpty()) {
                    return;
                }

                ViewGroup parentSend = (ViewGroup) sendLinkDialogBinding.getRoot().getParent();
                if (parentSend != null) {
                    parentSend.removeView(sendLinkDialogBinding.getRoot());
                }

                sendLinkDialogBinding.sendLinkDialogLinearLayoutV.removeAllViews();
                for (String bookmark : selectedInfos) {
                    TextView textView = new TextView(this);
                    textView.setTextSize(16);
                    textView.setTextColor(getColor(R.color.teal_text));
                    textView.setText(bookmark);
                    sendLinkDialogBinding.sendLinkDialogLinearLayoutV.addView(textView);
                }

                MaterialAlertDialogBuilder sendDialogBuilder = new MaterialAlertDialogBuilder(this);
                TextView textTitleSend = new TextView(this);
                String customTitle = getString(R.string.delete) + QUESTION;
                textTitleSend.setText(customTitle);
                textTitleSend.setTextSize(16);
                textTitleSend.setGravity(Gravity.CENTER);
                textTitleSend.setPadding(padding, 50, padding, 50);
                textTitleSend.setTextColor(getColor(R.color.dialog_red));
                sendDialogBuilder.setCustomTitle(textTitleSend);
                sendDialogBuilder.setView(sendLinkDialogBinding.getRoot());

                sendDialogBuilder.setPositiveButton(getString(R.string.okey), (confirmDialog, confirmWhich) -> {
                    for (String key : selectedKeys) {
                        streamLinks.remove(key);
                    }
                    appController.writeSettingsToFile();
                    dialog.dismiss();
                });

                sendDialogBuilder.setNegativeButton(getString(R.string.cancel), null);

                AlertDialog sendAlertDialog = sendDialogBuilder.create();
                openedDialogList.add(sendAlertDialog);
                handler.postDelayed(() -> {
                    sendAlertDialog.show();
                    Button pBtn = sendAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    setDialogBtnStyle(pBtn);
                    Button nBtn = sendAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                    setDialogBtnStyle(nBtn);
                    Window window = sendAlertDialog.getWindow();
                    if (window != null) {
                        window.setBackgroundDrawableResource(R.drawable.dialog_background_border_blue);
                        window.setLayout(alertDialogWidth, alertDialogHeight);
                    }
                }, 110);
            });

            Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            setDialogBtnStyle(negativeButton);

        });

        alertDialog.show();
        Button canceButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        setDialogBtnStyle(canceButton);
        Button checkAllButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        setDialogBtnStyle(checkAllButton);
        checkAllButton.setOnClickListener(view -> {
            int checkBoxCount = deleteLinkDialogBinding.deleteLinkDialogLinearLayoutV.getChildCount();
            for (int i = 0; i < checkBoxCount; i++) {
                CheckBox checkBox = (CheckBox) deleteLinkDialogBinding.deleteLinkDialogLinearLayoutV.getChildAt(i);
                checkBox.setChecked(true);
            }
        });

        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background_border_blue);
            window.setLayout(alertDialogWidth, alertDialogHeight);
        }
    }

    private void setDialogBtnStyle(Button button) {
        button.setTextColor(getColor(R.color.purple_400));
        button.setTextSize(16);
    }


    private void drawFrameBorders(int playerId) {
        if (exoPlayers[playerId].getPlayer().isPlaying() || exoPlayers[playerId].isTwitchPlaying()) {
            if (exoPlayers[playerId].isManuallyMuted()) {
                mFrameViews[playerId].setBackground(AppCompatResources.getDrawable(this, R.drawable.frame_background_border_mute));
            } else {
                mFrameViews[playerId].setBackground(AppCompatResources.getDrawable(this, R.drawable.frame_background_border_play));
            }
        } else {
            mFrameViews[playerId].setBackground(AppCompatResources.getDrawable(this, R.drawable.frame_background_border_free));
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setExoViewsListener() {
        for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
            final int idx = i;
            mExoViews[i].post(() -> {
                View surfaceView = mExoViews[idx].getVideoSurfaceView();
                if (surfaceView instanceof TextureView textureView) {
                    ExoTouchZoomPanListener zoomPanListener = new ExoTouchZoomPanListener(textureView, activityRef, exoPlayers[idx]);
                    mExoViews[idx].setOnTouchListener(zoomPanListener);
                    mTouchListeners[idx] = zoomPanListener;
                }
            });

        }
    }

    private void setMenuSlideListeners(BottomSheetDialog bottomSheetDialog) {
        bottomSheetDialog.setOnShowListener(dialog ->
                setLightBarColor(bottomSheetDialog.getWindow(), ContextCompat.getColor(this, R.color.transparent)));
        bottomSheetDialog.setOnDismissListener(dialog -> openedDialogList.clear());
    }

    private void setFullScreenOff() {
        if (mBinding.guideVerticalLandCenter != null && mBinding.guideHorizontalLandCenter != null) {
            mBinding.guideVerticalLandCenter.setGuidelinePercent(0.5f);
            mBinding.guideHorizontalLandCenter.setGuidelinePercent(0.5f);
            for (FrameLayout frame : mFrameViews) {
                frame.setVisibility(View.VISIBLE);
            }
        }
        if (mBinding.scaleTextView != null) {
            mBinding.scaleTextView.setVisibility(View.GONE);
        }
    }


    private void setFullScreenOn(View view) {
        if (mBinding.scaleTextView != null && !appController.isHideScale()) {
            handler.postDelayed(() -> {
                if (isFullScreen) {
                    mBinding.scaleTextView.setVisibility(View.VISIBLE);
                }
            }, 1200);
        }
        if (mBinding.guideVerticalLandCenter != null && mBinding.guideHorizontalLandCenter != null) {
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(mBinding.constraintHolder);
            for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
                if (mExoViews[i] == view || mWebViews[i] == view) {
                    mFullScreenedPlayer = i;

                    if (mBinding.scaleTextView != null) {
                        mBinding.scaleTextView.setText(exoPlayers[i].getScaleText());
                    }

                    switch (mFullScreenedPlayer) {
                        case 0:
                            constraintSet.setGuidelinePercent(R.id.guideVerticalLandCenter, 1f);
                            constraintSet.setGuidelinePercent(R.id.guideHorizontalLandCenter, 1f);
                            break;
                        case 1:
                            constraintSet.setGuidelinePercent(R.id.guideVerticalLandCenter, 0f);
                            constraintSet.setGuidelinePercent(R.id.guideHorizontalLandCenter, 1f);
                            break;
                        case 2:
                            constraintSet.setGuidelinePercent(R.id.guideVerticalLandCenter, 1f);
                            constraintSet.setGuidelinePercent(R.id.guideHorizontalLandCenter, 0f);
                            break;
                        case 3:
                            constraintSet.setGuidelinePercent(R.id.guideVerticalLandCenter, 0f);
                            constraintSet.setGuidelinePercent(R.id.guideHorizontalLandCenter, 0f);
                            break;
                    }
                    animateFullScreen(constraintSet, 110);
                    updateFrames(mFullScreenedPlayer);
                    return;
                }
            }
        }
    }

    private void animateFullScreen(ConstraintSet constraintSet, long time) {
        ChangeBounds transition = new ChangeBounds();
        transition.setDuration(time);
        transition.setInterpolator(new AccelerateInterpolator());
        TransitionManager.beginDelayedTransition(mBinding.constraintHolder, transition);
        constraintSet.applyTo(mBinding.constraintHolder);
    }

    private void updateFrames(int fullScreenFrameIdx) {
        for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
            if (i == fullScreenFrameIdx) {
                mFrameViews[i].setVisibility(View.VISIBLE);
            } else {
                mFrameViews[i].setVisibility(View.GONE);
            }
        }
    }

    private int isPointInsideView(float x, float y) {
        int[] location = new int[2];
        for (int i = 0; i < AppController.PLAYERS_COUNT; i++) {
            mFrameViews[i].getLocationOnScreen(location);
            int viewX = location[0];
            int viewY = location[1];
            if (x >= viewX && x <= (viewX + mFrameViews[i].getWidth()) &&
                    y >= viewY && y <= (viewY + mFrameViews[i].getHeight())) {
                return i;
            }
        }
        return -1;
    }

    private void setLightBarColor(Window window, int color) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
        //  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        if (window != null) {
            window.setNavigationBarColor(color);
        }
        // }
    }

    private void setEdgePadding() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets gestureInsets = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures());

            int systemLeft = systemInsets.left;
            int systemRight = systemInsets.right;

            int gestureLeft = gestureInsets.left;
            int gestureRight = gestureInsets.right;

            boolean isLandscape = v.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            int paddingLeft = 0;
            int paddingRight = 0;
            int paddingBottom = Math.max(gestureInsets.bottom, systemInsets.bottom);
            if (isLandscape) {
                if (systemLeft > systemRight) {
                    paddingLeft = Math.max(gestureLeft, systemLeft);
                } else if (systemRight > systemLeft) {
                    paddingRight = Math.max(gestureRight, systemRight);
                }
            }
            v.setPadding(paddingLeft, systemInsets.top, paddingRight, paddingBottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private Intent getIntentActionOpenDoc() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/plain",           // txt
                "audio/x-mpegurl",      // m3u
                "application/vnd.apple.mpegurl" // m3u8
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return intent;
    }

    private Map<String, Content> readLinksFromUserFile(Uri uri) {
        Map<String, Content> fileLinks = new HashMap<>();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }

            for (int i = lines.size() - 1; i >= 0; i--) {
                String link = lines.get(i);
                int linkType = Content.checkLinkType(link);
                if (linkType == Content.UNSUPPORTED_LINK_TYPE
                        || (appController.isWebViewError() && linkType == Content.TWITCH_LINK_DESKTOP_TYPE)
                        || (appController.isWebViewError() && linkType == Content.TWITCH_LINK_MOBILE_TYPE)) {
                    continue; // не ссылка — пропускаем
                }
                String title = null;
                String userAgent = null;
                // ищем метаданные выше
                for (int j = i - 1; j >= 0; j--) {
                    String meta = lines.get(j);
                    if (meta.startsWith("#EXTINF:")) {
                        int commaIdx = meta.lastIndexOf(",");
                        if (commaIdx >= 0 && commaIdx < meta.length() - 1) {
                            title = meta.substring(commaIdx + 1).trim();
                        }
                        // искать user-agent в EXTINF
                        int uaIdx = meta.indexOf("http-user-agent=");
                        if (uaIdx >= 0 && userAgent == null) {
                            int start = meta.indexOf("\"", uaIdx);
                            int end = meta.indexOf("\"", start + 1);
                            if (start >= 0 && end > start) {
                                userAgent = meta.substring(start + 1, end);
                            }
                        }
                        break;
                    } else if (meta.startsWith("#EXTVLCOPT:http-user-agent=")) {
                        userAgent = meta.substring("#EXTVLCOPT:http-user-agent=".length()).trim();
                        // не выходим, идём искать название
                    } else if (Content.checkLinkType(meta) == Content.UNSUPPORTED_LINK_TYPE) {
                        title = meta.trim();
                        break;
                    } else {
                        break;
                    }
                    i = j; // поднимаем указатель, чтобы не разбирать мета 2 раза
                }

                if (title == null || title.isEmpty()) {
                    title = getString(R.string.channel_name) + SPACE + i;
                }

                Content content = new Content();
                content.setUserLink(link);
                String[] realLinkChannel = Content.buildRealLink(link, linkType);
                content.setRealLink(realLinkChannel[0]);
                content.setChannel(realLinkChannel[1]);
                content.setInfo(Content.buildInfo(title, link));
                content.setLinkType(linkType);
                if (userAgent != null) {
                    content.setUserAgent(userAgent);
                }

                String uniqueTitle = title;
                int counter = 1;
                while (fileLinks.containsKey(uniqueTitle)) {
                    uniqueTitle = title + counter++;
                }
                content.setTitle(uniqueTitle);
                fileLinks.put(uniqueTitle, content);
            }
        } catch (IOException e) {
            Log.d(AppController.LOG_TAG, "Ошибка чтения файла: " + e.getMessage(), e);
        }
        return fileLinks;
    }


    private void showDialogEmptyFile() {
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        MaterialAlertDialogBuilder iptvLinkBuilder = new MaterialAlertDialogBuilder(this);
        TextView textTitle = new TextView(this);
        textTitle.setText(getString(R.string.not_find_links_in_file));
        textTitle.setTextSize(16);
        textTitle.setGravity(Gravity.CENTER);
        textTitle.setPadding(padding, 50, padding, 50);
        textTitle.setTextColor(getColor(R.color.dialog_red));
        iptvLinkBuilder.setCustomTitle(textTitle);

        TextView messageText = new TextView(this);
        messageText.setText(HtmlCompat.fromHtml(getString(R.string.not_find_links_in_file_rules), HtmlCompat.FROM_HTML_MODE_LEGACY));
        messageText.setTextColor(getColor(R.color.blue_100));
        messageText.setPadding(50, padding, 50, padding);
        messageText.setTextSize(16);
        messageText.setMovementMethod(new ScrollingMovementMethod());

        ScrollView scrollViewIP = new ScrollView(this);
        scrollViewIP.addView(messageText);
        iptvLinkBuilder.setView(scrollViewIP);
        iptvLinkBuilder.setPositiveButton(getString(R.string.okey), (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog iptvDialog = iptvLinkBuilder.create();
        openedDialogList.add(iptvDialog);
        iptvDialog.show();
        Button posBtn = iptvDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        setDialogBtnStyle(posBtn);
        Button negBtn = iptvDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        setDialogBtnStyle(negBtn);
        Window window = iptvDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background_border_blue);
            window.setLayout(alertDialogWidth, alertDialogHeight);
        }
    }

    private void showDialogAddContentFromFile(Map<String, Content> fileLinks) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        TextView textTitle = new TextView(this);
        textTitle.setText(getString(R.string.find_links_in_file));
        textTitle.setTextSize(16);
        textTitle.setGravity(Gravity.CENTER);
        textTitle.setPadding(padding, 50, padding, 50);
        textTitle.setTextColor(getColor(R.color.purple_400));
        builder.setCustomTitle(textTitle);

        List<String> infoList = new ArrayList<>();
        for (Map.Entry<String, Content> entry : fileLinks.entrySet()) {
            infoList.add(entry.getValue().getInfo());
        }
        Collections.sort(infoList);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, infoList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.teal_text));
                textView.setTextSize(16);
                textView.setLineSpacing(0f, 1.0f);
                return view;
            }
        };

        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        builder.setView(listView);
        builder.setPositiveButton(getString(R.string.okey), (dialogInterface, i) -> {
            appController.addContents(fileLinks);
            dialogInterface.dismiss();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog alertDialog = builder.create();
        openedDialogList.add(alertDialog);
        alertDialog.show();
        Button posBtn = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        setDialogBtnStyle(posBtn);
        Button negBtn = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        setDialogBtnStyle(negBtn);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.dialog_background_border_blue);
            window.setLayout(alertDialogWidth, alertDialogHeight);
        }
    }

    private void clearDialogsAndMenu() {
        for (AlertDialog dialog : openedDialogList) {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
        mBottomSheetDialog.dismiss();
        mBottomSheetDialog = null;
    }

}