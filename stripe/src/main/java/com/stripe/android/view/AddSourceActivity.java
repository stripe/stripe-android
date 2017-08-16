package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.transition.Fade;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.StripePaymentSource;
import com.stripe.android.model.Token;

public class AddSourceActivity extends AppCompatActivity {

    public static final String EXTRA_NEW_SOURCE = "new_source";
    static final String ADD_SOURCE_ACTIVITY = "AddSourceActivity";
    static final String EXTRA_SHOW_ZIP = "show_zip";
    static final String EXTRA_UPDATE_CUSTOMER = "update_customer";
    static final long FADE_DURATION_MS = 100L;
    CardMultilineWidget mCardMultilineWidget;
    CustomerSessionProxy mCustomerSessionProxy;
    ProgressBar mProgressBar;
    TextView mErrorTextView;
    FrameLayout mErrorLayout;
    StripeProvider mStripeProvider;
    Toolbar mToolbar;

    private boolean mCommunicating;
    private boolean mUpdatesCustomer;

    /**
     * Create an {@link Intent} to start a {@link AddSourceActivity}.
     *
     * @param context the {@link Context} used to launch the activity
     * @param requirePostalField {@code true} to require a postal code field
     * @param updatesCustomer {@code true} if the activity should update using an
     * already-initialized {@link CustomerSession}, or {@code false} if it should just
     * return a source.
     * @return an {@link Intent} that can be used to start this activity
     */
    public static Intent newIntent(@NonNull Context context,
                                   boolean requirePostalField,
                                   boolean updatesCustomer) {
        Intent intent = new Intent(context, AddSourceActivity.class);
        intent.putExtra(EXTRA_SHOW_ZIP, requirePostalField);
        intent.putExtra(EXTRA_UPDATE_CUSTOMER, updatesCustomer);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_source);
        mCardMultilineWidget = findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = findViewById(R.id.add_source_progress_bar);
        mErrorTextView = findViewById(R.id.tv_add_source_error);
        mToolbar = findViewById(R.id.add_source_toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setCommunicatingProgress(false);
        boolean showZip = getIntent().getBooleanExtra(EXTRA_SHOW_ZIP, false);
        mUpdatesCustomer = getIntent().getBooleanExtra(EXTRA_UPDATE_CUSTOMER, false);
        mCardMultilineWidget.setShouldShowPostalCode(showZip);

        mErrorLayout = findViewById(R.id.add_source_error_container);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveItem = menu.findItem(R.id.action_save);
        Drawable tintedIcon = ViewUtils.getTintedIcon(
                this,
                R.drawable.ic_checkmark,
                android.R.color.primary_text_dark);
        saveItem.setIcon(tintedIcon);
        return super.onPrepareOptionsMenu(menu);
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

    @VisibleForTesting
    void setCustomerSessionProxy(CustomerSessionProxy proxy) {
        mCustomerSessionProxy = proxy;
    }

    @VisibleForTesting
    void setStripeProvider(@NonNull StripeProvider stripeProvider) {
        mStripeProvider = stripeProvider;
    }

    private void saveCardOrDisplayError() {
        mErrorTextView.setVisibility(View.GONE);
        Card card = mCardMultilineWidget.getCard();
        if (card == null) {
            // In this case, the error will be displayed on the card widget itself.
            return;
        }

        card.addLoggingToken(ADD_SOURCE_ACTIVITY);
        Stripe stripe = getStripe();
        stripe.setDefaultPublishableKey(PaymentConfiguration.getInstance().getPublishableKey());

        SourceParams sourceParams = SourceParams.createCardParams(card);
        setCommunicatingProgress(true);

        stripe.createSource(sourceParams, new SourceCallback() {
            @Override
            public void onError(Exception error) {
                setCommunicatingProgress(false);
                showError(error.getLocalizedMessage(), mStripeProvider == null);
            }

            @Override
            public void onSuccess(Source source) {
                if (mUpdatesCustomer) {
                    attachCardToCustomer(source);
                } else {
                    finishWithSource(source);
                }
            }
        });
    }

    private void attachCardToCustomer(StripePaymentSource source) {
        CustomerSession.SourceRetrievalListener listener =
                new CustomerSession.SourceRetrievalListener() {
                    @Override
                    public void onSourceRetrieved(@NonNull Source source) {
                        finishWithSource(source);
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage) {
                        String displayedError = errorMessage == null ? "" : errorMessage;
                        setCommunicatingProgress(false);
                        showError(displayedError, mStripeProvider == null);
                    }
                };

        if (mCustomerSessionProxy == null) {
            CustomerSession.getInstance().addCustomerSource(source.getId(), listener);
        } else {
            mCustomerSessionProxy.addCustomerSource(source.getId(), listener);
        }
    }

    private void finishWithSource(@NonNull Source source) {
        setCommunicatingProgress(false);
        mErrorTextView.setVisibility(View.GONE);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_NEW_SOURCE, source.toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    private Stripe getStripe() {
        if (mStripeProvider == null) {
            return new Stripe(this);
        } else {
            return mStripeProvider.getStripe(this);
        }
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

    private void showError(@NonNull String error, boolean shouldAnimate) {
        mErrorTextView.setText(error);
        if (shouldAnimate) {
            Fade fadeIn = new Fade(Fade.IN);
            fadeIn.setDuration(FADE_DURATION_MS);
            TransitionManager.beginDelayedTransition(mErrorLayout, fadeIn);
        }
        mErrorTextView.setVisibility(View.VISIBLE);
    }

    interface StripeProvider {
        Stripe getStripe(@NonNull Context context);
    }

    interface CustomerSessionProxy {
        void addCustomerSource(String sourceId, CustomerSession.SourceRetrievalListener listener);
    }
}
