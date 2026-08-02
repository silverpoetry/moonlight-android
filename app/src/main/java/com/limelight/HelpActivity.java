package com.limelight;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.ComponentActivity;

import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;

public class HelpActivity extends ComponentActivity {

    private SpinnerDialog loadingDialog;
    private WebView webView;

    private BackNavigationRegistration backNavigationRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);
        UiHelper.notifyNewRootView(this);

        // These allow the user to zoom the page
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // This sets the view to display the whole page by default
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (loadingDialog == null) {
                    loadingDialog = SpinnerDialog.displayDialog(HelpActivity.this,
                            getResources().getString(R.string.help_loading_title),
                            getResources().getString(R.string.help_loading_msg), false);
                }

                refreshBackDispatchState();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (loadingDialog != null) {
                    loadingDialog.dismiss();
                    loadingDialog = null;
                }

                refreshBackDispatchState();
            }
        });

        webView.loadUrl(getIntent().getData().toString());
    }

    private void refreshBackDispatchState() {
        if (webView.canGoBack() && backNavigationRegistration == null) {
            backNavigationRegistration =
                    BackNavigationRegistration.register(this, this::navigateWebViewBack);
        }
        else if (!webView.canGoBack() && backNavigationRegistration != null) {
            backNavigationRegistration.unregister();
            backNavigationRegistration = null;
        }
    }

    private void navigateWebViewBack() {
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    @Override
    protected void onDestroy() {
        if (backNavigationRegistration != null) {
            backNavigationRegistration.unregister();
            backNavigationRegistration = null;
        }

        super.onDestroy();
    }

}
