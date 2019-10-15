package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import com.stripe.android.PaymentAuthWebViewStarter;
import com.stripe.android.R;
import com.stripe.android.StripeTextUtils;
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization;
import com.stripe.android.stripe3ds2.utils.CustomizeUtils;

import static com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION;

public class PaymentAuthWebViewActivity
        extends AppCompatActivity {

    @Nullable private ToolbarCustomization mToolbarCustomization;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(PaymentAuthWebView.TAG, "PaymentAuthWebViewActivity#onCreate");

        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent().setAction(UL_HANDLE_CHALLENGE_ACTION));

        setContentView(R.layout.payment_auth_web_view_layout);

        final Toolbar toolbar = findViewById(R.id.payment_auth_web_view_toolbar);
        setSupportActionBar(toolbar);
        mToolbarCustomization =
                getIntent().getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_UI_CUSTOMIZATION);
        customizeToolbar(toolbar);

        final String clientSecret = getIntent()
                .getStringExtra(PaymentAuthWebViewStarter.EXTRA_CLIENT_SECRET);

        Log.d(PaymentAuthWebView.TAG,
                "PaymentAuthWebViewActivity#onCreate - clientSecret: " + clientSecret);

        final String returnUrl = getIntent()
                .getStringExtra(PaymentAuthWebViewStarter.EXTRA_RETURN_URL);

        setResult(Activity.RESULT_OK, new Intent()
                .putExtra(StripeIntentResultExtras.CLIENT_SECRET, clientSecret));

        final PaymentAuthWebView webView = findViewById(R.id.auth_web_view);
        final ProgressBar progressBar = findViewById(R.id.auth_web_view_progress_bar);
        webView.init(this, progressBar, clientSecret, returnUrl);

        final String authUrl = getIntent().getStringExtra(PaymentAuthWebViewStarter.EXTRA_AUTH_URL);
        Log.d(PaymentAuthWebView.TAG, "Calling PaymentAuthWebView#init: " + authUrl);
        webView.loadUrl(authUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.payment_auth_web_view_menu, menu);

        if (mToolbarCustomization != null &&
                !StripeTextUtils.isBlank(mToolbarCustomization.getButtonText())) {
            final MenuItem closeMenuItem = menu.findItem(R.id.action_close);
            closeMenuItem.setTitle(mToolbarCustomization.getButtonText());
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_close) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void customizeToolbar(@NonNull Toolbar toolbar) {
        if (mToolbarCustomization != null) {
            if (!StripeTextUtils.isBlank(mToolbarCustomization.getHeaderText())) {
                toolbar.setTitle(CustomizeUtils.buildStyledText(this,
                        mToolbarCustomization.getHeaderText(), mToolbarCustomization));
            }

            if (mToolbarCustomization.getBackgroundColor() != null) {
                @ColorInt final int backgroundColor =
                        Color.parseColor(mToolbarCustomization.getBackgroundColor());
                toolbar.setBackgroundColor(backgroundColor);

                CustomizeUtils.setStatusBarColor(this, backgroundColor);
            }
        }
    }
}
