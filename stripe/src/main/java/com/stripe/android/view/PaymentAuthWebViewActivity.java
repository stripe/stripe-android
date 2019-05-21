package com.stripe.android.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.stripe.android.R;

public class PaymentAuthWebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_auth_web_view_layout);

        final Toolbar toolbar = findViewById(R.id.payment_auth_web_view_toolbar);
        setSupportActionBar(toolbar);

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
}
