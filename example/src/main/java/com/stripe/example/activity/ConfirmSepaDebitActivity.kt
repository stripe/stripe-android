package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.module.BackendApiFactory
import com.stripe.example.service.BackendApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.create_sepa_debit_pm_layout.*
import org.json.JSONObject

/**
 * An example integration for confirming a Payment Intent using a SEPA Debit Payment Method.
 *
 * See [SEPA Direct Debit payments](https://stripe.com/docs/payments/sepa-debit) for more
 * details.
 */
class ConfirmSepaDebitActivity : AppCompatActivity() {
    private val compositeSubscription = CompositeDisposable()

    private val backendApi: BackendApi by lazy {
        BackendApiFactory(this).create()
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(findViewById(android.R.id.content))
    }

    private val stripe: Stripe by lazy {
        Stripe(this, PaymentConfiguration.getInstance(this).publishableKey)
    }

    private var clientSecret: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_sepa_debit_pm_layout)
        setTitle(R.string.launch_confirm_pm_sepa_debit)

        iban_input.setText(TEST_ACCOUNT_NUMBER)

        confirm_with_sepa_debit_button.setOnClickListener {
            tv_status.text = ""
            createPaymentIntent(iban_input.text.toString())
        }
    }

    override fun onDestroy() {
        compositeSubscription.dispose()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {
                tv_status.append("\n\nStatus after confirmation: ${result.intent.status}")
                snackbarController.show("Status after confirmation: ${result.intent.status}")
                enableUi()
            }

            override fun onError(e: Exception) {
                tv_status.append("\n\nError during confirmation: ${e.message}")
                snackbarController.show("Error during confirmation: ${e.message}")
                enableUi()
            }
        })
    }

    private fun createPaymentIntent(iban: String) {
        compositeSubscription.add(
            backendApi.createPaymentIntent(PAYMENT_INTENT_PARAMAS.toMutableMap())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    disableUi()
                    tv_status.append(getString(R.string.creating_payment_intent))
                }
                .doFinally {
                    enableUi()
                }
                .subscribe(
                    {
                        handleCreatePaymentIntentResponse(iban, JSONObject(it.string()))
                    },
                    ::handleError
                )
        )
    }

    private fun disableUi() {
        progress_bar.visibility = View.VISIBLE
        confirm_with_sepa_debit_button.isEnabled = false
    }

    private fun enableUi() {
        progress_bar.visibility = View.INVISIBLE
        confirm_with_sepa_debit_button.isEnabled = true
    }

    private fun handleCreatePaymentIntentResponse(
        iban: String,
        json: JSONObject
    ) {
        val intentId = json.getString("intent")
        val clientSecret = json.getString("secret").also {
            this.clientSecret = it
        }

        tv_status.append("\n\nCreated payment intent: $intentId")

        if (EXISTING_PAYMENT_METHOD_ID == null) {
            stripe.confirmPayment(
                this,
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    paymentMethodCreateParams = createPaymentMethodParams(iban),
                    clientSecret = clientSecret
                )
            )
        } else {
            stripe.confirmPayment(
                this,
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    paymentMethodId = EXISTING_PAYMENT_METHOD_ID,
                    clientSecret = clientSecret,
                    mandateData = MandateDataParams(
                        MandateDataParams.Type.Online(
                            ipAddress = "127.0.0.1",
                            userAgent = "agent"
                        )
                    )
                )
            )
        }
    }

    private fun createPaymentMethodParams(iban: String): PaymentMethodCreateParams {
        return PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.SepaDebit(iban),
            PaymentMethod.BillingDetails.Builder()
                .setAddress(Address.Builder()
                    .setCity("San Francisco")
                    .setCountry("US")
                    .setLine1("123 Market St")
                    .setLine2("#345")
                    .setPostalCode("94107")
                    .setState("CA")
                    .build()
                )
                .setEmail("jenny@example.com")
                .setName("Jenny Rosen")
                .setPhone("(555) 555-5555")
                .build()
        )
    }

    private fun handleError(ex: Throwable) {
        tv_status.append("\n\nError while creating PaymentIntent: ${ex.message}")
    }

    private companion object {
        private const val TEST_ACCOUNT_NUMBER = "DE89370400440532013000"

        private val PAYMENT_INTENT_PARAMAS = mapOf(
            "payment_method_types[]" to "sepa_debit",
            "amount" to 1000,
            "country" to "nl"
        )

        // set to an existing payment method id to use in integration
        private val EXISTING_PAYMENT_METHOD_ID: String? = null
    }
}
