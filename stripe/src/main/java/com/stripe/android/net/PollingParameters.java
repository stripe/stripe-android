package com.stripe.android.net;

/**
 * Container class for polling parameters.
 */
class PollingParameters {

    private static final long DEFAULT_TIMEOUT_MS = 10000L;
    private static final long INITIAL_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 15000;
    private static final int MAX_RETRY_COUNT = 5;
    private static final long MAX_TIMEOUT_MS = 5L * 60L * 1000L; // five minutes
    private static final int POLLING_MULTIPLIER = 2;

    private final long mDefaultTimeoutMs;
    private final long mInitialDelayMs;
    private final long mMaxDelayMs;
    private final int mMaxRetryCount;
    private final long mMaxTimeoutMs;
    private final int mPollingMultiplier;

    PollingParameters(
            long defaultTimeoutMs,
            long initialDelayMs,
            long maxDelayMs,
            int maxRetryCount,
            long maxTimeoutMs,
            int pollingMultiplier) {
        mDefaultTimeoutMs = defaultTimeoutMs;
        mInitialDelayMs = initialDelayMs;
        mMaxDelayMs = maxDelayMs;
        mMaxRetryCount = maxRetryCount;
        mMaxTimeoutMs = maxTimeoutMs;
        mPollingMultiplier = pollingMultiplier;
    }

    long getDefaultTimeoutMs() {
        return mDefaultTimeoutMs;
    }

    long getInitialDelayMs() {
        return mInitialDelayMs;
    }

    int getInitialDelayMsInt() {
        return (int) mInitialDelayMs;
    }

    long getMaxDelayMs() {
        return mMaxDelayMs;
    }

    int getMaxRetryCount() {
        return mMaxRetryCount;
    }

    long getMaxTimeoutMs() {
        return mMaxTimeoutMs;
    }

    int getPollingMultiplier() {
        return mPollingMultiplier;
    }

    static PollingParameters generateDefaultParameters() {
        return new PollingParameters(
                DEFAULT_TIMEOUT_MS,
                INITIAL_DELAY_MS,
                MAX_DELAY_MS,
                MAX_RETRY_COUNT,
                MAX_TIMEOUT_MS,
                POLLING_MULTIPLIER);
    }
}
