package com.stripe.wrap.pay.testharness;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;

import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.stripe.android.model.Source;
import com.stripe.android.model.Token;
import com.stripe.wrap.pay.activity.StripeAndroidPayActivity;

public class TestAndroidPayActivity extends StripeAndroidPayActivity {

    AndroidPayAvailabilityChooser mAndroidPayAvailabilityChooser;
    GoogleApiClientMockBuilder mGoogleApiClientMockBuilder;
    StripeAndroidPayActivityListener mListener;

    public void setStripeAppCompatActivityListener(StripeAndroidPayActivityListener listener) {
        mListener = listener;
    }

    public void setGoogleApiClientMockBuilder(GoogleApiClientMockBuilder builder) {
        mGoogleApiClientMockBuilder = builder;
    }

    public void setAndroidPayAvailabilityChooser(AndroidPayAvailabilityChooser chooser) {
        mAndroidPayAvailabilityChooser = chooser;
    }

    @NonNull
    @Override
    protected GoogleApiClient buildGoogleApiClient() {
        return mGoogleApiClientMockBuilder.getMockGoogleApiClient();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mListener != null) {
            mListener.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected int getWalletEnvironment() {
        int walletEnvironment = super.getWalletEnvironment();
        if (mListener != null) {
            mListener.getWalletEnvironment(walletEnvironment);
        }
        return walletEnvironment;
    }

    @Override
    protected int getWalletTheme() {
        int walletTheme = super.getWalletTheme();
        if (mListener  != null) {
            mListener.getWalletTheme(walletTheme);
        }
        return walletTheme;
    }

    @NonNull
    @Override
    protected WalletFragmentOptions getWalletFragmentOptions(int walletFragmentMode) {
        WalletFragmentOptions options = super.getWalletFragmentOptions(walletFragmentMode);
        if (mListener != null) {
            mListener.getWalletFragmentOptions(options);
        }
        return options;
    }

    @Override
    protected void addBuyButtonWalletFragment(@NonNull SupportWalletFragment walletFragment) {
        if (mListener != null) {
            mListener.addBuyButtonWalletFragment(walletFragment);
        }
    }

    @Override
    protected void addConfirmationWalletFragment(@NonNull SupportWalletFragment walletFragment) {
        if (mListener != null) {
            mListener.addConfirmationWalletFragment(walletFragment);
        }
    }

    @Override
    protected void onBeforeAndroidPayAvailable() {
        super.onBeforeAndroidPayAvailable();
        if (mListener != null) {
            mListener.onBeforeAndroidPayAvailable();
        }
    }

    @Override
    protected void onAfterAndroidPayCheckComplete() {
        super.onAfterAndroidPayCheckComplete();
        if (mListener != null) {
            mListener.onAfterAndroidPayCheckComplete();
        }
    }

    @Override
    protected void onAndroidPayAvailable() {
        if (mListener != null) {
            mListener.onAndroidPayAvailable();
        }
    }

    @Override
    protected void onAndroidPayNotAvailable() {
        if (mListener != null) {
            mListener.onAndroidPayNotAvailable();
        }
    }

    @Override
    protected void handleError(int errorCode) {
        if (mListener != null) {
            mListener.handleError(errorCode);
        }
    }

    @Override
    protected void loadFullWallet(@NonNull FullWalletRequest fullWalletRequest) {
        super.loadFullWallet(fullWalletRequest);
        if (mListener != null) {
            mListener.loadFullWallet(fullWalletRequest);
        }
    }

    @Override
    protected void onConfirmedMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) {
        super.onConfirmedMaskedWalletRetrieved(maskedWallet);
        if (mListener != null) {
            mListener.onConfirmedMaskedWalletRetrieved(maskedWallet);
        }
    }

    @Override
    protected void onMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) {
        if (mListener != null) {
            mListener.onMaskedWalletRetrieved(maskedWallet);
        }
    }

    @Override
    protected void onTokenReturned(FullWallet wallet, Token token) {
        super.onTokenReturned(wallet, token);
        if (mListener != null) {
            mListener.onTokenReturned(wallet, token);
        }
    }

    @Override
    protected void onSourceReturned(FullWallet wallet, Source source) {
        super.onSourceReturned(wallet, source);
        if (mListener != null) {
            mListener.onSourceReturned(wallet, source);
        }
    }

    @Override
    protected void verifyAndPrepareAndroidPayControls(
            @NonNull GoogleApiClient googleApiClient,
            @NonNull IsReadyToPayRequest isReadyToPayRequest) {
        if (mListener != null && mAndroidPayAvailabilityChooser != null) {
            mListener.verifyAndPrepareAndroidPayControls(isReadyToPayRequest);
            // Simulate the check completing
            onAfterAndroidPayCheckComplete();
            BooleanResult result = mAndroidPayAvailabilityChooser.doesAndroidPayCheckSucceed();
            if (result.getStatus().isSuccess() && result.getValue()) {
                createAndAddBuyButtonWalletFragment();
            } else {
                onAndroidPayNotAvailable();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        super.onConnectionFailed(connectionResult);
        if (mListener != null) {
            mListener.onConnectionFailed(connectionResult);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        super.onConnectionSuspended(i);
        if (mListener != null) {
            mListener.onConnectionSuspended(i);
        }
    }

    /**
     * Listener that acts as a shadow of this Activity
     */
    public interface StripeAndroidPayActivityListener {
        void addBuyButtonWalletFragment(SupportWalletFragment walletFragment);
        void addConfirmationWalletFragment(SupportWalletFragment walletFragment);
        void getWalletEnvironment(int walletEnvironment);
        void getWalletFragmentOptions(WalletFragmentOptions options);
        void getWalletTheme(int walletTheme);
        void handleError(int errorCode);
        void loadFullWallet(FullWalletRequest fullWalletRequest);
        void onActivityResult(int requestCode, int resultCode, Intent data);
        void onAfterAndroidPayCheckComplete();
        void onAndroidPayAvailable();
        void onAndroidPayNotAvailable();
        void onBeforeAndroidPayAvailable();
        void onConfirmedMaskedWalletRetrieved(MaskedWallet maskedWallet);
        void onConnectionFailed(@NonNull ConnectionResult connectionResult);
        void onConnectionSuspended(int i);
        void onMaskedWalletRetrieved(MaskedWallet maskedWallet);
        void onTokenReturned(FullWallet fullWallet, Token token);
        void onSourceReturned(FullWallet fullWallet, Source source);
        void verifyAndPrepareAndroidPayControls(@NonNull IsReadyToPayRequest payRequest);
    }

    public interface GoogleApiClientMockBuilder {
        GoogleApiClient getMockGoogleApiClient();
    }

    public interface AndroidPayAvailabilityChooser {
        BooleanResult doesAndroidPayCheckSucceed();
    }
}
