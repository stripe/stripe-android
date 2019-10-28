package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.module.RetrofitFactory
import com.stripe.example.service.BackendApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_payment_auth.*
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * An example of creating a PaymentIntent, then confirming it with [Stripe.confirmPayment]
 */
class PaymentAuthActivity : AppCompatActivity() {

    private val compositeSubscription = CompositeDisposable()

    private lateinit var stripe: Stripe
    private lateinit var backendApi: BackendApi
    private lateinit var statusTextView: TextView

    private val stripeAccountId: String? = Settings.STRIPE_ACCOUNT_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_auth)

        val uiCustomization =
            PaymentAuthConfig.Stripe3ds2UiCustomization.Builder().build()
        PaymentAuthConfig.init(PaymentAuthConfig.Builder()
            .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setTimeout(6)
                .setUiCustomization(uiCustomization)
                .build())
            .build())

        statusTextView = findViewById(R.id.status)
        if (savedInstanceState != null) {
            statusTextView.text = savedInstanceState.getString(STATE_STATUS)
        }

        backendApi = RetrofitFactory.instance.create(BackendApi::class.java)
        val publishableKey = PaymentConfiguration.getInstance(this).publishableKey
        stripe = Stripe(this, publishableKey,
            stripeAccountId = stripeAccountId,
            enableLogging = true
        )

        buy_3ds1_button.setOnClickListener {
            createPaymentIntent(stripeAccountId, AuthType.ThreeDS1)
        }
        buy_3ds2_button.setOnClickListener {
            createPaymentIntent(stripeAccountId, AuthType.ThreeDS2)
        }

        setup_button.setOnClickListener { createSetupIntent() }
    }

    private fun confirmPaymentIntent(
        paymentIntentClientSecret: String,
        authType: AuthType
    ) {
        statusTextView.append("\n\nStarting payment authentication")
        stripe.confirmPayment(
            this,
            when (authType) {
                AuthType.ThreeDS1 -> create3ds1ConfirmParams(paymentIntentClientSecret)
                AuthType.ThreeDS2 -> create3ds2ConfirmParams(paymentIntentClientSecret)
            }
        )
    }

    private fun confirmSetupIntent(setupIntentClientSecret: String) {
        statusTextView.append("\n\nStarting setup intent authentication")
        stripe.confirmSetupIntent(this, create3ds2SetupIntentParams(setupIntentClientSecret))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        progress_bar.visibility = View.VISIBLE
        statusTextView.append("\n\nPayment authentication completed, getting result")

        val isPaymentResult =
            stripe.onPaymentResult(requestCode, data, AuthResultListener(this))
        if (!isPaymentResult) {
            stripe.onSetupResult(requestCode, data, SetupAuthResultListener(this))
        }
    }

    override fun onPause() {
        progress_bar.visibility = View.INVISIBLE
        super.onPause()
    }

    override fun onDestroy() {
        compositeSubscription.dispose()
        super.onDestroy()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(STATE_STATUS, statusTextView.text.toString())
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
                    progress_bar.visibility = View.VISIBLE
                    buy_3ds2_button.isEnabled = false
                    buy_3ds1_button.isEnabled = false
                    setup_button.isEnabled = false
                    statusTextView.setText(R.string.creating_payment_intent)
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
                    progress_bar.visibility = View.VISIBLE
                    buy_3ds2_button.isEnabled = false
                    buy_3ds1_button.isEnabled = false
                    setup_button.isEnabled = false
                    statusTextView.setText(R.string.creating_setup_intent)
                }
                .subscribe(
                    { handleCreateSetupIntentResponse(it) },
                    { handleError(it) }
                )
        )
    }

    private fun handleError(err: Throwable) {
        progress_bar.visibility = View.INVISIBLE
        err.printStackTrace()
        statusTextView.append("\n\n" + err.message)
    }

    private fun handleCreatePaymentIntentResponse(
        responseBody: ResponseBody,
        authType: AuthType
    ) {
        try {
            val responseData = JSONObject(responseBody.string())
            statusTextView.append("\n\n" + getString(R.string.payment_intent_status,
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
            statusTextView.append("\n\n" + getString(R.string.setup_intent_status,
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
        buy_3ds2_button.isEnabled = true
        buy_3ds1_button.isEnabled = true
        setup_button.isEnabled = true
        progress_bar.visibility = View.INVISIBLE
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

    private class AuthResultListener constructor(
        activity: PaymentAuthActivity
    ) : ApiResultCallback<PaymentIntentResult> {
        private val activityRef: WeakReference<PaymentAuthActivity> = WeakReference(activity)

        override fun onSuccess(result: PaymentIntentResult) {
            val activity = activityRef.get() ?: return

            val paymentIntent = result.intent
            activity.statusTextView.append("\n\n" +
                "Auth outcome: " + result.outcome + "\n\n" +
                activity.getString(R.string.payment_intent_status, paymentIntent.status))
            activity.onAuthComplete()
        }

        override fun onError(e: Exception) {
            val activity = activityRef.get() ?: return

            activity.statusTextView.append("\n\nException: " + e.message)
            activity.onAuthComplete()
        }
    }

    private class SetupAuthResultListener constructor(activity: PaymentAuthActivity) : ApiResultCallback<SetupIntentResult> {
        private val activityRef: WeakReference<PaymentAuthActivity> = WeakReference(activity)

        override fun onSuccess(result: SetupIntentResult) {
            val activity = activityRef.get() ?: return

            val setupIntent = result.intent
            activity.statusTextView.append("\n\n" + activity.getString(R.string.setup_intent_status, setupIntent.status))
            activity.onAuthComplete()
        }

        override fun onError(e: Exception) {
            val activity = activityRef.get() ?: return

            activity.statusTextView.append("\n\nException: " + e.message)
            activity.onAuthComplete()
        }
    }

    companion object {

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
