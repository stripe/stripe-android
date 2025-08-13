package com.stripe.android.identity.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
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

    // Helper to get allowed document types as a sorted list
    private fun getAllowedDocumentTypes(): List<String> {
        return identityViewModel.verificationPage.value
            ?.data
            ?.documentSelect
            ?.idDocumentTypeAllowlist
            ?.keys
            ?.sorted()
            ?: listOf("id_card", "passport", "driving_license")
    }

    @OptIn(FlowPreview::class)
    override val scanFeedback = combine(
        scannerState,
        targetScanTypeFlow,
        identityViewModel.verificationPage.asFlow()
    ) { scannerState, targetScanType, _ ->
        when (scannerState) {
            State.Initializing -> idleFeedback(targetScanType)
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

    // Helper for driving_license + id_card
    private fun getDriverLicenseOrIdStringRes(isFront: Boolean): Int {
        return if (isFront) {
            R.string.stripe_front_of_driver_license_or_id
        } else {
            R.string.stripe_back_of_driver_license_or_id
        }
    }

    // Helper for driving_license + passport
    private fun getDriverLicenseOrPassportStringRes(isFront: Boolean): Int {
        return if (isFront) {
            R.string.stripe_front_of_driver_license_or_passport
        } else {
            R.string.stripe_back_of_driver_license_or_passport
        }
    }

    // Helper for id_card + passport
    private fun getPassportOrIdStringRes(isFront: Boolean): Int {
        return if (isFront) {
            R.string.stripe_front_of_passport_or_id
        } else {
            R.string.stripe_back_of_passport_or_id
        }
    }

    // Helper for all three types
    private fun getAllIdTypesStringRes(isFront: Boolean): Int {
        return if (isFront) {
            R.string.stripe_front_of_all_id_types
        } else {
            R.string.stripe_back_of_all_id_types
        }
    }

    // Helper for driving_license only
    private fun getDriverLicenseStringRes(isFront: Boolean): Int {
        return if (isFront) {
            R.string.stripe_front_of_dl
        } else {
            R.string.stripe_back_of_dl
        }
    }

    // Helper for passport only
    private fun getPassportStringRes(isFront: Boolean): Int {
        return if (isFront) {
            R.string.stripe_front_of_passport
        } else {
            R.string.stripe_back_of_passport
        }
    }

    // Helper for id_card only
    private fun getIdDocumentStringRes(isFront: Boolean): Int {
        return if (isFront) {
            R.string.stripe_front_of_id_document
        } else {
            R.string.stripe_back_of_id_document
        }
    }

    // Helper for fallback
    private fun getFallbackIdDocumentStringRes(isFront: Boolean): Int {
        return if (isFront) {
            R.string.stripe_front_of_id_document
        } else {
            R.string.stripe_back_of_id_document
        }
    }

    // Helper to get the correct string resource for scan title based on allowed types and side
    fun getDocumentPositionStringRes(targetScanType: IdentityScanState.ScanType? = null): Int {
        val allowlist = getAllowedDocumentTypes()
        val isFront = targetScanType.isNullOrFront()
        return when (allowlist) {
            listOf("driving_license", "id_card") -> getDriverLicenseOrIdStringRes(isFront)
            listOf("driving_license", "passport") -> getDriverLicenseOrPassportStringRes(isFront)
            listOf("id_card", "passport") -> getPassportOrIdStringRes(isFront)
            listOf("driving_license", "id_card", "passport") -> getAllIdTypesStringRes(isFront)
            listOf("driving_license") -> getDriverLicenseStringRes(isFront)
            listOf("passport") -> getPassportStringRes(isFront)
            listOf("id_card") -> getIdDocumentStringRes(isFront)
            else -> getFallbackIdDocumentStringRes(isFront)
        }
    }

    // Helper to get the correct string resource for scan instructions based on allowed types and side
    private fun idleFeedback(targetScanType: IdentityScanState.ScanType? = null): Int {
        val allowlist = getAllowedDocumentTypes()
        val isFront = targetScanType.isNullOrFront()
        return when (allowlist) {
            listOf("driving_license", "id_card") -> if (isFront) {
                R.string.stripe_position_driver_license_or_id
            } else {
                R.string.stripe_flip_driver_license_or_id
            }
            listOf("driving_license", "passport") -> if (isFront) {
                R.string.stripe_position_driver_license_or_passport
            } else {
                R.string.stripe_flip_driver_license_or_passport
            }
            listOf("id_card", "passport") -> if (isFront) {
                R.string.stripe_position_passport_or_id
            } else {
                R.string.stripe_flip_passport_or_id
            }
            listOf("driving_license", "id_card", "passport") -> if (isFront) {
                R.string.stripe_position_all_id_types
            } else {
                R.string.stripe_flip_all_id_types
            }
            listOf("driving_license") -> if (isFront) {
                R.string.stripe_position_dl_front
            } else {
                R.string.stripe_position_dl_back
            }
            listOf("passport") -> R.string.stripe_position_passport
            listOf("id_card") -> if (isFront) {
                R.string.stripe_position_id_front
            } else {
                R.string.stripe_position_id_back
            }
            else -> if (isFront) {
                R.string.stripe_position_id_front
            } else {
                R.string.stripe_position_id_back
            }
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
