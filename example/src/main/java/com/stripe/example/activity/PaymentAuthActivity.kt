package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.StripeFactory
import com.stripe.example.databinding.PaymentAuthActivityBinding
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

/**
 * An example of creating a PaymentIntent, then confirming it with [Stripe.confirmPayment]
 */
class PaymentAuthActivity : AppCompatActivity() {

    private val viewBinding: PaymentAuthActivityBinding by lazy {
        PaymentAuthActivityBinding.inflate(layoutInflater)
    }

    private val compositeSubscription = CompositeDisposable()
    private val backendApi: BackendApi by lazy {
        BackendApiFactory(this).create()
    }
    private val stripeAccountId: String? by lazy {
        Settings(this).stripeAccountId
    }
    private val stripe: Stripe by lazy {
        StripeFactory(this, stripeAccountId).create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val stripeAccountId = Settings(this).stripeAccountId

        val uiCustomization =
            PaymentAuthConfig.Stripe3ds2UiCustomization.Builder().build()
        PaymentAuthConfig.init(PaymentAuthConfig.Builder()
            .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setTimeout(6)
                .setUiCustomization(uiCustomization)
                .build())
            .build())

        if (savedInstanceState != null) {
            viewBinding.status.text = savedInstanceState.getString(STATE_STATUS)
        }

        viewBinding.buy3ds1Button.setOnClickListener {
            createPaymentIntent(stripeAccountId, AuthType.ThreeDS1)
        }
        viewBinding.buy3ds2Button.setOnClickListener {
            createPaymentIntent(stripeAccountId, AuthType.ThreeDS2)
        }

