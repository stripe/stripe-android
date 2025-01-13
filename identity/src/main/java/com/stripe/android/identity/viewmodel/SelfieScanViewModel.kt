package com.stripe.android.identity.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.LaplacianBlurDetector
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class SelfieScanViewModel(
    applicationContext: Application,
    override val fpsTracker: FPSTracker,
    override val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    modelPerformanceTracker: ModelPerformanceTracker,
    laplacianBlurDetector: LaplacianBlurDetector,
    private val verificationFlowFinishable: VerificationFlowFinishable
) : IdentityScanViewModel(
    applicationContext,
    fpsTracker,
    identityAnalyticsRequestFactory,
    modelPerformanceTracker,
    laplacianBlurDetector,
    verificationFlowFinishable
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val scanFeedback = scannerState.mapLatest {
        when (it) {
            is State.Scanning -> {
                when (it.scanState) {
                    is IdentityScanState.Finished -> R.string.stripe_selfie_capture_complete
                    is IdentityScanState.Found -> R.string.stripe_capturing
                    is IdentityScanState.Initial -> R.string.stripe_position_selfie
                    is IdentityScanState.Satisfied -> R.string.stripe_selfie_capture_complete
                    is IdentityScanState.TimeOut -> null
                    is IdentityScanState.Unsatisfied -> null
                    null -> R.string.stripe_selfie_capture_complete
                }
            }
            is State.Scanned -> R.string.stripe_selfie_capture_complete
            else -> null
        }
    }.distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = R.string.stripe_position_selfie
        )

    internal class SelfieScanViewModelFactory @Inject constructor(
        private val verificationFlowFinishable: VerificationFlowFinishable,
        private val modelPerformanceTracker: ModelPerformanceTracker,
        private val laplacianBlurDetector: LaplacianBlurDetector,
        private val fpsTracker: FPSTracker,
        private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return SelfieScanViewModel(
                extras.requireApplication(),
                fpsTracker,
                identityAnalyticsRequestFactory,
                modelPerformanceTracker,
                laplacianBlurDetector,
                verificationFlowFinishable
            ) as T
        }
    }
}
