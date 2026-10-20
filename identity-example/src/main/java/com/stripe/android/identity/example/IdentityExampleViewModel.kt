@file:OptIn(LinkControllerPreview::class)

package com.stripe.android.identity.example

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.result.Result
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.example.ui.IdentitySubmissionState
import com.stripe.android.identity.example.ui.VerificationType
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkControllerPreview
import com.stripe.android.networking.RequestSurface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * The playground backend's merchant (Andrew's Audio). Networked Identity is live mode only, and Link must use
 * the same account and mode as the VerificationSession.
 */
private const val PLAYGROUND_MERCHANT_PUBLISHABLE_KEY =
    "pk_live_51H34dXGMZYGNxJkBczBS5XMk0Hsf3AZ9GzvcspbZN4aZzZm3TTe9kBoX4D3R77CsxuHCBHbw1pQnm9HFTgHQQsdM00sFIApTez"

/**
 * Networked Identity playground state. The example acts as the "outside" host: it signs in to Link
 * itself and hands the resulting consumer session to Identity, like crypto onramp would.
 */
internal data class NetworkedIdentityExampleState(
    val linkEmail: String = "",
    val busy: Boolean = false,
    val status: String? = null,
    val handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff? = null,
)

@Suppress("TooManyFunctions")
internal class IdentityExampleViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val linkController =
        LinkController.create(application, savedStateHandle, RequestSurface.CryptoOnramp)
    private var linkPresenter: LinkController.Presenter? = null

    private val _networkedIdentity = MutableStateFlow(NetworkedIdentityExampleState())
    val networkedIdentity: StateFlow<NetworkedIdentityExampleState> = _networkedIdentity.asStateFlow()

    fun updateNetworkedIdentity(
        update: (NetworkedIdentityExampleState) -> NetworkedIdentityExampleState
    ) {
        _networkedIdentity.update(update)
    }

    /** Call from the activity's onCreate: Link's screens report back through activity results. */
    fun attachLinkPresenter(activity: ComponentActivity) {
        val created = linkController.createPresenter(
            activity = activity,
            presentPaymentMethodsCallback = {},
            authenticationCallback = ::onLinkAuthenticated,
            authorizeCallback = {},
        )
        linkPresenter = created
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (linkPresenter === created) linkPresenter = null
                }
            }
        )
    }

    /**
     * Signs in to Link with Link's own screens, outside of Identity, like crypto onramp does, so the
     * handoff can be passed to the verification sheet.
     */
    fun signInToLink() {
        val email = _networkedIdentity.value.linkEmail.trim()
        if (email.isEmpty()) {
            setStatus("Enter a Link email first.")
            return
        }
        viewModelScope.launch {
            _networkedIdentity.update { it.copy(busy = true, status = "Signing in to Link…") }
            val configured = linkController.configure(
                LinkController.Configuration(
                    publishableKey = PLAYGROUND_MERCHANT_PUBLISHABLE_KEY,
                    merchantDisplayName = "Identity Example",
                    email = email,
                )
            )
            configured.exceptionOrNull()?.let { return@launch fail(it) }
            val presenter = linkPresenter ?: return@launch fail(IllegalStateException("No activity can present Link."))
            presenter.authenticate(email)
        }
    }

    /** Drops the local handoff only. The Link session itself stays signed in. */
    fun clearHandoff() {
        _networkedIdentity.update { it.copy(handoff = null, status = "Handoff cleared.") }
    }

    fun networkedIdentityOptions() = IdentityVerificationSheet.Configuration.NetworkedIdentityOptions(
        linkSessionHandoff = _networkedIdentity.value.handoff,
        merchantPublishableKey = PLAYGROUND_MERCHANT_PUBLISHABLE_KEY,
    )

    private fun onLinkAuthenticated(result: LinkController.AuthenticationResult) {
        when (result) {
            LinkController.AuthenticationResult.Success -> captureHandoff()
            LinkController.AuthenticationResult.Canceled ->
                _networkedIdentity.update { it.copy(busy = false, status = "Link sign-in canceled.") }
            is LinkController.AuthenticationResult.Failed -> fail(result.error)
        }
    }

    private fun captureHandoff() {
        val account = linkController.state(getApplication()).value.internalLinkAccount
        val linkSessionKey = account?.linkSessionKey
        if (account == null || linkSessionKey.isNullOrBlank()) {
            fail(IllegalStateException("Link returned no consumer session."))
            return
        }
        _networkedIdentity.update {
            it.copy(
                busy = false,
                handoff = IdentityVerificationSheet.Configuration.LinkSessionHandoff(
                    email = account.email,
                    linkSessionKey = linkSessionKey,
                ),
                status = "Link session ready for ${account.email}.",
            )
        }
    }

    private fun fail(error: Throwable) {
        _networkedIdentity.update { it.copy(busy = false, status = "Link error: ${error.message}") }
    }

    private fun setStatus(message: String) {
        _networkedIdentity.update { it.copy(status = message) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }
    }

    fun postForResult(
        submissionState: IdentitySubmissionState
    ) = liveData {
        val result = Fuel.post(
            if (submissionState.useTestMode) {
                TEST_MODE_EXAMPLE_BACKEND_URL
            } else {
                EXAMPLE_BACKEND_URL
            }
        )
            .header("content-type", "application/json")
            .body(
                json.encodeToString(
                    VerificationSessionCreationRequest.serializer(),
                    VerificationSessionCreationRequest(
                        type = submissionState.verificationType.value,
                        threeDFaceCaptureEnabled = if (submissionState.requireSelfie) {
                            submissionState.threeDFaceCaptureEnabled
                        } else {
                            null
                        },
                        options =
                        when (submissionState.verificationType) {
                            VerificationType.DOCUMENT -> {
                                VerificationSessionCreationRequest.Options(
                                    document = submissionState.toDocumentOptions(),
                                    phone = submissionState.toPhoneOptions()
                                )
                            }

                            VerificationType.PHONE -> {
                                VerificationSessionCreationRequest.Options(
                                    document = if (submissionState.useDocumentFallback == true) {
                                        submissionState.toDocumentOptions()
                                    } else {
                                        null
                                    },
                                    phoneOtp = VerificationSessionCreationRequest.Options.PhoneOTP(
                                        check = submissionState.phoneOtpCheck ?: PhoneOTPCheck.None
                                    ),
                                    phoneRecords = if (submissionState.useDocumentFallback == true) {
                                        VerificationSessionCreationRequest.Options.PhoneRecords(
                                            fallback = Fallback.Document
                                        )
                                    } else {
                                        null
                                    }
                                )
                            }

                            VerificationType.ID_NUMBER -> {
                                VerificationSessionCreationRequest.Options(
                                    phone = submissionState.toPhoneOptions()
                                )
                            }

                            VerificationType.ADDRESS -> {
                                VerificationSessionCreationRequest.Options()
                            }
                        },
                        providedDetails = when (submissionState.verificationType) {
                            VerificationType.DOCUMENT -> {
                                if (submissionState.requirePhoneVerification == true) {
                                    VerificationSessionCreationRequest.ProvidedDetails(
                                        phone = requireNotNull(
                                            submissionState.providedPhoneNumber
                                        )
                                    )
                                } else {
                                    null
                                }
                            }

                            VerificationType.ADDRESS -> {
                                null
                            }

                            VerificationType.ID_NUMBER -> {
                                if (submissionState.requirePhoneVerification == true) {
                                    VerificationSessionCreationRequest.ProvidedDetails(
                                        phone = requireNotNull(
                                            submissionState.providedPhoneNumber
                                        )
                                    )
                                } else {
                                    null
                                }
                            }

                            VerificationType.PHONE -> {
                                null
                            }
                        }.withEmail(_networkedIdentity.value.linkEmail.trim().ifEmpty { null })
                    )
                )
            ).awaitStringResult()

        if (result is Result.Failure) {
            emit(result)
        } else {
            try {
                json.decodeFromString(
                    VerificationSessionCreationResponse.serializer(),
                    result.get()
                ).let {
                    emit(Result.success(it))
                }
            } catch (t: Throwable) {
                emit(Result.error(Exception(t)))
            }
        }
    }

    private fun IdentitySubmissionState.toDocumentOptions() =
        VerificationSessionCreationRequest.Options.Document(
            requireIdNumber = this.requireId,
            requireMatchingSelfie = this.requireSelfie,
            requireLiveCapture = this.requireLiveCapture,
            requireAddress = this.requireAddress,
            allowedTypes = mutableListOf<DocumentType>().also {
                if (this.allowDrivingLicense) {
                    it.add(
                        DocumentType.DrivingLicense
                    )
                }
                if (this.allowPassport) {
                    it.add(
                        DocumentType.Passport
                    )
                }
                if (this.allowId) it.add(DocumentType.IdCard)
            }
        )

    private fun IdentitySubmissionState.toPhoneOptions() =
        if (this.requirePhoneVerification == true) {
            VerificationSessionCreationRequest.Options.Phone(
                requireVerification = true
            )
        } else {
            null
        }

    private companion object {
        // if you want to view and fork the backend code, go to
        // https://codesandbox.io/p/devbox/compassionate-violet-gshhgf
        const val EXAMPLE_BACKEND_URL =
            "https://stripe-mobile-identity-verification-playground.stripedemos.com/verification-sessions"
        const val TEST_MODE_EXAMPLE_BACKEND_URL =
            "https://stripe-mobile-identity-verification-playground.stripedemos.com/test/verification-sessions"
    }
}
