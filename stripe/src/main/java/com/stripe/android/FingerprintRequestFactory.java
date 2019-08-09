package com.stripe.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

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
        return new FingerprintRequest(
                mTelemetryClientUtil.createTelemetryMap(),
                mTelemetryClientUtil.getHashedId()
        );
    }
}
