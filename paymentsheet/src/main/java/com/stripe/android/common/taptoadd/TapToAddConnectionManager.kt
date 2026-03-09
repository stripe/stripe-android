package com.stripe.android.common.taptoadd

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TapUseCase
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

internal interface TapToAddConnectionManager {
    /**
     * Indicates if the device has the support required to use the on-device NFC reader
     */
    val isSupported: Boolean

    /**
     * Indicates if the NFC reader has been connected to.
     */
    val isConnected: Boolean

    /**
     * Starts connecting to the NFC reader. If already connected or unsupported, will do nothing.
     */
    fun connect()

    /**
     * Waits for connection to NFC reader to be completed or returns current connection status of the NFC reader.
     */
    suspend fun awaitConnection(): Result<Boolean>

    companion object {
        fun create(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            applicationContext: Context,
            paymentConfiguration: PaymentConfiguration,
            workContext: CoroutineContext,
            isSimulated: Boolean,
        ): TapToAddConnectionManager {
            return if (isStripeTerminalSdkAvailable()) {
                DefaultTapToAddConnectionManager(
                    applicationContext = applicationContext,
                    workContext = workContext,
                    paymentConfiguration = paymentConfiguration,
                    errorReporter = errorReporter,
                    terminalWrapper = terminalWrapper,
                    isSimulated = isSimulated,
                )
            } else {
                UnsupportedTapToAddConnectionManager()
            }
        }
    }
}

@OptIn(TapToAddPreview::class)
internal class DefaultTapToAddConnectionManager(
    applicationContext: Context,
    workContext: CoroutineContext,
    private val paymentConfiguration: PaymentConfiguration,
    private val errorReporter: ErrorReporter,
    private val terminalWrapper: TerminalWrapper,
    isSimulated: Boolean,
) : TapToAddConnectionManager, TerminalListener, TapToPayReaderListener {
    private var connectionTask: CompletableDeferred<Boolean>? = null

    private val discoveryConfiguration = DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulated)
    private val workScope = CoroutineScope(workContext)
    private val connectionStartLock = Mutex()

    override val isSupported: Boolean
        get() {
            return FeatureFlags.enableTapToAdd.isEnabled && terminal().supportsReadersOfType(
                deviceType = DeviceType.TAP_TO_PAY_DEVICE,
                discoveryConfiguration = discoveryConfiguration,
            ).isSupported
        }

    override val isConnected: Boolean
        get() = terminal().connectedReader != null

    init {
        if (!terminalWrapper.isInitialized()) {
            terminalWrapper.initTerminal(
                context = applicationContext,
                tokenProvider = object : ConnectionTokenProvider {
                    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
                        callback.onSuccess(paymentConfiguration.publishableKey)
                    }
                },
                listener = this,
            )
        }
    }

    override fun connect() {
        workScope.launch {
            val canContinue = connectionStartLock.withLock {
                if (!isSupported || isConnected || connectionTask?.isActive == true) {
                    false
                } else {
                    connectionTask = CompletableDeferred()
                    true
                }
            }

            if (!canContinue) {
                return@launch
            }

            try {
                discoverReaders()
            } catch (exception: SecurityException) {
                errorReporter.report(
                    ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_LOCATION_PERMISSIONS_FAILURE,
                    StripeException.create(exception),
                )

                connectionTask?.completeExceptionally(exception)
            }
        }
    }

    override suspend fun awaitConnection(): Result<Boolean> {
        return runCatching {
            isConnected || connectionTask?.await() ?: false
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverReaders() {
        terminal().discoverReaders(
            config = discoveryConfiguration,
            discoveryListener = object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    onUpdatedReaders(readers)
                }
            },
            callback = object : Callback {
                override fun onFailure(e: TerminalException) {
                    handleConnectError(
                        error = e,
                        errorEvent = ErrorReporter.ExpectedErrorEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_FAILURE,
                    )
                }

                override fun onSuccess() {
                    errorReporter.report(ErrorReporter.SuccessEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_SUCCESS)
                }
            }
        )
    }

    private fun onUpdatedReaders(readers: List<Reader>) {
        val reader = readers.firstOrNull() ?: run {
            /*
             * The Tap to Pay variant should never not return a reader through this callback.
             * If no readers are found, something has changed in the internal implementation
             * of Terminal that we should know about
             */
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_NO_READER_FOUND,
            )

            connectionTask?.completeExceptionally(IllegalStateException("No reader found!"))
            return
        }

        terminal().connectReader(
            reader = reader,
            config = ConnectionConfiguration.TapToPayConnectionConfiguration(
                useCase = TapUseCase.Verify(),
                autoReconnectOnUnexpectedDisconnect = true,
                tapToPayReaderListener = this@DefaultTapToAddConnectionManager,
            ),
            connectionCallback = object : ReaderCallback {
                override fun onFailure(e: TerminalException) {
                    handleConnectError(
                        error = e,
                        errorEvent = ErrorReporter.ExpectedErrorEvent.TAP_TO_ADD_CONNECT_READER_CALL_FAILURE,
                    )
                }

                override fun onSuccess(reader: Reader) {
                    errorReporter.report(
                        ErrorReporter.SuccessEvent.TAP_TO_ADD_CONNECT_READER_CALL_SUCCESS
                    )

                    connectionTask?.complete(true)
                }
            }
        )
    }

    private fun terminal() = terminalWrapper.getInstance()

    private fun handleConnectError(
        error: TerminalException,
        errorEvent: ErrorReporter.ErrorEvent,
    ) {
        when (error.errorCode) {
            TerminalErrorCode.ALREADY_CONNECTED_TO_READER -> connectionTask?.complete(isConnected)
            else -> {
                errorReporter.report(
                    errorEvent,
                    StripeException.create(error),
                )

                connectionTask?.completeExceptionally(error)
            }
        }
    }
}

internal class UnsupportedTapToAddConnectionManager : TapToAddConnectionManager {
    override val isSupported: Boolean = false
    override val isConnected: Boolean = false

    override fun connect() {
        // No-op
    }

    override suspend fun awaitConnection(): Result<Boolean> {
        return Result.success(false)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
sealed interface CreateConnectionTokenResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Success(
        internal val connectionToken: String,
    ) : CreateConnectionTokenResult

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failure(
        internal val cause: Exception,
        internal val message: String,
    ) : CreateConnectionTokenResult
}
