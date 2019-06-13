package com.stripe.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.util.Map;

final class FingerprintRequestFactory implements Factory0<FingerprintRequest> {

    @NonNull private final TelemetryClientUtil mTelemetryClientUtil;

    FingerprintRequestFactory(@NonNull Context context) {
        this(new TelemetryClientUtil(context));
    }

    @VisibleForTesting
    FingerprintRequestFactory(@NonNull TelemetryClientUtil telemetryClientUtil) {
        mTelemetryClientUtil = telemetryClientUtil;
    }

    @NonNull
    @Override
    public FingerprintRequest create() {
        final Map<String, Object> params = mTelemetryClientUtil.createTelemetryMap();
        StripeNetworkUtils.removeNullAndEmptyParams(params);
        return new FingerprintRequest(params, mTelemetryClientUtil.getHashedId());
    }
}
