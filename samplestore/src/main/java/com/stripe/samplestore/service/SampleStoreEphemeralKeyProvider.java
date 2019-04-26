package com.stripe.samplestore.service;

import android.support.annotation.NonNull;
import android.support.annotation.Size;

import com.stripe.android.EphemeralKeyProvider;
import com.stripe.android.EphemeralKeyUpdateListener;
import com.stripe.samplestore.RetrofitFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

public class SampleStoreEphemeralKeyProvider implements EphemeralKeyProvider {
    @NonNull private final CompositeDisposable mCompositeDisposable;
    @NonNull private final StripeService mStripeService;
    @NonNull private final ProgressListener mProgressListener;

    public SampleStoreEphemeralKeyProvider(@NonNull ProgressListener progressListener) {
        final Retrofit retrofit = RetrofitFactory.getInstance();
        mStripeService = retrofit.create(StripeService.class);
        mCompositeDisposable = new CompositeDisposable();
        mProgressListener = progressListener;
    }

    @Override
    public void createEphemeralKey(@NonNull @Size(min = 4) String apiVersion,
                                   @NonNull final EphemeralKeyUpdateListener keyUpdateListener) {
        final Map<String, String> apiParamMap = new HashMap<>();
        apiParamMap.put("api_version", apiVersion);

        mCompositeDisposable.add(
                mStripeService.createEphemeralKey(apiParamMap)
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
                                throwable -> mProgressListener
                                        .onStringResponse(throwable.getMessage())));
    }

    public void destroy() {
        mCompositeDisposable.dispose();
    }

    public interface ProgressListener {
        void onStringResponse(@NonNull String string);
    }
}
