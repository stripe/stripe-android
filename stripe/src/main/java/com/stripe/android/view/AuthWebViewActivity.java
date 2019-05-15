package com.stripe.android.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.stripe.android.R;

public class AuthWebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_web_view_layout);

        final String returnUrl = getIntent().getStringExtra(AuthWebViewStarter.EXTRA_RETURN_URL);

        final AuthWebView webView = findViewById(R.id.auth_web_view);
        webView.init(this, returnUrl);
        webView.loadUrl(getIntent().getStringExtra(AuthWebViewStarter.EXTRA_AUTH_URL));
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
