package com.stripe.android.common.taptoadd

import android.annotation.SuppressLint
import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ExponentialBackoffRetryDelaySupplier
import com.stripe.android.core.networking.RetryDelaySupplier
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal interface TapToAddConnectionManager {
    /**
     * Indicates if the device has the support required to use the on-device NFC reader
     */
    val isSupported: Boolean

    /**
     * Connects to the NFC reader. Successful completion of this function indicates a successful connection while
     * an interruption will result from a connection failure
     */
    suspend fun connect()

    companion object {
        fun create(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            applicationContext: Context,
            logger: Logger,
            paymentConfiguration: Provider<PaymentConfiguration>,
            workContext: CoroutineContext,
            isSimulatedProvider: TapToAddIsSimulatedProvider,
        ): TapToAddConnectionManager {
            return if (isStripeTerminalSdkAvailable()) {
                TapToAddRetriableConnectionManager(
                    tapToAddConnectionManager = DefaultTapToAddConnectionManager(
                        applicationContext = applicationContext,
                        workContext = workContext,
                        paymentConfiguration = paymentConfiguration,
                        errorReporter = errorReporter,
                        terminalWrapper = terminalWrapper,
                        logger = logger,
                        isSimulatedProvider = isSimulatedProvider,
                    ),
                    fatalErrorChecker = DefaultTapToAddFatalErrorChecker(),
                    retryDelaySupplier = ExponentialBackoffRetryDelaySupplier(),
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
    private val workContext: CoroutineContext,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
    private val errorReporter: ErrorReporter,
    private val terminalWrapper: TerminalWrapper,
    private val logger: Logger,
    isSimulatedProvider: TapToAddIsSimulatedProvider,
) : TapToAddConnectionManager, TerminalListener, TapToPayReaderListener {
    private var connectionTask: CompletableDeferred<Unit>? = null

    private val discoveryConfiguration by lazy {
        DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulatedProvider.get())
    }

    private val connectionTaskLock = Mutex()

    override val isSupported: Boolean
        get() {
            return terminal().supportsReadersOfType(
                deviceType = DeviceType.TAP_TO_PAY_DEVICE,
                discoveryConfiguration = discoveryConfiguration,
            ).isSupported
        }

    init {
        if (!terminalWrapper.isInitialized()) {
            terminalWrapper.initTerminal(
                context = applicationContext,
                tokenProvider = object : ConnectionTokenProvider {
                    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
                        callback.onSuccess(paymentConfiguration.get().publishableKey)
                    }
                },
                listener = this,
            )
        }
    }

    override suspend fun connect() = withContext(workContext) {
        runCatching {
            if (!isSupported) {
                throw IllegalStateException("Tap to Add is not supported by this device!")
            }

            if (terminal().connectedReader != null) {
                return@withContext
            }

            val existingTask = connectionTaskLock.withLock {
                return@withLock connectionTask ?: run {
                    connectionTask = CompletableDeferred()
                    null
                }
            }

            existingTask?.let { task ->
                return@withContext task.await()
            }

            val discoverReadersResult = discoverReaders()

            if (discoverReadersResult is DiscoverCallResult.CollectedReaders) {
                connectReader(discoverReadersResult.readers)
            }
        }.fold(
            onSuccess = {
                connectionTaskLock.withLock {
                    connectionTask?.complete(Unit)
                    connectionTask = null
                }
            },
            onFailure = { error ->
                connectionTaskLock.withLock {
                    connectionTask?.completeExceptionally(error)
                    connectionTask = null
                }

                throw error
            }
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun discoverReaders() = suspendCancellableCoroutine { continuation ->
        try {
            val cancellable = terminal().discoverReaders(
                config = discoveryConfiguration,
                discoveryListener = object : DiscoveryListener {
                    override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                        continuation.resumeWith(Result.success(DiscoverCallResult.CollectedReaders(readers)))
                    }
                },
                callback = object : Callback {
                    override fun onFailure(e: TerminalException) {
                        if (e.isAlreadyConnectedToReader()) {
                            errorReporter.report(
                                ErrorReporter.SuccessEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_SUCCESS
                            )

                            continuation.resumeWith(Result.success(DiscoverCallResult.AlreadyConnected))
                        } else {
                            reportError(
                                error = e,
                                errorEvent =
                                    ErrorReporter.ExpectedErrorEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_FAILURE,
                            )

                            continuation.resumeWith(Result.failure(e))
                        }
                    }

                    override fun onSuccess() {
                        errorReporter.report(
                            ErrorReporter.SuccessEvent.TAP_TO_ADD_DISCOVER_READERS_CALL_SUCCESS
                        )
                    }
                }
            )

            continuation.invokeOnCancellation {
                cancellable.cancel(
                    object : Callback {
                        override fun onSuccess() {
                            // No-op
                        }

                        override fun onFailure(e: TerminalException) {
                            reportError(
                                error = e,
                                errorEvent =
                                    ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_DISCOVER_READERS_CANCEL_FAILURE,
                            )
                        }
                    }
                )
            }
        } catch (exception: SecurityException) {
            reportError(
                error = exception,
                errorEvent = ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_LOCATION_PERMISSIONS_FAILURE,
            )

            continuation.resumeWith(Result.failure(exception))
        }
    }

    private suspend fun connectReader(readers: List<Reader>) = suspendCancellableCoroutine { continuation ->
        val reader = readers.firstOrNull() ?: run {
            val exception = IllegalStateException("No reader found!")

            reportError(
                error = exception,
                errorEvent = ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_NO_READER_FOUND,
            )

            continuation.resumeWith(Result.failure(exception))

            return@suspendCancellableCoroutine
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
                    if (e.isAlreadyConnectedToReader()) {
                        errorReporter.report(
                            ErrorReporter.SuccessEvent.TAP_TO_ADD_CONNECT_READER_CALL_SUCCESS
                        )

                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        reportError(
                            error = e,
                            errorEvent = ErrorReporter.ExpectedErrorEvent.TAP_TO_ADD_CONNECT_READER_CALL_FAILURE,
                        )

                        continuation.resumeWith(Result.failure(e))
                    }
                }

                override fun onSuccess(reader: Reader) {
                    errorReporter.report(
                        ErrorReporter.SuccessEvent.TAP_TO_ADD_CONNECT_READER_CALL_SUCCESS
                    )

                    continuation.resumeWith(Result.success(Unit))
                }
            }
        )
    }

    private fun terminal() = terminalWrapper.getInstance()

    private fun Throwable.isAlreadyConnectedToReader(): Boolean {
        return this is TerminalException && errorCode == TerminalErrorCode.ALREADY_CONNECTED_TO_READER
    }

    private fun reportError(
        error: Throwable,
        errorEvent: ErrorReporter.ErrorEvent?,
    ) {
        val additionalParams = mutableMapOf<String, String>()

        if (error is TerminalException) {
            additionalParams[TERMINAL_ERROR_CODE_KEY] = error.errorCode.toLogString()
        }

        errorEvent?.let { event ->
            errorReporter.report(
                event,
                StripeException.create(error),
            )
        }

        logger.warning("TapToAddConnectionError: $error")
    }

    private sealed interface DiscoverCallResult {
        data class CollectedReaders(val readers: List<Reader>) : DiscoverCallResult
        data object AlreadyConnected : DiscoverCallResult
    }
}

internal class UnsupportedTapToAddConnectionManager : TapToAddConnectionManager {
    override val isSupported: Boolean = false

    override suspend fun connect() {
        // No-op
    }
}

internal class TapToAddRetriableConnectionManager(
    private val tapToAddConnectionManager: TapToAddConnectionManager,
    private val fatalErrorChecker: TapToAddFatalErrorChecker,
    private val retryDelaySupplier: RetryDelaySupplier,
) : TapToAddConnectionManager by tapToAddConnectionManager {
    override suspend fun connect() {
        var retriesRemaining = MAX_RETRIES

        while (true) {
            runCatching {
                tapToAddConnectionManager.connect()
            }.fold(
                onSuccess = {
                    break
                },
                onFailure = { error ->
                    if (retriesRemaining == 0 || fatalErrorChecker.isFatal(error)) {
                        throw error
                    } else {
                        delay(
                            retryDelaySupplier.getDelay(
                                maxRetries = MAX_RETRIES,
                                remainingRetries = retriesRemaining
                            )
                        )

                        retriesRemaining--
                    }
                }
            )
        }
    }

    private companion object {
        private const val MAX_RETRIES = 3
    }
}

private const val TERMINAL_ERROR_CODE_KEY = "terminalErrorCode"
