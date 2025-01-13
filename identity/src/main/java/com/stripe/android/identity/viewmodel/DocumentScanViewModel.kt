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
import com.stripe.android.identity.states.IdentityScanState.Companion.isNullOrFront
import com.stripe.android.identity.states.LaplacianBlurDetector
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class DocumentScanViewModel(
    applicationContext: Application,
    override val fpsTracker: FPSTracker,
    override val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    modelPerformanceTracker: ModelPerformanceTracker,
    laplacianBlurDetector: LaplacianBlurDetector,
    verificationFlowFinishable: VerificationFlowFinishable
) : IdentityScanViewModel(
    applicationContext,
    fpsTracker,
    identityAnalyticsRequestFactory,
    modelPerformanceTracker,
    laplacianBlurDetector,
    verificationFlowFinishable
) {

    @OptIn(FlowPreview::class)
    override val scanFeedback = combine(
        scannerState,
        targetScanTypeFlow
    ) { scannerState, targetScanType ->
        when (scannerState) {
            State.Initializing -> {
                if (targetScanType.isNullOrFront()) {
                    R.string.stripe_position_id_front
                } else {
                    R.string.stripe_position_id_back
                }
            }
            is State.Scanned -> R.string.stripe_scanned
            is State.Scanning -> {
                when (scannerState.scanState) {
                    is IdentityScanState.Finished -> R.string.stripe_scanned
                    is IdentityScanState.Found -> {
                        scannerState.scanState.feedbackRes ?: R.string.stripe_hold_still
                    }

                    is IdentityScanState.Initial -> idleFeedback(targetScanType)
                    is IdentityScanState.Satisfied -> R.string.stripe_scanned
                    is IdentityScanState.TimeOut -> idleFeedback(targetScanType)
                    is IdentityScanState.Unsatisfied -> idleFeedback(targetScanType)
                    null -> idleFeedback(targetScanType) // just initialized or start scanning, no scanState yet
                }
            }

            is State.Timeout -> {
                idleFeedback(targetScanType)
            }
        }
    }.distinctUntilChanged()
        .debounce { value ->
            if (value == R.string.stripe_scanned) 0 else 300
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = idleFeedback()
        )

    private fun idleFeedback(targetScanType: IdentityScanState.ScanType? = null) =
        if (targetScanType.isNullOrFront()) {
            R.string.stripe_position_id_front
        } else {
            R.string.stripe_position_id_back
        }

    internal class DocumentScanViewModelFactory @Inject constructor(
        private val verificationFlowFinishable: VerificationFlowFinishable,
        private val modelPerformanceTracker: ModelPerformanceTracker,
        private val laplacianBlurDetector: LaplacianBlurDetector,
        private val fpsTracker: FPSTracker,
        private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return DocumentScanViewModel(
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
