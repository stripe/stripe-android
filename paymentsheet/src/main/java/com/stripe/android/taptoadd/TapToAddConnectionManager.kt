package com.stripe.android.taptoadd

import android.annotation.SuppressLint
import android.content.Context
import com.stripe.android.paymentelement.CreateTerminalSessionCallback
import com.stripe.android.paymentelement.CreateTerminalSessionResult
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal interface TapToAddConnectionManager {
    val isSupported: Boolean
    val isConnected: Boolean

    fun startConnecting()

    suspend fun awaitConnection(): Result<Boolean>

    fun stopConnecting()

    companion object {
        @OptIn(TapToAddPreview::class)
        fun create(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            applicationContext: Context,
            workContext: CoroutineContext,
            createTerminalSessionCallbackProvider: Provider<CreateTerminalSessionCallback>?
        ): TapToAddConnectionManager {
            return if (isStripeTerminalSdkAvailable()) {
                DefaultTapToAddConnectionManager(
                    applicationContext,
                    workContext,
                    createTerminalSessionCallbackProvider,
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
    createTerminalSessionCallbackProvider: Provider<CreateTerminalSessionCallback>?
) : TapToAddConnectionManager {
    private var readerDiscoveryTask: Cancelable? = null
    private var connectionTask: CompletableDeferred<Boolean>? = null

    private val discoveryConfiguration = DiscoveryConfiguration.TapToPayDiscoveryConfiguration()
    private val workScope = CoroutineScope(workContext)

    override val isSupported: Boolean
        get()  {
            return terminal().supportsReadersOfType(
                deviceType = DeviceType.TAP_TO_PAY_DEVICE,
                discoveryConfiguration = discoveryConfiguration,
            ).isSupported
        }

    override val isConnected: Boolean
        get() = terminal().connectedReader != null

    init {
        if (!Terminal.isInitialized()) {
            Terminal.initTerminal(
                context = applicationContext,
                tokenProvider = object : ConnectionTokenProvider {
                    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
                        workScope.launch {
                            val terminalSessionResult = createTerminalSessionCallbackProvider?.get()
                                ?.createTerminalSession()
                                ?: CreateTerminalSessionResult.Failure(
                                    cause = IllegalStateException("No callback available!")
                                )

                            when (terminalSessionResult) {
                                is CreateTerminalSessionResult.Success -> {
                                    callback.onSuccess(terminalSessionResult.connectionToken)
                                }
                                is CreateTerminalSessionResult.Failure -> {
                                    callback.onFailure(
                                        ConnectionTokenException(
                                            cause = terminalSessionResult.cause,
                                            message = terminalSessionResult.displayMessage.orEmpty(),
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                listener = object : TerminalListener {

                }
            )
        }
    }

    override fun startConnecting() {
        if (!isSupported || isConnected || connectionTask?.isActive == true) {
            return
        }

        connectionTask = CompletableDeferred()

        workScope.launch {
            try {
                @SuppressLint("MissingPermission")
                readerDiscoveryTask = terminal().discoverReaders(
                    config = discoveryConfiguration,
                    discoveryListener = object : DiscoveryListener {
                        override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                            /*
                             * If we are still connected, we should not try reconnecting as we always expect the reader
                             * to remain the same in Tap to Pay mode.
                             */
                            if (isConnected) {
                                return
                            }

                            val reader = readers.firstOrNull() ?: return

                            terminal().connectReader(
                                reader = reader,
                                config = ConnectionConfiguration.TapToPayConnectionConfiguration(
                                    locationId = "<Location_Id>",
                                    autoReconnectOnUnexpectedDisconnect = true,
                                    tapToPayReaderListener = object : TapToPayReaderListener {

                                    }
                                ),
                                connectionCallback = object : ReaderCallback {
                                    override fun onFailure(e: TerminalException) {
                                        connectionTask?.completeExceptionally(e)
                                        connectionTask = null
                                    }

                                    override fun onSuccess(reader: Reader) {
                                        connectionTask?.complete(true)
                                        connectionTask = null
                                    }
                                }
                            )
                        }
                    },
                    callback = object : Callback {
                        override fun onFailure(e: TerminalException) {
                            connectionTask?.completeExceptionally(e)
                            connectionTask = null
                        }

                        override fun onSuccess() {
                            // No-op
                        }
                    }
                )
            } catch (exception: SecurityException) {
                // Should never reach here!
            }
        }
    }

    override suspend fun awaitConnection(): Result<Boolean> {
        return runCatching {
            connectionTask?.await() ?: false
        }
    }

    override fun stopConnecting() {
        cancelDiscovery()
    }

    private fun terminal() = Terminal.getInstance()

    private fun cancelDiscovery() {
        readerDiscoveryTask?.run {
            if (!isCompleted) {
                cancel(
                    object : Callback {
                        override fun onSuccess() {
                            readerDiscoveryTask = null
                        }

                        override fun onFailure(e: TerminalException) {
                            // Should never occur
                        }
                    }
                )
            }
        }
    }
}

internal class UnsupportedTapToAddConnectionManager : TapToAddConnectionManager {
    override val isSupported: Boolean = false
    override val isConnected: Boolean = false

    override fun startConnecting() {
        // No-op
    }

    override suspend fun awaitConnection(): Result<Boolean> {
        return Result.success(false)
    }

    override fun stopConnecting() {
        // No-op
    }
}
