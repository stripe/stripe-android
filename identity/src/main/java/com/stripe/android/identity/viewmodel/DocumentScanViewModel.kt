package com.stripe.android.identity.viewmodel

import android.app.Application
import android.provider.ContactsContract.CommonDataKinds.Identity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
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
import android.util.Log

internal class DocumentScanViewModel(
    applicationContext: Application,
    override val fpsTracker: FPSTracker,
    override val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    modelPerformanceTracker: ModelPerformanceTracker,
    laplacianBlurDetector: LaplacianBlurDetector,
    verificationFlowFinishable: VerificationFlowFinishable,
    private val identityViewModel: IdentityViewModel
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
        targetScanTypeFlow,
        identityViewModel.verificationPage.asFlow()
    ) { scannerState, targetScanType, verificationPage ->
        when (scannerState) {
            State.Initializing -> {
                val allowlist = verificationPage.data?.documentSelect?.idDocumentTypeAllowlist?.keys?.toList()
                Log.d("TAG", "allowlist: $allowlist")

                if (allowlist?.size == 1) {
                    when (allowlist[0]) {
                        "passport" -> R.string.stripe_position_passport
                        "driving_license" -> {
                            if (targetScanType.isNullOrFront()) {
                                R.string.stripe_position_dl_front
                            } else {
                                R.string.stripe_position_dl_back
                            }
                        }
                        else -> {
                            if (targetScanType.isNullOrFront()) {
                                R.string.stripe_position_id_front
                            } else {
                                R.string.stripe_position_id_back
                            }
                        }
                    }
                } else {
                    if (targetScanType.isNullOrFront()) {
                        R.string.stripe_position_id_front
                    } else {
                        R.string.stripe_position_id_back
                    }
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
                    null -> idleFeedback(targetScanType)
                }
            }

            is State.Timeout -> idleFeedback(targetScanType)
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

    fun getDocumentPositionStringRes(targetScanType: IdentityScanState.ScanType? = null): Int {
        val allowlist = identityViewModel.verificationPage.value
            ?.data
            ?.documentSelect
            ?.idDocumentTypeAllowlist
            ?.keys
            ?.toList()

        val idType = allowlist?.firstOrNull() ?: "id_document"
        val isFront = targetScanType.isNullOrFront()

        return when (idType) {
            "passport" -> if (isFront) {
                R.string.stripe_front_of_passport
            } else {
                R.string.stripe_back_of_passport
            }
            "driving_license" -> if (isFront) {
                R.string.stripe_front_of_dl
            } else {
                R.string.stripe_back_of_dl
            }
            else -> if (isFront) {
                R.string.stripe_front_of_id_document
            } else {
                R.string.stripe_back_of_id_document
            }
        }
    }

    private fun idleFeedback(targetScanType: IdentityScanState.ScanType? = null): Int {
        val allowlist = identityViewModel.verificationPage.value
            ?.data
            ?.documentSelect
            ?.idDocumentTypeAllowlist
            ?.keys
            ?.toList()

        if (allowlist?.size == 1) {
            return when (allowlist[0]) {
                "passport" -> R.string.stripe_position_passport
                "driving_license" -> {
                    if (targetScanType.isNullOrFront()) {
                        R.string.stripe_position_dl_front
                    } else {
                        R.string.stripe_position_dl_back
                    }
                }
                else -> {
                    if (targetScanType.isNullOrFront()) {
                        R.string.stripe_position_id_front
                    } else {
                        R.string.stripe_position_id_back
                    }
                }
            }
        }

        return if (targetScanType.isNullOrFront()) {
            R.string.stripe_position_id_front
        } else {
            R.string.stripe_position_id_back
        }
    }

    internal class DocumentScanViewModelFactory @Inject constructor(
        private val verificationFlowFinishable: VerificationFlowFinishable,
        private val modelPerformanceTracker: ModelPerformanceTracker,
        private val laplacianBlurDetector: LaplacianBlurDetector,
        private val fpsTracker: FPSTracker,
        private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
        private val identityViewModel: IdentityViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return DocumentScanViewModel(
                extras.requireApplication(),
                fpsTracker,
                identityAnalyticsRequestFactory,
                modelPerformanceTracker,
                laplacianBlurDetector,
                verificationFlowFinishable,
                identityViewModel
            ) as T
        }
    }
}
