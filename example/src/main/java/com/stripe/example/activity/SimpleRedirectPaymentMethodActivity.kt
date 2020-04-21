package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.databinding.SimpleRedirectActivityBinding
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

class SimpleRedirectPaymentMethodActivity : AppCompatActivity() {
    private val stripe: Stripe by lazy {
        Stripe(this, PaymentConfiguration.getInstance(this).publishableKey)
    }
    private val viewBinding: SimpleRedirectActivityBinding by lazy {
        SimpleRedirectActivityBinding.inflate(layoutInflater)
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }
    private val compositeSubscription = CompositeDisposable()
    private val backendApi: BackendApi by lazy {
        BackendApiFactory(this).create()
    }

    private val dropdownItem: DropdownItem
        get() {
            return DropdownItem.valueOf(
                viewBinding.paymentMethod.text.toString()
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this.applicationContext,
            R.layout.dropdown_menu_popup_item,
            DropdownItem.values().map { it.name })
        viewBinding.paymentMethod.setAdapter(adapter)
        viewBinding.paymentMethod.setOnItemClickListener { _, _, _, _ ->
            viewBinding.status.text = ""
            val dropdownItem = this.dropdownItem
            viewBinding.nameLayout.visibility = if (dropdownItem.requiresName) View.VISIBLE else View.GONE

            viewBinding.emailLayout.visibility = if (dropdownItem.requiresEmail) View.VISIBLE else View.GONE
        }
        viewBinding.paymentMethod.setText(DropdownItem.P24.name, false)

        viewBinding.submit.setOnClickListener {
            keyboardController.hide()

            val dropdownItem = this.dropdownItem
            val billingDetails = PaymentMethod.BillingDetails(
                name = viewBinding.name.text.toString().takeIf { dropdownItem.requiresName },
                email = viewBinding.email.text.toString().takeIf { dropdownItem.requiresEmail }
            )

            val country = dropdownItem.country
            val params = dropdownItem.createParams(billingDetails, null)

            createPaymentIntent(country, params)
        }
    }

    override fun onDestroy() {
        compositeSubscription.dispose()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        stripe.onPaymentResult(requestCode, data, PaymentIntentResultCallback(this))
    }

    private fun createPaymentIntent(country: String, params: PaymentMethodCreateParams) {
        compositeSubscription.add(
            backendApi
                .createPaymentIntent(
                    mutableMapOf(
                        "amount" to 1000,
                        "country" to country
                    )
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    disableUi()
                    viewBinding.status.setText(R.string.creating_payment_intent)
                }
                .subscribe(
                    { handleCreatePaymentIntentResponse(it, params) },
                    { handleError(it) }
                )
        )
    }

    private fun disableUi() {
        viewBinding.submit.isEnabled = false
        viewBinding.name.isEnabled = false
        viewBinding.email.isEnabled = false
        viewBinding.progressBar.visibility = View.VISIBLE
    }

    private fun handleError(err: Throwable) {
        viewBinding.submit.isEnabled = true
        viewBinding.name.isEnabled = true
        viewBinding.email.isEnabled = true
        viewBinding.progressBar.visibility = View.INVISIBLE
        viewBinding.status.append("\n\n" + err.message)
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
            handleError(e)
        } catch (e: JSONException) {
            handleError(e)
        }
    }

    private fun confirmPaymentIntent(
        paymentIntentClientSecret: String,
        params: PaymentMethodCreateParams
    ) {
        viewBinding.status.append("\n\nStarting payment authentication")
        stripe.confirmPayment(
            this,
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = params,
                clientSecret = paymentIntentClientSecret,
                returnUrl = "example://return_url"
            )
        )
    }

    private fun onConfirmSuccess(result: PaymentIntentResult) {
        val paymentIntent = result.intent
        viewBinding.status.append("\n\n" +
            "Auth outcome: ${result.outcome}\n\n" +
            getString(R.string.payment_intent_status, paymentIntent.status)
        )
        viewBinding.submit.isEnabled = true
        viewBinding.progressBar.visibility = View.INVISIBLE
    }

    private fun onConfirmError(e: Exception) {
        viewBinding.status.append("\n\nException: " + e.message)
        viewBinding.submit.isEnabled = true
        viewBinding.progressBar.visibility = View.INVISIBLE
    }

    private class PaymentIntentResultCallback(
        activity: SimpleRedirectPaymentMethodActivity
    ) : ApiResultCallback<PaymentIntentResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: PaymentIntentResult) {
            activityRef.get()?.onConfirmSuccess(result)
        }

        override fun onError(e: Exception) {
            activityRef.get()?.onConfirmError(e)
        }
    }

    private enum class DropdownItem(
        val country: String,
        val createParams: (PaymentMethod.BillingDetails, Map<String, String>?) -> PaymentMethodCreateParams,
        val requiresName: Boolean = true,
        val requiresEmail: Boolean = false
    ) {
        P24("pl", PaymentMethodCreateParams.Companion::createP24, requiresName = false, requiresEmail = true),
        Bancontact("be", PaymentMethodCreateParams.Companion::createBancontact),
        EPS("at", PaymentMethodCreateParams.Companion::createEps),
        Giropay("de", PaymentMethodCreateParams.Companion::createGiropay);
    }
}
