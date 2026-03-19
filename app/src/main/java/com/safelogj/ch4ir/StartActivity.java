package com.safelogj.ch4ir;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;

import androidx.activity.EdgeToEdge;
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
import com.safelogj.ch4ir.databinding.ActivityStartBinding;

@UnstableApi
public class StartActivity extends AppCompatActivity {

    private ActivityStartBinding mBinding;
    private AppController appController;


    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            EdgeToEdge.enable(this);
      //  }
        mBinding = ActivityStartBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setEdgePadding();
        setLightBarColor();
        appController = (AppController) getApplication();

        mBinding.startOkButton.setOnClickListener(view -> {
            if (appController.getPrivacyId() < PrivacyActivity.CURRENT_PRIVACY_ID) {
                startActivity(new Intent(this, PrivacyActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
        });
        mBinding.startAdsButton.setOnClickListener(view -> openYoutubeLink());
        mBinding.startPrivacyButton.setOnClickListener(view -> startActivity(new Intent(this, PrivacyActivity.class)));
        mBinding.startTextView0.setMovementMethod(LinkMovementMethod.getInstance());
        String text = appController.getInfoText();
        mBinding.startTextView0.setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBinding.skipCheckBox.setChecked(appController.isSkipStart());
        mBinding.hideScaleCheckBox.setChecked(appController.isHideScale());
        if (appController.isSkipStart() && !appController.isHasRedirectedOnce() && appController.getPrivacyId() == PrivacyActivity.CURRENT_PRIVACY_ID) {
            appController.setHasRedirectedOnce(true);
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
       setCheckBoxColor();
        mBinding.startScrollView.post(() -> mBinding.startScrollView.scrollTo(0, 0));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!appController.isHasRedirectedOnce() && mBinding.skipCheckBox.isChecked()) {
            appController.setHasRedirectedOnce(true);
        }

        appController.setSkipStart(mBinding.skipCheckBox.isChecked());
        appController.setHideScale(mBinding.hideScaleCheckBox.isChecked());
        appController.writeSettingsToFile();
    }

    private void setLightBarColor() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
               getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.main_back));
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
                android.util.Log.d(AppController.LOG_TAG, "padding left = " + systemLeft + " и правый = " + systemRight);

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

    private void setCheckBoxColor() {
        int nightModeFlags = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

        ColorStateList tintList;
        if (isDarkTheme) {
            tintList = ContextCompat.getColorStateList(this, R.color.checkbox_backgrount_selector_night);
        } else {
            tintList = ContextCompat.getColorStateList(this, R.color.checkbox_backgrount_selector);
        }
        CompoundButtonCompat.setButtonTintList(mBinding.skipCheckBox, tintList);
        CompoundButtonCompat.setButtonTintList(mBinding.hideScaleCheckBox, tintList);
    }

    private void openYoutubeLink() {
        try {
            Uri webpage = Uri.parse("https://www.youtube.com/watch?v=FNOfnhpO1aI");
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            startActivity(intent);
        } catch (Exception e) {
            //
        }
    }
}