package com.stripe.example.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stripe.Stripe
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.example.Settings
import com.stripe.example.module.StripeIntentViewModel
import com.stripe.model.terminal.ConnectionToken
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Base class for Activity's that wish to create and confirm payment methods.
 * Subclasses should observe on the [StripeIntentViewModel]'s LiveData properties
 * in order to display state of the interaction.
 */
abstract class StripeIntentActivity : AppCompatActivity() {
    internal val viewModel: StripeIntentViewModel by viewModels()
    private val stripeAccountId: String? by lazy {
        Settings(this).stripeAccountId
    }
    private val stripeSecretKey: String? by lazy {
        Settings(this).stripeSecretKey
    }

    private lateinit var paymentLauncher: PaymentLauncher

    private var terminalInstance: Terminal? = null
    private var availableCotsReader: Reader? = null
    private var activeSecret: String? = null

    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    // Register the permissions callback to handles the response to the system permissions dialog.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        ::onPermissionResult
    )

    private fun initializeTerminal() {
        if (!Terminal.isInitialized()) {
            Terminal.initTerminal(
                applicationContext,
                LogLevel.VERBOSE,
                object : ConnectionTokenProvider {
                    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
                        Stripe.apiKey = stripeSecretKey
                        callback.onSuccess(ConnectionToken.create(mapOf()).secret)
                    }
                },
                object : TerminalListener {
                    override fun onUnexpectedReaderDisconnect(reader: Reader) {
                    }
                }
            )

            terminalInstance = Terminal.getInstance()
            terminalInstance?.discoverReaders(
                DiscoveryConfiguration(
                    timeout = 100,
                    discoveryMethod = DiscoveryMethod.LOCAL_MOBILE,
                    isSimulated = false,
                ),
                object : DiscoveryListener {
                    override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                        Log.i("StripeIntentActivity", "Terminal initialized")
                        availableCotsReader = readers[0]
                    }
                },
                object : Callback {
                    override fun onFailure(e: TerminalException) { }

                    override fun onSuccess() { }
                }
            )
        }
    }

    /**
     * Receive the result of our permissions check, and initialize if we can
     */
    private fun onPermissionResult(result: Map<String, Boolean>) {
        // TODO: Location services
//        val deniedPermissions: Map<PermissionChecker.PermissionResult, List<String>> = result
//            .filter { !it.value }
//            .map { it.key }
//            .groupBy { permission ->
//                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) DENIED
//                else DENIED_PERMANENTLY
//            }

//        if (deniedPermissions.isNotEmpty()) {
//            deniedPermissions[DENIED]?.let {
//                Toast.makeText(
//                    this,
//                    "Please enable required permissions",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//            deniedPermissions[DENIED_PERMANENTLY]?.let {
//                Toast.makeText(
//                    this,
//                    "Please enable required permissions via app settings",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
        // TODO: Check for lack of permission
        initializeTerminal()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentLauncher =
            PaymentLauncher.create(
                this,
                PaymentConfiguration.getInstance(this).publishableKey,
                stripeAccountId
            ) { paymentResult ->
                if (viewModel.status.value.isNullOrEmpty()) {
                    viewModel.status.value =
                        """
                        Restored from a killed process...

                        Payment authentication completed, getting result
                        """.trimIndent()
                } else {
                    viewModel.status.value += "\n\nPayment authentication completed, getting result"
                }
                viewModel.paymentResultLiveData.postValue(paymentResult)
            }

        viewModel.paymentResultLiveData
            .observe(
                this
            ) {
                when (it) {
                    is PaymentResult.Completed -> {
                        onConfirmSuccess()
                    }
                    is PaymentResult.Canceled -> {
                        onConfirmCanceled()
                    }
                    is PaymentResult.Failed -> {
                        onConfirmError(it)
                    }
                }
            }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initializeTerminal()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN

                )
            )
        }
    }

    protected fun createAndConfirmPaymentIntent(
        country: String,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        supportedPaymentMethods: String? = null,
        shippingDetails: ConfirmPaymentIntentParams.Shipping? = null,
        stripeAccountId: String? = null,
        existingPaymentMethodId: String? = null,
        mandateDataParams: MandateDataParams? = null,
        onPaymentIntentCreated: (String) -> Unit = {}
    ) {
        requireNotNull(paymentMethodCreateParams ?: existingPaymentMethodId)

        keyboardController.hide()
        val v = currentFocus
        if (v is EditText) {
            v.clearFocus()
        }

        viewModel.createPaymentIntent(
            country = country,
            supportedPaymentMethods = supportedPaymentMethods
        ).observe(
            this
        ) { result ->
            result.onSuccess {
                handleCreatePaymentIntentResponse(
                    it,
                    paymentMethodCreateParams,
                    shippingDetails,
                    stripeAccountId,
                    existingPaymentMethodId,
                    mandateDataParams,
                    onPaymentIntentCreated
                )
            }
        }
    }

    protected fun createAndConfirmSetupIntent(
        country: String,
        params: PaymentMethodCreateParams,
        stripeAccountId: String? = null,
        onSetupIntentCreated: (String) -> Unit = {}
    ) {
        keyboardController.hide()

        viewModel.createSetupIntent(country).observe(
            this
        ) { result ->
            result.onSuccess {
                handleCreateSetupIntentResponse(
                    it,
                    params,
                    stripeAccountId,
                    onSetupIntentCreated
                )
            }
        }
    }

    private fun handleCreatePaymentIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams?,
        shippingDetails: ConfirmPaymentIntentParams.Shipping?,
        stripeAccountId: String?,
        existingPaymentMethodId: String?,
        mandateDataParams: MandateDataParams?,
        onPaymentIntentCreated: (String) -> Unit = {}
    ) {
        val secret = responseData.getString("secret")
        activeSecret = secret

        Log.i("StripeIntentActivity", "Payment response: $responseData")

        onPaymentIntentCreated(secret)
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting PaymentIntent confirmation" + (
                stripeAccountId?.let {
                    " for $it"
                } ?: ""
                )
        )
        val confirmPaymentIntentParams = if (existingPaymentMethodId == null) {
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = requireNotNull(params),
                clientSecret = secret,
                shipping = shippingDetails
            )
        } else {
            ConfirmPaymentIntentParams.createWithPaymentMethodId(
                paymentMethodId = existingPaymentMethodId,
                clientSecret = secret,
                mandateData = mandateDataParams
            )
        }
        paymentLauncher.confirm(confirmPaymentIntentParams)
    }

    private fun handleCreatePaymentIntentRecoverableDecline(
        responseData: JSONObject,
        params: PaymentMethodCreateParams?,
        shippingDetails: ConfirmPaymentIntentParams.Shipping?,
        stripeAccountId: String?,
        existingPaymentMethodId: String?,
        mandateDataParams: MandateDataParams?,
        onPaymentIntentCreated: (String) -> Unit = {}
    ) {
        val secret = responseData.getString("secret")
        onPaymentIntentCreated(secret)
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting PaymentIntent confirmation" + (
                stripeAccountId?.let {
                    " for $it"
                } ?: ""
                )
        )
        val confirmPaymentIntentParams = if (existingPaymentMethodId == null) {
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = requireNotNull(params),
                clientSecret = secret,
                shipping = shippingDetails
            )
        } else {
            ConfirmPaymentIntentParams.createWithPaymentMethodId(
                paymentMethodId = existingPaymentMethodId,
                clientSecret = secret,
                mandateData = mandateDataParams
            )
        }
        confirmPaymentIntent(confirmPaymentIntentParams)
    }

    protected fun confirmPaymentIntent(params: ConfirmPaymentIntentParams) {
        paymentLauncher.confirm(params)
    }

    private fun handleCreateSetupIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams,
        stripeAccountId: String?,
        onSetupIntentCreated: (String) -> Unit = {}
    ) {
        val secret = responseData.getString("secret")
        onSetupIntentCreated(secret)
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting SetupIntent confirmation" + (
                stripeAccountId?.let {
                    " for $it"
                } ?: ""
                )
        )
        confirmSetupIntent(
            ConfirmSetupIntentParams.create(
                paymentMethodCreateParams = params,
                clientSecret = secret
            )
        )
    }

    private fun confirmSetupIntent(
        params: ConfirmSetupIntentParams,
    ) {
        paymentLauncher.confirm(params)
    }

    protected open fun onConfirmSuccess() {
        viewModel.status.value += "\n\nPaymentIntent confirmation succeeded\n\n"
        viewModel.inProgress.value = false

        availableCotsReader?.let {
            viewModel.status.value +=
                "\n\nPaymentIntent confirmation failed, attempting TapOnMobile recovery"

            terminalInstance?.connectLocalMobileReader(
                it,
                ConnectionConfiguration.LocalMobileConnectionConfiguration(
                    // Hardcode location for experiment, may need as input
                    "tml_EonXKgW19MKlb8",
                ),
                object : ReaderCallback {
                    override fun onFailure(e: TerminalException) {
                        Log.i("StripeIntentActivity", "Failed to connect")
                    }

                    override fun onSuccess(reader: Reader) {
                        Log.i("StripeIntentActivity", "Connected $reader")
                        val secret = activeSecret ?: ""

                        terminalInstance?.retrievePaymentIntent(
                            secret,
                            retrievePaymentIntentCallback
                        )
                    }
                }
            )
        } ?: run {
            viewModel.status.value += "\n\nPaymentIntent confirmation failed with throwable"

            viewModel.inProgress.value = false
        }
    }

    protected open fun onConfirmCanceled() {
        viewModel.status.value += "\n\nPaymentIntent confirmation cancelled\n\n"
        viewModel.inProgress.value = false
    }

    private val confirmPaymentIntentCallback = object : PaymentIntentCallback {
        override fun onFailure(e: TerminalException) {
            Log.i("StripeIntentActivity", "collectPaymentIntentCallback failed")
        }

        override fun onSuccess(paymentIntent: PaymentIntent) {
            Log.i("StripeIntentActivity", "onSuccess")

            MainScope().launch { onConfirmSuccess() }
        }
    }

    private val collectPaymentIntentCallback = object : PaymentIntentCallback {
        override fun onFailure(e: TerminalException) {
            Log.i("StripeIntentActivity", "collectPaymentIntentCallback failed")
        }

        override fun onSuccess(paymentIntent: PaymentIntent) {
            Log.i("StripeIntentActivity", "collectPaymentIntentCallback onSuccess")

            terminalInstance?.processPayment(paymentIntent, confirmPaymentIntentCallback)
        }
    }

    private val retrievePaymentIntentCallback = object : PaymentIntentCallback {
        override fun onFailure(e: TerminalException) {
            Log.i("StripeIntentActivity", "retrievePaymentIntentCallback failed")
        }

        override fun onSuccess(paymentIntent: PaymentIntent) {
            Log.i("StripeIntentActivity", "retrieved $paymentIntent")
            terminalInstance?.collectPaymentMethod(paymentIntent, collectPaymentIntentCallback)
        }
    }

    protected open fun onConfirmError(failedResult: PaymentResult.Failed) {
        Log.i(
            "StripeIntentActivity",
            "Going to start terminal: $availableCotsReader $terminalInstance"
        )

        availableCotsReader?.let {
            viewModel.status.value +=
                "\n\nPaymentIntent confirmation failed, attempting TapOnMobile recovery"

            terminalInstance?.connectLocalMobileReader(
                it,
                ConnectionConfiguration.LocalMobileConnectionConfiguration(
                    // Hardcode location for experiment, may need as input
                    "tml_EonXKgW19MKlb8",
                ),
                object : ReaderCallback {
                    override fun onFailure(e: TerminalException) {
                        Log.i("StripeIntentActivity", "Failed to connect")
                    }

                    override fun onSuccess(reader: Reader) {
                        Log.i("StripeIntentActivity", "Connected $reader")
                        val secret = activeSecret ?: ""

                        terminalInstance?.retrievePaymentIntent(
                            secret,
                            retrievePaymentIntentCallback
                        )
                    }
                }
            )
        } ?: run {
            viewModel.status.value += "\n\nPaymentIntent confirmation failed with throwable " +
                "${failedResult.throwable} \n\n"

            viewModel.inProgress.value = false
        }
    }
}
