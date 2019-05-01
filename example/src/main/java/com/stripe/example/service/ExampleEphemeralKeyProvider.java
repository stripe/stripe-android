package com.stripe.example.service;

import android.support.annotation.NonNull;
import android.support.annotation.Size;

import com.stripe.android.EphemeralKeyProvider;
import com.stripe.android.EphemeralKeyUpdateListener;
import com.stripe.example.module.RetrofitFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * An implementation of {@link EphemeralKeyProvider} that can be used to generate
 * ephemeral keys on the backend.
 */
public class ExampleEphemeralKeyProvider implements EphemeralKeyProvider {

    @NonNull private final CompositeSubscription mCompositeSubscription;
    @NonNull private final StripeService mStripeService;
    @NonNull private final ProgressListener mProgressListener;

    public ExampleEphemeralKeyProvider(@NonNull ProgressListener progressListener) {
        mStripeService = RetrofitFactory.getInstance().create(StripeService.class);
        mCompositeSubscription = new CompositeSubscription();
        mProgressListener = progressListener;
    }

    @Override
    public void createEphemeralKey(@NonNull @Size(min = 4) String apiVersion,
                                   @NonNull final EphemeralKeyUpdateListener keyUpdateListener) {
        Map<String, String> apiParamMap = new HashMap<>();
        apiParamMap.put("api_version", apiVersion);

        mCompositeSubscription.add(mStripeService.createEphemeralKey(apiParamMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            try {
                                String rawKey = response.string();
                                keyUpdateListener.onKeyUpdate(rawKey);
                                mProgressListener.onStringResponse(rawKey);
                            } catch (IOException ignored) {
                            }
                        },
                        throwable ->
                                mProgressListener.onStringResponse(throwable.getMessage())));
    }

    public interface ProgressListener {
        void onStringResponse(String string);
    }
}
