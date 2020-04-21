package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.BecsDebitWidget
import com.stripe.example.R
import com.stripe.example.databinding.BecsDebitActivityBinding
import com.stripe.example.module.BackendApiFactory
import com.stripe.example.service.BackendApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference

class BecsDebitPaymentMethodActivity : AppCompatActivity() {
    private val stripe: Stripe by lazy {
        Stripe(this, PaymentConfiguration.getInstance(this).publishableKey)
    }
    private val viewBinding: BecsDebitActivityBinding by lazy {
        BecsDebitActivityBinding.inflate(layoutInflater)
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }
    private val compositeSubscription = CompositeDisposable()
    private val backendApi: BackendApi by lazy {
        BackendApiFactory(this).create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.element.validParamsCallback = object : BecsDebitWidget.ValidParamsCallback {
            override fun onInputChanged(isValid: Boolean) {
                viewBinding.payButton.isEnabled = isValid
                viewBinding.saveButton.isEnabled = isValid
            }
        }

        viewBinding.payButton.setOnClickListener {
            viewBinding.element.params?.let { params ->
                keyboardController.hide()

                createPaymentIntent(params)
            }
        }

        viewBinding.saveButton.setOnClickListener {
            viewBinding.element.params?.let { params ->
                keyboardController.hide()

                createSetupIntent(params)
            }
        }
    }

    override fun onDestroy() {
        compositeSubscription.dispose()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val isPaymentIntentConfirmationResult = stripe.onPaymentResult(
            requestCode, data, PaymentIntentResultCallback(this)
        )
        if (!isPaymentIntentConfirmationResult) {
            stripe.onSetupResult(requestCode, data, SetupIntentResultCallback(this))
        }
    }

    private fun createPaymentIntent(params: PaymentMethodCreateParams) {
        compositeSubscription.add(
            backendApi
                .createPaymentIntent(
                    mapOf(
                        "amount" to 1000,
                        "country" to "au"
                    ).toMutableMap()
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    disableUi()
                    viewBinding.status.setText(R.string.creating_payment_intent)
                }
                .subscribe(
                    { handleCreatePaymentIntentResponse(it, params) },
                    { onError(it) }
                )
        )
    }

    private fun createSetupIntent(params: PaymentMethodCreateParams) {
        compositeSubscription.add(
            backendApi
                .createSetupIntent(
                    mapOf(
                        "country" to "au"
                    ).toMutableMap()
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    disableUi()
                    viewBinding.status.setText(R.string.creating_setup_intent)
                }
                .subscribe(
                    { onCreateSetupIntentResponse(it, params) },
                    { onError(it) }
                )
        )
    }

    private fun disableUi() {
        viewBinding.payButton.isEnabled = false
        viewBinding.saveButton.isEnabled = false
    }

    private fun handleCreatePaymentIntentResponse(
        responseBody: ResponseBody,
        params: PaymentMethodCreateParams
    ) {
        try {
            val responseData = JSONObject(responseBody.string())
            viewBinding.status.append("\n\n" + getString(R.string.payment_intent_status,
                responseData.getString("status")))
            val secret = responseData.getString("secret")
            confirmPaymentIntent(secret, params)
        } catch (e: IOException) {
            onError(e)
        } catch (e: JSONException) {
            onError(e)
        }
    }

    private fun onCreateSetupIntentResponse(
        responseBody: ResponseBody,
        params: PaymentMethodCreateParams
    ) {
        try {
            val responseData = JSONObject(responseBody.string())
            viewBinding.status.append("\n\n" + getString(R.string.setup_intent_status,
                responseData.getString("status")))
            val secret = responseData.getString("secret")
            confirmSetupIntent(secret, params)
        } catch (e: IOException) {
            onError(e)
        } catch (e: JSONException) {
            onError(e)
        }
    }

    private fun confirmPaymentIntent(
        paymentIntentClientSecret: String,
        params: PaymentMethodCreateParams
    ) {
        viewBinding.status.append("\n\nConfirming PaymentIntent")
        stripe.confirmPayment(
            this,
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = params,
                clientSecret = paymentIntentClientSecret
            )
        )
    }

    private fun confirmSetupIntent(
        setupIntentClientSecret: String,
        params: PaymentMethodCreateParams
    ) {
        viewBinding.status.append("\n\nConfirming SetupIntent")
        stripe.confirmSetupIntent(
            this,
            ConfirmSetupIntentParams.create(
                paymentMethodCreateParams = params,
                clientSecret = setupIntentClientSecret
            )
        )
    }

    private fun onPaymentConfirmSuccess(result: PaymentIntentResult) {
        val paymentIntent = result.intent
        viewBinding.status.append("\n\n" +
            "PaymentIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.payment_intent_status, paymentIntent.status)
        )
        viewBinding.payButton.isEnabled = true
        viewBinding.saveButton.isEnabled = true
    }

    private fun onSetupConfirmSuccess(result: SetupIntentResult) {
        val paymentIntent = result.intent
        viewBinding.status.append("\n\n" +
            "SetupIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.payment_intent_status, paymentIntent.status)
        )
        viewBinding.payButton.isEnabled = true
        viewBinding.saveButton.isEnabled = true
    }

    private fun onError(e: Throwable) {
        viewBinding.status.append("\n\nException: " + e.message)
        viewBinding.payButton.isEnabled = true
        viewBinding.saveButton.isEnabled = true
    }

    private class PaymentIntentResultCallback(
        activity: BecsDebitPaymentMethodActivity
    ) : ApiResultCallback<PaymentIntentResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: PaymentIntentResult) {
            activityRef.get()?.onPaymentConfirmSuccess(result)
        }

        override fun onError(e: Exception) {
            activityRef.get()?.onError(e)
        }
    }

    private class SetupIntentResultCallback(
        activity: BecsDebitPaymentMethodActivity
    ) : ApiResultCallback<SetupIntentResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: SetupIntentResult) {
            activityRef.get()?.onSetupConfirmSuccess(result)
        }

        override fun onError(e: Exception) {
            activityRef.get()?.onError(e)
        }
    }
}
