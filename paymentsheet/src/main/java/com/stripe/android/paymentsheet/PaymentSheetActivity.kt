package com.stripe.android.paymentsheet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.Stripe
import com.stripe.android.common.ui.BottomSheet
import com.stripe.android.common.ui.rememberBottomSheetState
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.uicore.StripeTheme
import com.stripe.model.terminal.ConnectionToken
import com.stripe.net.RequestOptions
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.flow.filterNotNull
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.io.IOException
import java.security.InvalidParameterException

internal class PaymentSheetActivity : BaseSheetActivity<PaymentSheetResult>() {

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PaymentSheetViewModel.Factory {
        requireNotNull(starterArgs)
    }

    override val viewModel: PaymentSheetViewModel by viewModels { viewModelFactory }

    private val starterArgs: PaymentSheetContractV2.Args? by lazy {
        PaymentSheetContractV2.Args.fromIntent(intent)
    }

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        ::onPermissionResult
    )

    private fun onPermissionResult(result: Map<String, Boolean>) {
        val deniedPermissions: List<String> = result
            .filter { !it.value }
            .map { it.key }

//        // If we receive a response to our permission check, initialize
//        if (deniedPermissions.isEmpty() && !Terminal.isInitialized() && verifyGpsEnabled()) {
//            initialize()
//        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val validationResult = initializeArgs()
        super.onCreate(savedInstanceState)

        val deniedPermissions = mutableListOf<String>().apply {
//                    if (!isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }.toTypedArray()

        if (deniedPermissions.isNotEmpty()) {
            // If we don't have them yet, request them before doing anything else
            requestPermissionLauncher.launch(deniedPermissions)
        }
//                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))

        if (!Terminal.isInitialized()) {
            Terminal.initTerminal(
                context = this,
                tokenProvider = TokenProvider(viewModel = viewModel),
                listener = TerminalEventListener()
            )
        }
//        try {
//
//        } catch (e: IllegalStateException) {
//            println("caught init duplicate call")
//        }


        val validatedArgs = validationResult.getOrNull()
        if (validatedArgs == null) {
            finishWithError(error = validationResult.exceptionOrNull())
            return
        }

        viewModel.registerFromActivity(
            activityResultCaller = this,
            lifecycleOwner = this,
        )

        viewModel.setupGooglePay(
            lifecycleScope,
            registerForActivityResult(
                GooglePayPaymentMethodLauncherContractV2(),
                viewModel::onGooglePayResult
            )
        )

//        Terminal.getInstance().discoverReaders(
//            DiscoveryConfiguration(
//                discoveryMethod = DiscoveryMethod.INTERNET,
//            ),
//            object : DiscoveryListener {
//                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
//                    TODO("Not yet implemented")
//                }
//
//            },
//            object : com.stripe.stripeterminal.external.callable.Callback {
//                override fun onFailure(e: TerminalException) {
//                    e.printStackTrace()
//                }
//
//                override fun onSuccess() {
//                    println("Finished discovering readers")
//                }
//
//            }
//        )

        setContent {
            StripeTheme {
                val isProcessing by viewModel.processing.collectAsState()

                val bottomSheetState = rememberBottomSheetState(
                    confirmValueChange = { !isProcessing },
                )

                LaunchedEffect(Unit) {
                    viewModel.paymentSheetResult.filterNotNull().collect { sheetResult ->
                        setActivityResult(sheetResult)
                        bottomSheetState.hide()
                        finish()
                    }
                }

                BottomSheet(
                    state = bottomSheetState,
                    onDismissed = viewModel::onUserCancel,
                ) {
                    PaymentSheetScreen(viewModel)
                }
            }
        }
    }

    private fun initializeArgs(): Result<PaymentSheetContractV2.Args?> {
        val starterArgs = this.starterArgs

        val result = if (starterArgs == null) {
            Result.failure(defaultInitializationError())
        } else {
            try {
                starterArgs.initializationMode.validate()
                starterArgs.config?.validate()
                starterArgs.config?.appearance?.parseAppearance()
                Result.success(starterArgs)
            } catch (e: InvalidParameterException) {
                Result.failure(e)
            }
        }

        earlyExitDueToIllegalState = result.isFailure
        return result
    }

    override fun setActivityResult(result: PaymentSheetResult) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(PaymentSheetContractV2.Result(result).toBundle())
        )
    }

    private fun finishWithError(error: Throwable?) {
        val e = error ?: defaultInitializationError()
        setActivityResult(PaymentSheetResult.Failed(e))
        finish()
    }

    private fun defaultInitializationError(): IllegalArgumentException {
        return IllegalArgumentException("PaymentSheet started without arguments.")
    }

    class TokenProvider(
        val viewModel: PaymentSheetViewModel,
    ) : ConnectionTokenProvider {

        override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
            try {
                val token = ApiClient.createConnectionToken()
                viewModel.connectionToken = token
                callback.onSuccess(token)
            } catch (e: ConnectionTokenException) {
                callback.onFailure(e)
            }
        }
    }

    /**
     * The 'BackendService' interface handles the two simple calls we need to make to our backend.
     * This represents YOUR backend, so feel free to change the routes accordingly.
     */
    interface BackendService {

        /**
         * Get a connection token string from the backend
         */
        @POST("connection_token")
        fun getConnectionToken(): Call<ConnectionToken>

        /**
         * Create a payment intent on the backend
         */
        @FormUrlEncoded
        @POST("create_payment_intent")
        fun createPaymentIntent(
            @Field("amount") amount: Int,
            @Field("currency") currency: String
        ): Call<ServerPaymentIntent>

        /**
         * Capture a specific payment intent on our backend
         */
        @FormUrlEncoded
        @POST("capture_payment_intent")
        fun capturePaymentIntent(@Field("payment_intent_id") id: String): Call<Void>
    }

    object ApiClient {

        const val BACKEND_URL = "http://localhost:4242"

        private val client = OkHttpClient.Builder()
//            .addNetworkInterceptor(StethoInterceptor())
            .build()
        private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        private val service: BackendService = retrofit.create(BackendService::class.java)

        @Throws(ConnectionTokenException::class)
        internal fun createConnectionToken(): String {
            try {
//                val result = service.getConnectionToken().execute()
//                if (result.isSuccessful && result.body() != null) {
//                    return result.body()!!.secret
//                } else {
//                    throw ConnectionTokenException("Creating connection token failed")
//                }
                com.stripe.Stripe.apiKey = "rk_test_51IpzYoKRSL86NWDjhjLLncF9aGxfWwedRtSAk4eo1Zx6UYwN98l6FLLvMMd9fGZRgQKwwLOWKOJLqrSx5RoTXbMs009mJxj1ll"

                val requestOptionsBuilder = RequestOptions.builder()

//                if (directChargeAccountId.isNotBlank()) {
//                    requestOptionsBuilder.stripeAccount = directChargeAccountId
//                }


                val connectionToken = com.stripe.model.terminal.ConnectionToken.create(mapOf(), requestOptionsBuilder.build())
                println("asdf: ${connectionToken.secret}")
                return connectionToken.secret
            } catch (e: IOException) {
                throw ConnectionTokenException("Creating connection token failed", e)
            }
        }

        @Throws(Exception::class)
        internal fun createPaymentIntent(
            amount: Int, currency: String, callback: Callback<ServerPaymentIntent>
        ) {
            service.createPaymentIntent(amount, currency).enqueue(callback)
        }

        internal fun capturePaymentIntent(id: String) {
            service.capturePaymentIntent(id).execute()
        }
    }

    class TerminalEventListener : TerminalListener {
        override fun onUnexpectedReaderDisconnect(reader: Reader) {
            // Show UI that your reader disconnected
        }
    }

    // A one-field data class used to handle the connection token response from our backend
    data class ConnectionToken(val secret: String)

    data class ServerPaymentIntent(val intent: String, val secret: String)
}
