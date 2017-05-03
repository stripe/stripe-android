package com.stripe.wrap.pay.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.stripe.android.model.Source;
import com.stripe.android.model.Token;
import com.stripe.android.net.TokenParser;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.utils.PaymentUtils;

import org.json.JSONException;

import java.util.Locale;

/**
 * A class that handles the Google API callbacks for the purchase flow and {@link GoogleApiClient}
 * connection states, simplifying the required work to display and complete an Android Pay purchase.
 */
public abstract class StripeAndroidPayActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = StripeAndroidPayActivity.class.getName();

    public static final String EXTRA_ACCOUNT_NAME = "extra_account_name";
    public static final String EXTRA_CART = "extra_cart";

    // Request code to use when requesting the Masked Wallet.
    public static final int REQUEST_CODE_MASKED_WALLET = 2002;

    // Request code to use when allowing the user to change before confirming the Masked Wallet.
    public static final int REQUEST_CODE_CHANGE_MASKED_WALLET = 3003;

    // Request code to use when requesting the Full Wallet.
    public static final int REQUEST_CODE_LOAD_FULL_WALLET = 4004;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    protected String mAccountName;
    protected AndroidPayConfiguration mAndroidPayConfiguration;
    protected Cart mCart;
    protected GoogleApiClient mGoogleApiClient;
    protected String mGoogleTransactionId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        if (getIntent().hasExtra(EXTRA_CART)) {
            mCart = getIntent().getParcelableExtra(EXTRA_CART);
        }

        if (getIntent().hasExtra(EXTRA_ACCOUNT_NAME)) {
            mAccountName = getIntent().getStringExtra(EXTRA_ACCOUNT_NAME);
        }

        mAndroidPayConfiguration = AndroidPayConfiguration.getInstance();
        mGoogleApiClient = buildGoogleApiClient();

        onBeforeAndroidPayAvailable();
        verifyAndPrepareAndroidPayControls(mGoogleApiClient,
                PaymentUtils.getStripeIsReadyToPayRequest());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // retrieve the error code, if available
        int errorCode = -1;
        if (data != null) {
            errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1);
        }
        switch (requestCode) {
            case REQUEST_RESOLVE_ERROR:
                mResolvingError = false;
                if (resultCode != RESULT_OK) {
                    return;
                }
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
                break;
            case REQUEST_CODE_MASKED_WALLET:
                switch (resultCode) {
                    case RESULT_OK:
                        if (data != null) {
                            MaskedWallet maskedWallet =
                                    data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                            if (maskedWallet != null) {
                                mGoogleTransactionId = maskedWallet.getGoogleTransactionId();
                            }
                            onMaskedWalletRetrieved(maskedWallet);
                        }
                        break;
                    case RESULT_CANCELED:
                        break;
                    default:
                        handleError(errorCode);
                        break;
                }
                break;
            case REQUEST_CODE_CHANGE_MASKED_WALLET:
                switch (resultCode) {
                    case RESULT_OK:
                        if (data != null) {
                            MaskedWallet maskedWallet =
                                    data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                            if (maskedWallet != null) {
                                // Update the google transaction id
                                mGoogleTransactionId = maskedWallet.getGoogleTransactionId();
                            }
                            onConfirmedMaskedWalletRetrieved(maskedWallet);
                        }
                        break;
                    case RESULT_CANCELED:
                        break;
                    default:
                        handleError(errorCode);
                        break;
                }
                break;
            case REQUEST_CODE_LOAD_FULL_WALLET:
                switch (resultCode) {
                    case RESULT_OK:
                        if (data != null && data.hasExtra(WalletConstants.EXTRA_FULL_WALLET)) {
                            FullWallet fullWallet =
                                    data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET);
                            // the full wallet can now be used to process the customer's payment
                            // send the wallet info up to server to process, and to get the result
                            // for sending a transaction status
                            onFullWalletRetrieved(fullWallet);
                        }
                        break;
                    case RESULT_CANCELED:
                        break;
                    default:
                        handleError(errorCode);
                        break;
                }
            case WalletConstants.RESULT_ERROR:
                handleError(errorCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    protected void verifyAndPrepareAndroidPayControls(
            @NonNull GoogleApiClient googleApiClient,
            @NonNull IsReadyToPayRequest isReadyToPayRequest) {
        Wallet.Payments.isReadyToPay(googleApiClient, isReadyToPayRequest)
                .setResultCallback(
                        new ResultCallback<BooleanResult>() {
                            @Override
                            public void onResult(@NonNull BooleanResult booleanResult) {
                                onAfterAndroidPayCheckComplete();
                                if (booleanResult.getStatus().isSuccess()
                                        && booleanResult.getValue()) {
                                    createAndAddBuyButtonWalletFragment();
                                } else {
                                    onAndroidPayNotAvailable();
                                }
                            }
                        });
    }

    /**
     * Call to make a final payment request to Google Play Services.
     *
     * @param fullWalletRequest
     */
    protected void loadFullWallet(@NonNull FullWalletRequest fullWalletRequest) {
        Wallet.Payments.loadFullWallet(
                mGoogleApiClient,
                fullWalletRequest,
                REQUEST_CODE_LOAD_FULL_WALLET);
    }

    /**
     * Builds the {@link GoogleApiClient} used in this Activity. Override
     * if you'd like to change the default GoogleApiClient.
     *
     * @return a {@link GoogleApiClient} used to interact with the Wallet API
     */
    @NonNull
    protected GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(getWalletEnvironment())
                        .setTheme(getWalletTheme())
                        .build())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * Creates the {@link WalletFragmentStyle} for this Activity. Override to change
     * the appearance of the Wallet Fragment itself. The results of this method
     * are used to build the {@link WalletFragmentOptions}.
     *
     * @return a {@link WalletFragmentStyle} used to display Android Pay options to the user
     */
    @NonNull
    protected WalletFragmentStyle getWalletFragmentStyle() {
        return new WalletFragmentStyle()
                .setBuyButtonText(WalletFragmentStyle.BuyButtonText.BUY_WITH)
                .setBuyButtonAppearance(WalletFragmentStyle.BuyButtonAppearance.ANDROID_PAY_DARK)
                .setBuyButtonWidth(WalletFragmentStyle.Dimension.MATCH_PARENT);
    }

    @NonNull
    protected WalletFragmentOptions getWalletFragmentOptions(int walletFragmentMode) {
        if (walletFragmentMode != WalletFragmentMode.BUY_BUTTON
        && walletFragmentMode != WalletFragmentMode.SELECTION_DETAILS) {
            throw new IllegalArgumentException(
                    String.format(Locale.ENGLISH,
                            "Using unknown WalletFragmentMode (%d) to create WalletFragment",
                            walletFragmentMode));
        }

        return WalletFragmentOptions.newBuilder()
                .setEnvironment(getWalletEnvironment())
                .setFragmentStyle(getWalletFragmentStyle())
                .setTheme(getWalletTheme())
                .setMode(walletFragmentMode)
                .build();
    }

    /**
     * Creates the confirmation wallet fragment and calls
     * {@link #addConfirmationWalletFragment(SupportWalletFragment)}. Override this method to
     * launch a new activity as a confirmation screen, or to otherwise avoid creating a
     * confirmation fragment. This method is never automatically invoked, so it can be avoided
     * without an override if desired.
     *
     * @param maskedWallet a {@link MaskedWallet} whose details the user needs to confirm
     */
    protected void createAndAddConfirmationWalletFragment(@NonNull MaskedWallet maskedWallet) {
        SupportWalletFragment supportWalletFragment = SupportWalletFragment.newInstance(
                getWalletFragmentOptions(WalletFragmentMode.SELECTION_DETAILS));

        WalletFragmentInitParams.Builder startParamsBuilder =
                WalletFragmentInitParams.newBuilder()
                        .setMaskedWallet(maskedWallet)
                        .setMaskedWalletRequestCode(REQUEST_CODE_CHANGE_MASKED_WALLET);

        if (!TextUtils.isEmpty(mAccountName)) {
            startParamsBuilder.setAccountName(mAccountName);
        }

        supportWalletFragment.initialize(startParamsBuilder.build());
        addConfirmationWalletFragment(supportWalletFragment);
    }

    /**
     * Creates the Buy Button WalletFragment and calls
     * {@link #addBuyButtonWalletFragment(SupportWalletFragment)}. Override this method to
     * avoid displaying or instantiating a Buy Button at all.
     */
    protected void createAndAddBuyButtonWalletFragment() {
        SupportWalletFragment supportWalletFragment = SupportWalletFragment.newInstance(
                getWalletFragmentOptions(WalletFragmentMode.BUY_BUTTON));

        MaskedWalletRequest maskedWalletRequest =
                mAndroidPayConfiguration.generateMaskedWalletRequest(mCart);
        WalletFragmentInitParams.Builder startParamsBuilder =
                WalletFragmentInitParams.newBuilder()
                        .setMaskedWalletRequest(maskedWalletRequest)
                        .setMaskedWalletRequestCode(REQUEST_CODE_MASKED_WALLET);

        if (!TextUtils.isEmpty(mAccountName)) {
            startParamsBuilder.setAccountName(mAccountName);
        }

        supportWalletFragment.initialize(startParamsBuilder.build());
        addBuyButtonWalletFragment(supportWalletFragment);
    }

    /**
     * Handles receipt of a {@link FullWallet} from Google Play Services. This wallet includes
     * the Stripe {@link Token}. If that token exists, this method calls through to
     * {@link #onTokenReturned(FullWallet, Token)}. If the object returned is a {@link Source},
     * this function passes the result through to {@link #onSourceReturned(FullWallet, Source)}.
     *
     * @param fullWallet the {@link FullWallet} returned from Google Play Services
     */
    protected void onFullWalletRetrieved(@Nullable FullWallet fullWallet) {
        if (fullWallet == null || fullWallet.getPaymentMethodToken() == null) {
            return;
        }

        String rawPurchaseToken = fullWallet.getPaymentMethodToken().getToken();
        if (rawPurchaseToken == null) {
            Log.w(TAG, "Null token returned with non-null full wallet");
        }

        try {
            Token token = TokenParser.parseToken(rawPurchaseToken);
            onTokenReturned(fullWallet, token);
        } catch (JSONException jsonException) {
            Log.i(TAG,
                    String.format(Locale.ENGLISH,
                            "Could not parse object as Stripe token. Trying as Source.\n%s",
                            rawPurchaseToken),
                    jsonException);
            Source source = Source.fromString(rawPurchaseToken);

            if (source == null) {
                Log.w(TAG,
                        String.format(Locale.ENGLISH,
                                "Could not parse object as Stripe Source\n%s",
                                rawPurchaseToken),
                        jsonException);
                return;
            }
            onSourceReturned(fullWallet, source);
        }
    }

    /*------ Begin GoogleApiClient.ConnectionCallbacks ------*/

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        onAndroidPayAvailable();
    }

    @Override
    public void onConnectionSuspended(int i) {
        onAndroidPayNotAvailable();
    }

    /*------ End GoogleApiClient.ConnectionCallbacks ------*/

    /*------ Begin GoogleApiClient.OnConnectionFailedListener ------*/

    /**
     * Handles the error conditions for connection issues with the {@link GoogleApiClient}.
     * Deliberately mimics the behavior of enableAutoManage.
     *
     * @param connectionResult a {@link ConnectionResult} failure in the {@link GoogleApiClient}
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    /*------ End GoogleApiClient.OnConnectionFailedListener ------*/

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /*------ Required Overrides ------*/

    protected abstract void onAndroidPayAvailable();
    protected abstract void onAndroidPayNotAvailable();

    /*------ Optional Overrides ------*/

    protected void onBeforeAndroidPayAvailable() {
        // This is a good place to display a spinner if you anticipate network delays
        // initializing the Google API client.
    }

    protected void onAfterAndroidPayCheckComplete() {
        // If a spinner was showing, remove it in this method.
    }

    /**
     * Override to handle Google errors in custom ways.
     *
     * @param errorCode the error code returned from the {@link GoogleApiClient}
     */
    protected void handleError(int errorCode) { }

    /**
     * Override this method to display the Android Pay confirmation wallet fragment. Place
     * it in a container of your choice using a {@link android.support.v4.app.FragmentTransaction}.
     *
     * @param walletFragment a {@link SupportWalletFragment} created using the
     */
    protected void addConfirmationWalletFragment(@NonNull SupportWalletFragment walletFragment) { }

    /**
     * Override this method to display the Android Pay fragment (a clickable button). Place
     * it in a container of your choice using a {@link android.support.v4.app.FragmentTransaction}.
     *
     * @param walletFragment a {@link SupportWalletFragment} created using the
     */
    protected void addBuyButtonWalletFragment(@NonNull SupportWalletFragment walletFragment) { }

    /**
     * Override this method to react to a {@link MaskedWallet} being returned from the Google API
     * when the user is asked to confirm wallet choices.
     *
     * @param maskedWallet the {@link MaskedWallet} returned from the {@link GoogleApiClient}
     */
    protected void onConfirmedMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) { }

    /**
     * Override this method to react to a {@link MaskedWallet} being returned from
     * the Google API. The masked wallet will have the user's shipping information (if
     * it was required), which you can use to update the final shipping price. You can also
     * use the card information as a confirmation for the user.
     *
     * @param maskedWallet the {@link MaskedWallet} returned from the {@link GoogleApiClient}
     */
    protected void onMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) { }

    /**
     * Override this function to move to {@link WalletConstants#ENVIRONMENT_PRODUCTION}
     *
     * @return the current wallet environment
     */
    protected int getWalletEnvironment() {
        return WalletConstants.ENVIRONMENT_TEST;
    }

    /**
     * Override this function to change the theme of Wallet display items
     *
     * @return the current wallet theme
     */
    protected int getWalletTheme() {
        return WalletConstants.THEME_LIGHT;
    }

    /**
     * Called when a Stripe {@link Source} is returned from Google's servers.
     * Send the token in this method to your server to make a charge.
     *
     * Note: this feature is not yet implemented, but to be future-proof, a returned
     * {@link Source} should still be handled.
     *
     * @param wallet the final {@link FullWallet} object
     * @param source a Stripe {@link Source} that can be used to make a charge
     */
    protected void onSourceReturned(FullWallet wallet, Source source) { }

    /**
     * Called when a Stripe {@link Token} is returned from Google's servers.
     * Send the token in this method to your server to make a charge.
     *
     * @param wallet the final {@link FullWallet} object
     * @param token a Stripe {@link Token} that can be used to make a charge
     */
    protected void onTokenReturned(FullWallet wallet, Token token) { }

    /*------ End Overrides ------*/

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((StripeAndroidPayActivity) getActivity()).onDialogDismissed();
        }
    }
}
