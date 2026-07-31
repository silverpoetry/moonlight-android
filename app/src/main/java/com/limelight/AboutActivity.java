package com.limelight;

import android.graphics.Outline;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import com.limelight.utils.HelpLauncher;

/** Displays product identity and durable project resources. */
public final class AboutActivity extends BaseActivity {
    private static final String PROJECT_URL =
            "https://github.com/silverpoetry/moonlight-android";
    private static final String DOCUMENTATION_URL =
            "https://github.com/moonlight-stream/moonlight-docs/wiki/Setup-Guide";
    private static final String LICENSE_URL = PROJECT_URL + "/blob/master/LICENSE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.iv_back).setOnClickListener(
                view -> finish());
        findViewById(R.id.about_project).setOnClickListener(
                view -> HelpLauncher.launchUrl(this, PROJECT_URL));
        findViewById(R.id.about_documentation).setOnClickListener(
                view -> HelpLauncher.launchUrl(
                        this,
                        DOCUMENTATION_URL));
        findViewById(R.id.about_license).setOnClickListener(
                view -> HelpLauncher.launchUrl(this, LICENSE_URL));

        TextView version = findViewById(R.id.tv_version);
        version.setText(getString(
                R.string.about_version_format,
                BuildConfig.VERSION_NAME));

        ImageView logo = findViewById(R.id.iv_logo);
        logo.setClipToOutline(true);
        logo.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(
                        0,
                        0,
                        view.getWidth(),
                        view.getHeight(),
                        30f);
            }
        });
    }
}
