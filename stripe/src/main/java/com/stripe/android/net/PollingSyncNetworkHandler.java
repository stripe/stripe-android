package com.stripe.android.net;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;

/**
 * A synchronous polling manager that does not manage which thread
 * it is run on.
 *
 * @deprecated Polling Stripe sources is deprecated, and not guaranteed to be supported beyond
 * 4.X.X library updates.
 */
@Deprecated
class PollingSyncNetworkHandler {

    @NonNull private final String mSourceId;
    @NonNull private final String mClientSecret;
    @NonNull private final String mPublishableKey;
    @NonNull private final PollingParameters mPollingParameters;
    @NonNull private SourceRetriever mSourceRetriever;
    @NonNull private TimeRetriever mTimeRetriever;

    private long mTimeOutMs;

    PollingSyncNetworkHandler(
            @NonNull final String sourceId,
            @NonNull final String clientSecret,
            @NonNull final String publishableKey,
            @Nullable Integer timeOutMs,
            @Nullable SourceRetriever sourceRetriever,
            @Nullable TimeRetriever timeRetriever,
            @NonNull final PollingParameters pollingParameters) {
        mSourceId = sourceId;
        mClientSecret = clientSecret;
        mPublishableKey = publishableKey;
        mPollingParameters = pollingParameters;
        mTimeOutMs = timeOutMs == null ?
                mPollingParameters.getDefaultTimeoutMs()
                : Math.min(timeOutMs.longValue(), mPollingParameters.getMaxTimeoutMs());

        mSourceRetriever = sourceRetriever == null ? generateSourceRetriever() : sourceRetriever;
        mTimeRetriever = timeRetriever == null ? generateTimeRetriever() : timeRetriever;
    }

    @NonNull
    PollingResponse pollForSourceUpdate() {
        Source source = null;
        int errorCount = 0;
        long delayMs = mPollingParameters.getInitialDelayMs();
        long startTime = mTimeRetriever.getCurrentTimeInMillis();
        do {
            long timeWaited = mTimeRetriever.getCurrentTimeInMillis() - startTime;
            if (timeWaited > mTimeOutMs) {
                break;
            }

            try {
                source = mSourceRetriever.retrieveSource(
                        mSourceId,
                        mClientSecret,
                        mPublishableKey);
                delayMs = mPollingParameters.getInitialDelayMs();
                errorCount = 0;
            } catch (StripeException stripeEx) {
                if (++errorCount >= mPollingParameters.getMaxRetryCount()) {
                    return new PollingResponse(source, stripeEx);
                }
                delayMs =
                        Math.min(
                                delayMs * mPollingParameters.getPollingMultiplier(),
                                mPollingParameters.getMaxDelayMs());
            }

            if (source != null) {
                switch (source.getStatus()) {
                    case Source.CHARGEABLE:
                        return new PollingResponse(source, true, false);
                    case Source.CONSUMED:
                        return new PollingResponse(source, true, false);
                    case Source.CANCELED:
                        return new PollingResponse(source, false, false);
                    case Source.FAILED:
                        return new PollingResponse(source, false, false);
                    default:
                        // Then the source is still PENDING
                        break;
                }
            }

            try {
                synchronized (this) {
                    wait(delayMs);
                }
            } catch (InterruptedException ignored) {
                // This will decrease our wait time, but our timeout and
                // max retries will be unaffected.
            }
        } while (source == null || Source.PENDING.equals(source.getStatus()));

        return new PollingResponse(source, false, true);
    }

    @VisibleForTesting
    long getTimeOutMs() {
        return mTimeOutMs;
    }

    interface TimeRetriever {
        long getCurrentTimeInMillis();
    }

    @NonNull
    private static SourceRetriever generateSourceRetriever() {
        return new SourceRetriever() {
            @Override
            public Source retrieveSource(
                    @NonNull String sourceId,
                    @NonNull String clientSecret,
                    @NonNull String publishableKey) throws StripeException {
                return StripeApiHandler.retrieveSource(sourceId, clientSecret, publishableKey);
            }
        };
    }

    @NonNull
    private static TimeRetriever generateTimeRetriever() {
        return new TimeRetriever() {
            @Override
            public long getCurrentTimeInMillis() {
                return SystemClock.uptimeMillis();
            }
        };
    }
}