        viewBinding.setupButton.setOnClickListener { createSetupIntent() }
    }

    private fun confirmPaymentIntent(
        paymentIntentClientSecret: String,
        authType: AuthType
    ) {
        viewBinding.status.append("\n\nStarting payment authentication")
        stripe.confirmPayment(
            this,
            when (authType) {
                AuthType.ThreeDS1 -> create3ds1ConfirmParams(paymentIntentClientSecret)
                AuthType.ThreeDS2 -> create3ds2ConfirmParams(paymentIntentClientSecret)
            }
        )
    }

    private fun confirmSetupIntent(setupIntentClientSecret: String) {
        viewBinding.status.append("\n\nStarting setup intent authentication")
        stripe.confirmSetupIntent(this, create3ds2SetupIntentParams(setupIntentClientSecret))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewBinding.progressBar.visibility = View.VISIBLE
        viewBinding.status.append("\n\nPayment authentication completed, getting result")

        val isPaymentResult =
            stripe.onPaymentResult(requestCode, data, PaymentIntentResultCallback(this))
        if (!isPaymentResult) {
            stripe.onSetupResult(requestCode, data, SetupIntentResultCallback(this))
        }
    }

    override fun onPause() {
        viewBinding.progressBar.visibility = View.INVISIBLE
        super.onPause()
    }

    override fun onDestroy() {
        compositeSubscription.dispose()
        super.onDestroy()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(STATE_STATUS, viewBinding.status.text.toString())
    }

    private fun createPaymentIntent(
        stripeAccountId: String?,
        authType: AuthType
    ) {
        compositeSubscription.add(
            backendApi.createPaymentIntent(
                createPaymentIntentParams(stripeAccountId).toMutableMap()
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    viewBinding.progressBar.visibility = View.VISIBLE
                    viewBinding.buy3ds2Button.isEnabled = false
                    viewBinding.buy3ds1Button.isEnabled = false
                    viewBinding.setupButton.isEnabled = false
                    viewBinding.status.setText(R.string.creating_payment_intent)
                }
                .subscribe(
                    { handleCreatePaymentIntentResponse(it, authType) },
                    { handleError(it) }
                )
        )
    }

    private fun createSetupIntent() {
        compositeSubscription.add(
            backendApi.createSetupIntent(hashMapOf("country" to "us"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    viewBinding.progressBar.visibility = View.VISIBLE
                    viewBinding.buy3ds2Button.isEnabled = false
                    viewBinding.buy3ds1Button.isEnabled = false
                    viewBinding.setupButton.isEnabled = false
                    viewBinding.status.setText(R.string.creating_setup_intent)
                }
                .subscribe(
                    { handleCreateSetupIntentResponse(it) },
                    { handleError(it) }
                )
        )
    }

    private fun handleError(err: Throwable) {
        viewBinding.progressBar.visibility = View.INVISIBLE
        err.printStackTrace()
        viewBinding.status.append("\n\n" + err.message)
    }

    private fun handleCreatePaymentIntentResponse(
        responseBody: ResponseBody,
        authType: AuthType
    ) {
        try {
            val responseData = JSONObject(responseBody.string())
            viewBinding.status.append("\n\n" + getString(R.string.payment_intent_status,
                responseData.getString("status")))
            val secret = responseData.getString("secret")
            confirmPaymentIntent(secret, authType)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun handleCreateSetupIntentResponse(responseBody: ResponseBody) {
        try {
            val responseData = JSONObject(responseBody.string())
            viewBinding.status.append("\n\n" + getString(R.string.setup_intent_status,
                responseData.getString("status")))
            val secret = responseData.getString("secret")
            confirmSetupIntent(secret)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onAuthComplete() {
        viewBinding.buy3ds2Button.isEnabled = true
        viewBinding.buy3ds1Button.isEnabled = true
        viewBinding.setupButton.isEnabled = true
        viewBinding.progressBar.visibility = View.INVISIBLE
    }

    private fun createPaymentIntentParams(stripeAccountId: String?): Map<String, Any> {
        return mapOf(
            "payment_method_types[]" to "card",
            "amount" to 1000,
            "country" to "us"
        )
            .plus(
                stripeAccountId?.let {
                    mapOf("stripe_account" to it)
                }.orEmpty()
            )
    }

    private class PaymentIntentResultCallback constructor(
        activity: PaymentAuthActivity
    ) : ApiResultCallback<PaymentIntentResult> {
        private val activityRef: WeakReference<PaymentAuthActivity> = WeakReference(activity)

        override fun onSuccess(result: PaymentIntentResult) {
            val activity = activityRef.get() ?: return

            val paymentIntent = result.intent
            activity.viewBinding.status.append("\n\n" +
                "Auth outcome: " + result.outcome + "\n\n" +
                activity.getString(R.string.payment_intent_status, paymentIntent.status))
            activity.onAuthComplete()
        }

        override fun onError(e: Exception) {
            val activity = activityRef.get() ?: return

            activity.viewBinding.status.append("\n\nException: " + e.message)
            activity.onAuthComplete()
        }
    }

    private class SetupIntentResultCallback constructor(
        activity: PaymentAuthActivity
    ) : ApiResultCallback<SetupIntentResult> {
        private val activityRef: WeakReference<PaymentAuthActivity> = WeakReference(activity)

        override fun onSuccess(result: SetupIntentResult) {
            val activity = activityRef.get() ?: return

            val setupIntent = result.intent
            activity.viewBinding.status.append("\n\n" + activity.getString(R.string.setup_intent_status, setupIntent.status))
            activity.onAuthComplete()
        }

        override fun onError(e: Exception) {
            val activity = activityRef.get() ?: return

            activity.viewBinding.status.append("\n\nException: " + e.message)
            activity.onAuthComplete()
        }
    }

    private companion object {

        /**
         * See https://stripe.com/docs/payments/3d-secure#three-ds-cards for more options.
         */
        private fun create3ds2ConfirmParams(
            paymentIntentClientSecret: String
        ): ConfirmPaymentIntentParams {
            return ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.Card.Builder()
                        .setNumber("4000000000003238")
                        .setExpiryMonth(1)
                        .setExpiryYear(2025)
                        .setCvc("123")
                        .build()
                ),
                paymentIntentClientSecret,
                RETURN_URL
            )
        }

        private fun create3ds1ConfirmParams(
            paymentIntentClientSecret: String
        ): ConfirmPaymentIntentParams {
            return ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.Card.Builder()
                        .setNumber("4000000000003063")
                        .setExpiryMonth(1)
                        .setExpiryYear(2025)
                        .setCvc("123")
                        .build()
                ),
                paymentIntentClientSecret,
                RETURN_URL
            )
        }

        private fun create3ds2SetupIntentParams(
            setupIntentClientSecret: String
        ): ConfirmSetupIntentParams {
            return ConfirmSetupIntentParams.create(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.Card.Builder()
                        .setNumber("4000000000003238")
                        .setExpiryMonth(1)
                        .setExpiryYear(2025)
                        .setCvc("123")
                        .build()
                ),
                setupIntentClientSecret,
                RETURN_URL
            )
        }

        private const val RETURN_URL = "stripe://payment_auth"

        private const val STATE_STATUS = "status"

        enum class AuthType {
            ThreeDS1,
            ThreeDS2
        }
    }
}
