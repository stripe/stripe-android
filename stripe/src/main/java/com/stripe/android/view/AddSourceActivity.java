package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.transition.Fade;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;

public class AddSourceActivity extends AppCompatActivity {

    public static final String EXTRA_NEW_SOURCE = "new_source";
    static final String ADD_SOURCE_ACTIVITY = "AddSourceActivity";
    static final String EXTRA_SHOW_ZIP = "show_zip";
    static final long FADE_DURATION_MS = 100L;
    CardMultilineWidget mCardMultilineWidget;
    ProgressBar mProgressBar;
    TextView mErrorTextView;
    FrameLayout mErrorLayout;

    private boolean mCommunicating;

    public static Intent newIntent(@NonNull Context context, boolean requireZipField) {
        Intent intent = new Intent(context, AddSourceActivity.class);
        intent.putExtra(EXTRA_SHOW_ZIP, requireZipField);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_source);
        mCardMultilineWidget = findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = findViewById(R.id.add_source_progress_bar);
        mErrorTextView = findViewById(R.id.tv_add_source_error);
        Toolbar toolbar = findViewById(R.id.add_source_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setCommunicatingProgress(false);
        boolean showZip = getIntent().getBooleanExtra(EXTRA_SHOW_ZIP, false);
        mCardMultilineWidget.setShouldShowPostalCode(showZip);

        mErrorLayout = findViewById(R.id.add_source_error_container);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_source_menu, menu);
        menu.findItem(R.id.action_save).setEnabled(!mCommunicating);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveCardOrDisplayError();
            return true;
        } else {
            boolean handled = super.onOptionsItemSelected(item);
            if (!handled) {
                onBackPressed();
            }
            return handled;
        }
    }

    private void saveCardOrDisplayError() {
        mErrorTextView.setVisibility(View.GONE);
        Card card = mCardMultilineWidget.getCard();
        if (card == null) {
            // In this case, the error will be displayed on the card widget itself.
            return;
        }

        card.addLoggingToken(ADD_SOURCE_ACTIVITY);
        Stripe stripe = new Stripe(this);
        stripe.setDefaultPublishableKey(PaymentConfiguration.getInstance().getPublishableKey());

        SourceParams sourceParams = SourceParams.createCardParams(card);
        setCommunicatingProgress(true);

        stripe.createSource(sourceParams, new SourceCallback() {
            @Override
            public void onError(Exception error) {
                setCommunicatingProgress(false);
                showError(error.getLocalizedMessage());
            }

            @Override
            public void onSuccess(Source source) {
                setCommunicatingProgress(false);
                mErrorTextView.setVisibility(View.GONE);
                Intent intent = new Intent();
                intent.putExtra(EXTRA_NEW_SOURCE, source.toString());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void setCommunicatingProgress(boolean communicating) {
        mCommunicating = communicating;
        if (communicating) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.GONE);
        }
        supportInvalidateOptionsMenu();
    }

    private void showError(@NonNull String error) {
        Fade fadeIn = new Fade(Fade.IN);
        fadeIn.setDuration(FADE_DURATION_MS);
        mErrorTextView.setText(error);
        TransitionManager.beginDelayedTransition(mErrorLayout, fadeIn);
        mErrorTextView.setVisibility(View.VISIBLE);
    }
}
