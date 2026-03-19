package com.safelogj.ch4ir;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.media3.common.util.UnstableApi;

import com.safelogj.ch4ir.databinding.ActivityPrivacyBinding;

@UnstableApi
public class PrivacyActivity extends AppCompatActivity {

    public static final int CURRENT_PRIVACY_ID = 2;
    private static final String CHECK_BOX_KEY = "checkBoxKey";
    private ActivityPrivacyBinding mBinding;

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mBinding = ActivityPrivacyBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setEdgePadding();
        setLightBarColor();
        AppController appController = (AppController) getApplication();
        appController.setHasRedirectedOnce(true);
        boolean privacyNew = appController.getPrivacyId() < CURRENT_PRIVACY_ID;
        mBinding.privacyAllowButton.setOnClickListener(view -> {
            if (!privacyNew) {
                finish();
            } else if (mBinding.privacyCheckBox.isChecked()) {
                appController.setPrivacyId(CURRENT_PRIVACY_ID);
                appController.writeSettingsToFile();
                finish();
            } else {
                setCheckBoxColorRed(checkNightMode());
            }
        });

        if (!privacyNew) {
            mBinding.privacyCheckBox.setVisibility(View.GONE);
        }

        mBinding.privacyTextView0.setMovementMethod(LinkMovementMethod.getInstance());
        String text = appController.getPrivacyText();
        mBinding.privacyTextView0.setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setCheckBoxColor(checkNightMode());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CHECK_BOX_KEY, mBinding.privacyCheckBox.isChecked());

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        boolean checked = savedInstanceState.getBoolean(CHECK_BOX_KEY);
        mBinding.privacyCheckBox.setChecked(checked);
    }

    private void setLightBarColor() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
       // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.main_back));
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

    private void setCheckBoxColor(boolean isNight) {
        ColorStateList tintList;
        if (isNight) {
            tintList = ContextCompat.getColorStateList(this, R.color.checkbox_backgrount_selector_black_night);
        } else {
            tintList = ContextCompat.getColorStateList(this, R.color.checkbox_backgrount_selector_black);
        }
        CompoundButtonCompat.setButtonTintList(mBinding.privacyCheckBox, tintList);
    }

    private void setCheckBoxColorRed(boolean isNight) {
        ColorStateList tintList;
        if (isNight) {
            tintList = ContextCompat.getColorStateList(this, R.color.checkbox_backgrount_selector_red_night);
        } else {
            tintList = ContextCompat.getColorStateList(this, R.color.checkbox_backgrount_selector_red);
        }
        CompoundButtonCompat.setButtonTintList(mBinding.privacyCheckBox, tintList);
    }

    private boolean checkNightMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return  (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
    }
}