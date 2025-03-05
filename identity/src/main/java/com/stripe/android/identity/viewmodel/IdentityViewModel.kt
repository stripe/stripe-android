package com.stripe.android.identity.viewmodel

import android.app.Application
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.injection.IdentityActivitySubcomponent
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.VerificationPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel hosted by IdentityActivity, shared across fragments.
 */
internal class IdentityViewModel(
    application: Application,
    internal val verificationArgs: IdentityVerificationSheetContract.Args,
    private val identityRepository: IdentityRepository,
    internal val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    /**
     * StateFlow to track camera permissions
     */
    private val _cameraPermissionGranted = MutableStateFlow(
        savedStateHandle[CAMERA_PERMISSION_GRANTED] ?: false
    )
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted

    /**
     * StateFlow to track request status of postVerificationPageData
     */
    @VisibleForTesting
    internal val verificationPageData = MutableStateFlow<Resource<Int>>(
        savedStateHandle[VERIFICATION_PAGE_DATA] ?: Resource.idle()
    )

    /**
     * StateFlow to track request status of postVerificationPageSubmit
     */
    @VisibleForTesting
    internal val verificationPageSubmit = MutableStateFlow<Resource<Int>>(
        savedStateHandle[VERIFICATION_PAGE_SUBMIT] ?: Resource.idle()
    )

    /**
     * Response for initial VerificationPage, used for building UI.
     */
    @VisibleForTesting
    internal val _verificationPage: MutableLiveData<Resource<VerificationPage>> =
        // No need to write to savedStateHandle for livedata
        savedStateHandle.getLiveData(
            key = VERIFICATION_PAGE,
            initialValue = Resource.idle()
        )

    val verificationPage: LiveData<Resource<VerificationPage>> = _verificationPage

    data class PageAndModelFiles(
        val page: VerificationPage
    )

    /**
     * Wrapper for both page and model
     */
    val pageAndModelFiles = object : MediatorLiveData<Resource<PageAndModelFiles>>() {
        private var page: VerificationPage? = null

        init {
            postValue(Resource.loading())
            addSource(verificationPage) {
                when (it.status) {
                    Status.SUCCESS -> {
                        page = it.data
                        maybePostSuccess()
                    }

                    Status.ERROR -> {
                        postValue(Resource.error("$verificationPage posts error"))
                    }

                    Status.LOADING -> {} // no-op
                    Status.IDLE -> {}
                }
            }
        }

        private fun maybePostSuccess() {
            page?.let { page ->
                postValue(Resource.success(PageAndModelFiles(page)))
            }
        }
    }

    /**
     * LiveData for the cause of ErrorScreen.
     */
    val errorCause = MutableLiveData<Throwable>()
    private val errorCauseObServer = Observer<Throwable> { value -> logError(value) }

    init {
        errorCause.observeForever(errorCauseObServer)
    }

    override fun onCleared() {
        super.onCleared()
        errorCause.removeObserver(errorCauseObServer)
    }


    /**
     * Simple wrapper for observing [verificationPage].
     */
    fun observeForVerificationPage(
        owner: LifecycleOwner,
        onSuccess: (VerificationPage) -> Unit,
        onFailure: (Throwable) -> Unit = {
            Log.d(TAG, "Failed to get VerificationPage")
        }
    ) {
        verificationPage.observe(owner) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    onSuccess(requireNotNull(resource.data))
                }

                Status.ERROR -> {
                    Log.e(TAG, "Fail to get VerificationPage")
                    onFailure(requireNotNull(resource.throwable))
                }

                Status.LOADING -> {} // no-op
                Status.IDLE -> {} // no-op
            }
        }
    }

    /**
     * Retrieve the VerificationPage data and post its value to [verificationPage]
     */
    fun retrieveAndBufferVerificationPage() {
        _verificationPage.postValue(Resource.loading())
        viewModelScope.launch {
            runCatching {
                identityRepository.retrieveVerificationPage(
                    verificationArgs.verificationSessionId,
                    verificationArgs.ephemeralKeySecret
                )
            }.fold(
                onSuccess = { verificationPage ->
                    _verificationPage.postValue(Resource.success(verificationPage))
                    identityAnalyticsRequestFactory.verificationPage = verificationPage
                },
                onFailure = {
                    "Failed to retrieve verification page with " +
                        (
                            "sessionID: ${verificationArgs.verificationSessionId} and ephemeralKey: " +
                                verificationArgs.ephemeralKeySecret
                            )
                            .let { msg ->
                                _verificationPage.postValue(Resource.error(msg, IllegalStateException(msg, it)))
                            }
                }
            )
        }
    }

    private fun logError(cause: Throwable) {
        identityAnalyticsRequestFactory.genericError(
            cause.message,
            cause.stackTraceToString()
        )
    }

    internal class IdentityViewModelFactory(
        private val applicationSupplier: () -> Application,
        private val subcomponentSupplier: () -> IdentityActivitySubcomponent,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val subcomponent = subcomponentSupplier()
            val savedStateHandle = extras.createSavedStateHandle()

            return IdentityViewModel(
                applicationSupplier(),
                subcomponent.verificationArgs,
                subcomponent.identityRepository,
                subcomponent.identityAnalyticsRequestFactory,
                savedStateHandle,
            ) as T
        }
    }

    internal companion object {
        val TAG: String = IdentityViewModel::class.java.simpleName
        const val FRONT = "front"
        const val BACK = "back"
        private const val CAMERA_PERMISSION_GRANTED = "cameraPermissionGranted"
        private const val VERIFICATION_PAGE = "verification_page"
        private const val VERIFICATION_PAGE_DATA = "verification_page_data"
        private const val VERIFICATION_PAGE_SUBMIT = "verification_page_submit"
    }
}
