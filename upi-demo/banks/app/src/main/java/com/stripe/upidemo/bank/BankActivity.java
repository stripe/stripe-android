package com.stripe.upidemo.bank;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** A fake payment app. Only handles stripe-upi-demo://pay; never handles real UPI URIs. */
public final class BankActivity extends Activity {
    private TextView message;
    private String transactionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        render(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        render(intent);
    }

    private void render(Intent intent) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView title = text(getString(R.string.app_name), 28);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setTextColor(getColor(R.color.bank_accent));
        content.addView(title);
        content.addView(text("Fake bank app — no money moves", 16));

        message = text("", 15);
        Uri uri = intent.getData();
        boolean isPayment = uri != null
                && "stripe-upi-demo".equals(uri.getScheme())
                && "pay".equals(uri.getHost());
        transactionId = isPayment ? uri.getQueryParameter("tr") : null;

        if (transactionId == null || transactionId.isEmpty()) {
            content.addView(text("Open UPI Demo Checkout and start a fake payment to select this app.", 18));
        } else {
            content.addView(text(uri.getQueryParameter("pn") + "\n"
                    + uri.getQueryParameter("cu") + " " + uri.getQueryParameter("am"), 24));
            TextView details = text(uri.toString(), 13);
            details.setTextIsSelectable(true);
            content.addView(details);

            button(content, "Approve and return", () -> {
                if (updateBackend("approve")) returnResponse("SUCCESS");
            });
            button(content, "Decline and return", () -> {
                if (updateBackend("decline")) returnResponse("FAILURE");
            });
            button(content, "Cancel / return no result", () -> {
                setResult(RESULT_CANCELED);
                finish();
            });
            button(content, "Return SUCCESS without approving", () -> returnResponse("SUCCESS"));
            button(content, "Approve, stay here", () -> {
                if (updateBackend("approve")) {
                    message.setText("Approved in the fake backend. Stay here as long as you like, then use "
                            + "Android Back to return with no success result. You can also background "
                            + "and restore checkout to inspect the device's task behavior.");
                }
            });
            content.addView(text("Approval becomes succeeded after 3 seconds. Cancel and fake SUCCESS "
                    + "leave the fake payment pending, so verification can run for up to 15 seconds.", 14));
        }
        content.addView(message);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll);
    }

    private boolean updateBackend(String decision) {
        try {
            Uri stateUri = new Uri.Builder()
                    .scheme("content")
                    .authority("com.stripe.android.upidemo.state")
                    .appendPath("payments")
                    .appendPath(transactionId)
                    .build();
            ContentValues values = new ContentValues();
            values.put("decision", decision);
            int updated = getContentResolver().update(stateUri, values, null, null);
            if (updated != 1) {
                message.setText("This fake payment is no longer active. Start a new payment in UPI Demo Checkout.");
                return false;
            }
            return true;
        } catch (RuntimeException error) {
            message.setText("Cannot update the demo backend. Install UPI Demo Checkout first.\n" + error);
            return false;
        }
    }

    private void returnResponse(String status) {
        Intent result = new Intent().putExtra("response",
                "txnId=" + transactionId + "&Status=" + status + "&responseCode=00");
        setResult(RESULT_OK, result);
        finish();
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setPadding(0, dp(10), 0, dp(10));
        return view;
    }

    private void button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(view -> action.run());
        parent.addView(button);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
