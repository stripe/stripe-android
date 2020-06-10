package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiResultCallback
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentIntentResult
import com.stripe.android.PaymentSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.StripeError
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.Customer
import com.stripe.android.view.ShippingInfoWidget
import com.stripe.example.R
import com.stripe.example.StripeFactory
import com.stripe.example.databinding.LaunchPaymentSessionFragmentBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class FragmentExamplesFragment : Fragment() {
    private val viewBinding: LaunchPaymentSessionFragmentBinding by lazy {
        LaunchPaymentSessionFragmentBinding.inflate(layoutInflater)
    }

    private val viewModel: FragmentExamplesViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        )[FragmentExamplesViewModel::class.java]
    }

    private val stripe: Stripe by lazy {
        StripeFactory(requireContext()).create()
    }

    private val paymentSession: PaymentSession by lazy {
        PaymentSession(
            fragment = this,
            config = PaymentSessionConfig.Builder()
                .setShippingMethodsRequired(false)
                .setHiddenShippingInfoFields(
                    ShippingInfoWidget.CustomizableShippingField.PHONE_FIELD,
                    ShippingInfoWidget.CustomizableShippingField.CITY_FIELD
                )
                .build()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initPaymentSession(createCustomerSession())
        viewBinding.launchPaymentSession.setOnClickListener {
            paymentSession.presentPaymentMethodSelection()
        }
        viewBinding.launchPaymentAuth.setOnClickListener { createPaymentIntent() }
        viewBinding.launchSetupAuth.setOnClickListener { createSetupIntent() }

        viewModel.paymentIntentResultLiveData.observe(
            viewLifecycleOwner,
            Observer { result ->
                result.fold(
                    onSuccess = {
                        val status = getString(
                            R.string.payment_intent_status,
                            it.intent.status
                        )
                        updateStatus("$status; Outcome: ${it.outcome}")
                    },
                    onFailure = ::onError
                )
                onConfirmationComplete()
            }
        )

        viewModel.setupIntentResultLiveData.observe(
            viewLifecycleOwner,
            Observer { result ->
                result.fold(
                    onSuccess = {
                        val paymentIntent = it.intent
                        val status = getString(
                            R.string.setup_intent_status,
                            paymentIntent.status
                        )
                        updateStatus("$status; Outcome: ${it.outcome}")
                    },
                    onFailure = ::onError
                )
                onConfirmationComplete()
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return viewBinding.root
    }

    override fun onPause() {
        viewBinding.progressBar.visibility = View.INVISIBLE
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewBinding.progressBar.visibility = View.VISIBLE

        val isPaymentSessionResult =
            data != null && paymentSession.handlePaymentData(requestCode, resultCode, data)
        if (isPaymentSessionResult) {
            Toast.makeText(
                requireActivity(),
                "Received PaymentSession result",
                Toast.LENGTH_SHORT
            ).show()
            viewBinding.progressBar.visibility = View.INVISIBLE
            return
        }

        val isPaymentResult = stripe.onPaymentResult(
            requestCode,
            data,
            callback = object : ApiResultCallback<PaymentIntentResult> {
                override fun onSuccess(result: PaymentIntentResult) {
                    viewModel.paymentIntentResultLiveData.value = Result.success(result)
                }

                override fun onError(e: Exception) {
                    viewModel.paymentIntentResultLiveData.value = Result.failure(e)
                }
            }
        )
        if (isPaymentResult) {
            return
        }

        val isSetupResult = stripe.onSetupResult(
            requestCode,
            data,
            callback = object : ApiResultCallback<SetupIntentResult> {
                override fun onSuccess(result: SetupIntentResult) {
                    viewModel.setupIntentResultLiveData.value = Result.success(result)
                }

                override fun onError(e: Exception) {
                    viewModel.setupIntentResultLiveData.value = Result.failure(e)
                }
            }
        )
        if (isSetupResult) {
            return
        }
    }

    private fun createPaymentIntent() {
        viewBinding.progressBar.visibility = View.VISIBLE
        viewBinding.launchPaymentAuth.isEnabled = false
        viewBinding.status.setText(R.string.creating_payment_intent)

        viewModel.createPaymentIntent().observe(
            viewLifecycleOwner,
            Observer {
                it.fold(
                    onSuccess = ::onCreatePaymentIntentResponse,
                    onFailure = ::onError
                )
            }
        )
    }

    private fun createSetupIntent() {
        viewBinding.progressBar.visibility = View.VISIBLE
        viewBinding.launchSetupAuth.isEnabled = false
        viewBinding.status.setText(R.string.creating_setup_intent)

        viewModel.createSetupIntent().observe(
            viewLifecycleOwner,
            Observer {
                it.fold(
                    onSuccess = ::onCreateSetupIntentResponse,
                    onFailure = ::onError
                )
            }
        )
    }

    private fun onConfirmationComplete() {
        viewBinding.launchPaymentAuth.isEnabled = true
        viewBinding.progressBar.visibility = View.INVISIBLE
    }

    private fun onCreatePaymentIntentResponse(response: JSONObject) {
        try {
            updateStatus(
                getString(
                    R.string.payment_intent_status,
                    response.getString("status")
                )
            )
            confirmPaymentIntent(response.getString("secret"))
        } catch (e: IOException) {
            onError(e)
        } catch (e: JSONException) {
            onError(e)
        }
    }

    private fun onCreateSetupIntentResponse(response: JSONObject) {
        try {
            updateStatus(
                getString(
                    R.string.setup_intent_status,
                    response.getString("status")
                )
            )
            confirmSetupIntent(response.getString("secret"))
        } catch (e: IOException) {
            onError(e)
        } catch (e: JSONException) {
            onError(e)
        }
    }

    private fun confirmPaymentIntent(paymentIntentClientSecret: String) {
        updateStatus("Starting payment authentication")
        stripe.confirmPayment(
            this,
            ConfirmPaymentIntentParams.createWithPaymentMethodId(
                PAYMENT_METHOD_3DS2_REQUIRED,
                paymentIntentClientSecret,
                RETURN_URL
            )
        )
    }

    private fun confirmSetupIntent(setupIntentClientSecret: String) {
        updateStatus("Starting payment setup authentication")
        stripe.confirmSetupIntent(
            this,
            ConfirmSetupIntentParams.create(
                PAYMENT_METHOD_3DS2_REQUIRED,
                setupIntentClientSecret,
                RETURN_URL
            )
        )
    }

    private fun initPaymentSession(customerSession: CustomerSession) {
        paymentSession.init(
            object : PaymentSession.PaymentSessionListener {
                override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                }

                override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
                    customerSession.retrieveCurrentCustomer(
                        object : CustomerSession.CustomerRetrievalListener {
                            override fun onCustomerRetrieved(customer: Customer) {
                                viewBinding.launchPaymentSession.isEnabled = true
                            }

                            override fun onError(
                                errorCode: Int,
                                errorMessage: String,
                                stripeError: StripeError?
                            ) {
                            }
                        })
                }
            }
        )
        paymentSession.setCartTotal(2000L)
    }

    private fun createCustomerSession(): CustomerSession {
        val customerSession = CustomerSession.getInstance()
        customerSession.retrieveCurrentCustomer(
            object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {}

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                }
            }
        )
        return customerSession
    }

    private fun onError(throwable: Throwable) {
        updateStatus(throwable.message.orEmpty())
    }

    private fun updateStatus(message: String) {
        viewBinding.status.append("\n\n\n$message")
    }

    private companion object {
        private const val PAYMENT_METHOD_3DS2_REQUIRED = "pm_card_threeDSecure2Required"
        private const val RETURN_URL = "stripe://payment_auth"
    }
}
