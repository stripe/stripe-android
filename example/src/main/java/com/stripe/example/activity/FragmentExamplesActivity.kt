package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.stripe.android.ApiResultCallback
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
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
import com.stripe.example.module.BackendApiFactory
import com.stripe.example.service.BackendApi
import com.stripe.example.service.ExampleEphemeralKeyProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.HashMap

class FragmentExamplesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.fragments_example_layout)

        setTitle(R.string.launch_payment_session_from_fragment)

        val newFragment = LauncherFragment()

        val ft = supportFragmentManager.beginTransaction()
        ft.add(R.id.root, newFragment, LauncherFragment::class.java.simpleName)
            .commit()
    }

    class LauncherFragment : Fragment() {

        private val compositeDisposable = CompositeDisposable()

        private lateinit var stripe: Stripe
        private lateinit var paymentSession: PaymentSession
        private lateinit var backendApi: BackendApi

        private lateinit var progressBar: ProgressBar
        private lateinit var launchPaymentSessionButton: Button
        private lateinit var launchPaymentAuthButton: Button
        private lateinit var launchSetupAuthButton: Button
        private lateinit var statusTextView: TextView

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            backendApi = BackendApiFactory(requireContext()).create()
            stripe = Stripe(
                requireContext(),
                PaymentConfiguration.getInstance(requireContext()).publishableKey
            )
            paymentSession = createPaymentSession(createCustomerSession())

            val rootView = requireNotNull(view)
            statusTextView = rootView.findViewById(R.id.tv_status)
            progressBar = rootView.findViewById(R.id.progress_bar)

            launchPaymentSessionButton = rootView.findViewById(R.id.launch_payment_session)
            launchPaymentSessionButton.setOnClickListener {
                paymentSession.presentPaymentMethodSelection()
            }

            launchPaymentAuthButton = rootView.findViewById(R.id.launch_payment_auth)
            launchPaymentAuthButton.setOnClickListener { createPaymentIntent() }

            launchSetupAuthButton = rootView.findViewById(R.id.launch_setup_auth)
            launchSetupAuthButton.setOnClickListener { createSetupIntent() }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return layoutInflater.inflate(R.layout.launch_payment_session_fragment, null)
        }

        override fun onPause() {
            progressBar.visibility = View.INVISIBLE
            super.onPause()
        }

        override fun onDestroy() {
            compositeDisposable.dispose()
            super.onDestroy()
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            progressBar.visibility = View.VISIBLE

            val isPaymentSessionResult =
                data != null && paymentSession.handlePaymentData(requestCode, resultCode, data)
            if (isPaymentSessionResult) {
                Toast.makeText(
                    requireActivity(),
                    "Received PaymentSession result",
                    Toast.LENGTH_SHORT)
                    .show()
                return
            }

            val isPaymentResult = stripe.onPaymentResult(
                requestCode,
                data,
                PaymentAuthResultListener(this)
            )
            if (isPaymentResult) {
                return
            }

            val isSetupResult = stripe.onSetupResult(
                requestCode,
                data,
                SetupAuthResultListener(this)
            )
            if (isSetupResult) {
                return
            }
        }

        private fun createPaymentIntent() {
            compositeDisposable.add(
                backendApi.createPaymentIntent(createPaymentIntentParams())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        progressBar.visibility = View.VISIBLE
                        launchPaymentAuthButton.isEnabled = false
                        statusTextView.setText(R.string.creating_payment_intent)
                    }
                    .subscribe { onCreatePaymentIntentResponse(it) })
        }

        private fun createSetupIntent() {
            compositeDisposable.add(
                backendApi.createSetupIntent(hashMapOf("country" to "us"))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        progressBar.visibility = View.VISIBLE
                        launchSetupAuthButton.isEnabled = false
                        statusTextView.setText(R.string.creating_setup_intent)
                    }
                    .subscribe { onCreateSetupIntentResponse(it) })
        }

        private fun onAuthComplete() {
            launchPaymentAuthButton.isEnabled = true
            progressBar.visibility = View.INVISIBLE
        }

        private fun onCreatePaymentIntentResponse(responseBody: ResponseBody) {
            try {
                val responseData = JSONObject(responseBody.string())
                statusTextView.append("\n\n" + getString(R.string.payment_intent_status,
                    responseData.getString("status")))
                val secret = responseData.getString("secret")
                confirmPaymentIntent(secret)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        private fun onCreateSetupIntentResponse(responseBody: ResponseBody) {
            try {
                val responseData = JSONObject(responseBody.string())
                statusTextView.append("\n\n" + getString(R.string.payment_intent_status,
                    responseData.getString("status")))
                val secret = responseData.getString("secret")
                confirmSetupIntent(secret)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        private fun createPaymentIntentParams(): HashMap<String, Any> {
            return hashMapOf(
                "payment_method_types[]" to "card",
                "amount" to 1000,
                "country" to "us"
            )
        }

        private fun confirmPaymentIntent(paymentIntentClientSecret: String) {
            statusTextView.append("\n\nStarting payment authentication")
            stripe.confirmPayment(this,
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    PAYMENT_METHOD_3DS2_REQUIRED,
                    paymentIntentClientSecret,
                    RETURN_URL))
        }

        private fun confirmSetupIntent(setupIntentClientSecret: String) {
            statusTextView.append("\n\nStarting payment authentication")
            stripe.confirmSetupIntent(this,
                ConfirmSetupIntentParams.create(
                    PAYMENT_METHOD_3DS2_REQUIRED,
                    setupIntentClientSecret,
                    RETURN_URL
                )
            )
        }

        private fun createPaymentSession(customerSession: CustomerSession): PaymentSession {
            val paymentSession = PaymentSession(this)
            val paymentSessionInitialized = paymentSession.init(
                object : PaymentSession.PaymentSessionListener {
                    override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
                    }

                    override fun onError(errorCode: Int, errorMessage: String) {
                    }

                    override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
                        customerSession.retrieveCurrentCustomer(
                            object : CustomerSession.CustomerRetrievalListener {
                                override fun onCustomerRetrieved(customer: Customer) {
                                    launchPaymentSessionButton.isEnabled = true
                                }

                                override fun onError(
                                    errorCode: Int,
                                    errorMessage: String,
                                    stripeError: StripeError?
                                ) {
                                }
                            })
                    }
                },
                PaymentSessionConfig.Builder()
                    .setHiddenShippingInfoFields(
                        ShippingInfoWidget.CustomizableShippingField.PHONE_FIELD,
                        ShippingInfoWidget.CustomizableShippingField.CITY_FIELD
                    )
                    .build())
            if (paymentSessionInitialized) {
                paymentSession.setCartTotal(2000L)
            }

            return paymentSession
        }

        private fun createCustomerSession(): CustomerSession {
            CustomerSession.initCustomerSession(
                requireContext(),
                ExampleEphemeralKeyProvider(requireContext())
            )
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

        private class PaymentAuthResultListener internal constructor(
            fragment: LauncherFragment
        ) : ApiResultCallback<PaymentIntentResult> {
            private val fragmentRef: WeakReference<LauncherFragment> = WeakReference(fragment)

            override fun onSuccess(result: PaymentIntentResult) {
                val fragment = fragmentRef.get() ?: return

                val paymentIntent = result.intent
                fragment.statusTextView.append("\n\n" +
                    "Auth outcome: " + result.outcome + "\n\n" +
                    fragment.getString(R.string.payment_intent_status,
                        paymentIntent.status))
                fragment.onAuthComplete()
            }

            override fun onError(e: Exception) {
                val fragment = fragmentRef.get() ?: return

                fragment.statusTextView.append("\n\nException: " + e.message)
                fragment.onAuthComplete()
            }
        }

        private class SetupAuthResultListener internal constructor(
            fragment: LauncherFragment
        ) : ApiResultCallback<SetupIntentResult> {
            private val fragmentRef: WeakReference<LauncherFragment> = WeakReference(fragment)

            override fun onSuccess(result: SetupIntentResult) {
                val fragment = fragmentRef.get() ?: return

                val paymentIntent = result.intent
                fragment.statusTextView.append("\n\n" +
                    "Outcome: " + result.outcome + "\n\n" +
                    fragment.getString(R.string.payment_intent_status,
                        paymentIntent.status))
                fragment.onAuthComplete()
            }

            override fun onError(e: Exception) {
                val fragment = fragmentRef.get() ?: return

                fragment.statusTextView.append("\n\nException: " + e.message)
                fragment.onAuthComplete()
            }
        }

        private companion object {
            private const val PAYMENT_METHOD_3DS2_REQUIRED = "pm_card_threeDSecure2Required"
            private const val RETURN_URL = "stripe://payment_auth"
        }
    }
}
