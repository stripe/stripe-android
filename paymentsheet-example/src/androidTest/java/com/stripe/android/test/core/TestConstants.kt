package com.stripe.android.test.core

import com.stripe.android.paymentsheet.example.BuildConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val INDIVIDUAL_TEST_TIMEOUT_SECONDS = 90L
const val HOOKS_PAGE_LOAD_TIMEOUT = 60L

// The Chrome-bearing google_apis CI image is slower than local/aosp-atd, so allow more headroom
// for UI waits on CI (still well under the 90s per-test ceiling).
val DEFAULT_UI_TIMEOUT: Duration = if (BuildConfig.IS_RUNNING_IN_CI) 25.seconds else 15.seconds
