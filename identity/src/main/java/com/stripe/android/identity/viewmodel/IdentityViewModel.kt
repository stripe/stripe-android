package com.stripe.android.identity.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import kotlinx.coroutines.launch

/**
 * ViewModel hosted by IdentityActivity, shared across fragments.
 */
internal class IdentityViewModel(
    internal val args: IdentityVerificationSheetContract.Args,
    private val identityRepository: IdentityRepository
) : ViewModel() {

    /**
     * Response for initial VerificationPage, used for building UI.
     */
    private val _verificationPage = MutableLiveData<VerificationPage>()
    val verificationPage: LiveData<VerificationPage> = _verificationPage

    /**
     * API request fails, could be [APIException] if the request returns with an error response,
     * or [APIConnectionException] if the request fails.
     */
    private val _verificationPageApiError = MutableLiveData<Throwable>()
    val verificationPageApiError: LiveData<Throwable> = _verificationPageApiError

    /**
     * Retrieve the VerificationPage data and post it as [verificationPage]
     * or error result as [verificationPageApiError].
     */
    fun retrieveAndBufferVerificationPage() {
        viewModelScope.launch {
            runCatching {
                identityRepository.retrieveVerificationPage(
                    args.verificationSessionId,
                    args.ephemeralKeySecret
                )
            }.fold(
                onSuccess = _verificationPage::postValue,
                onFailure = _verificationPageApiError::postValue
            )
        }
    }

    /**
     * Post collected [CollectedDataParam] to update [VerificationPageData].
     */
    @Throws(
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postVerificationPageData(collectedDataParam: CollectedDataParam) =
        identityRepository.postVerificationPageData(
            args.verificationSessionId,
            args.ephemeralKeySecret,
            collectedDataParam
        )

    /**
     * Submit the final [VerificationPageData].
     */
    @Throws(
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postVerificationPageSubmit() =
        identityRepository.postVerificationPageSubmit(
            args.verificationSessionId,
            args.ephemeralKeySecret
        )

    internal class IdentityViewModelFactory(
        private val args: IdentityVerificationSheetContract.Args,
        private val identityRepository: IdentityRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityViewModel(args, identityRepository) as T
        }
    }
}
