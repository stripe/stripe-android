package com.stripe.android.identity.viewmodel

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import kotlinx.coroutines.launch
import java.io.File

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
    private val _verificationPage = MutableLiveData<Resource<VerificationPage>>()
    val verificationPage: LiveData<Resource<VerificationPage>> = _verificationPage

    /**
     * Network response for the IDDetector model.
     */
    private val _idDetectorModelFile = MutableLiveData<Resource<File>>()
    val idDetectorModelFile: LiveData<Resource<File>> = _idDetectorModelFile

    /**
     * Simple wrapper for observing [verificationPage].
     */
    fun observeForVerificationPage(
        owner: LifecycleOwner,
        onSuccess: (VerificationPage) -> Unit,
        onFailure: (Throwable?) -> Unit,
    ) {
        verificationPage.observe(owner) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    onSuccess(requireNotNull(resource.data))
                }
                Status.ERROR -> {
                    Log.e(TAG, "Fail to get VerificationPage")
                    onFailure(resource.throwable)
                }
                Status.LOADING -> {} // no-op
            }
        }
    }

    /**
     * Retrieve the VerificationPage data and post its value to [verificationPage]
     */
    fun retrieveAndBufferVerificationPage(shouldRetrieveModel: Boolean = true) {
        viewModelScope.launch {
            runCatching {
                _verificationPage.postValue(Resource.loading())
                identityRepository.retrieveVerificationPage(
                    args.verificationSessionId,
                    args.ephemeralKeySecret
                )
            }.fold(
                onSuccess = {
                    _verificationPage.postValue(Resource.success(it))
                    if (shouldRetrieveModel) {
                        downloadIDDetectorModel(it.documentCapture.models.idDetectorUrl)
                    }
                },
                onFailure = {
                    _verificationPage.postValue(
                        Resource.error(
                            "Failed to retrieve verification page with " +
                                "sessionID: ${args.verificationSessionId} and ephemeralKey: ${args.ephemeralKeySecret}",
                            it
                        ),
                    )
                }
            )
        }
    }

    /**
     * Download the IDDetector model and post its value to [idDetectorModelFile].
     */
    private fun downloadIDDetectorModel(modelUrl: String) {
        viewModelScope.launch {
            runCatching {
                _idDetectorModelFile.postValue(Resource.loading())
                identityRepository.downloadModel(modelUrl)
            }.fold(
                onSuccess = {
                    _idDetectorModelFile.postValue(Resource.success(it))
                },
                onFailure = {
                    _idDetectorModelFile.postValue(
                        Resource.error(
                            "Failed to download model from $modelUrl",
                            it
                        )
                    )
                }
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

    private companion object {
        val TAG: String = IdentityViewModel::class.java.simpleName
    }
}
