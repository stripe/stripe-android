package com.stripe.android.view;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.stripe.android.PaymentAuthWebViewStarter;
import com.stripe.android.R;
import com.stripe.android.StripeTextUtils;
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization;
import com.stripe.android.stripe3ds2.utils.CustomizeUtils;

public class PaymentAuthWebViewActivity extends AppCompatActivity {

    @Nullable private ToolbarCustomization mToolbarCustomization;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_auth_web_view_layout);

        final Toolbar toolbar = findViewById(R.id.payment_auth_web_view_toolbar);
        setSupportActionBar(toolbar);
        mToolbarCustomization =
                getIntent().getParcelableExtra(PaymentAuthWebViewStarter.EXTRA_UI_CUSTOMIZATION);
        customizeToolbar(toolbar);

        final String returnUrl = getIntent()
                .getStringExtra(PaymentAuthWebViewStarter.EXTRA_RETURN_URL);

        final PaymentAuthWebView webView = findViewById(R.id.auth_web_view);
        webView.init(this, returnUrl);
        webView.loadUrl(getIntent().getStringExtra(PaymentAuthWebViewStarter.EXTRA_AUTH_URL));
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
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
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void customizeToolbar(@NonNull Toolbar toolbar) {
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
