package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.model.Address
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.example.R
import com.stripe.example.databinding.CreateSepaDebitActivityBinding

/**
 * An example integration for confirming a Payment Intent using a SEPA Debit Payment Method.
 *
 * See [SEPA Direct Debit payments](https://stripe.com/docs/payments/sepa-debit) for more
 * details.
 */
class ConfirmSepaDebitActivity : StripeIntentActivity() {
    private val viewBinding: CreateSepaDebitActivityBinding by lazy {
        CreateSepaDebitActivityBinding.inflate(layoutInflater)
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setTitle(R.string.launch_confirm_pm_sepa_debit)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.ibanInput.setText(TEST_ACCOUNT_NUMBER)

        viewBinding.confirmButton.setOnClickListener {
            val params =
                createPaymentMethodParams(viewBinding.ibanInput.text.toString())
                    .takeIf { EXISTING_PAYMENT_METHOD_ID == null }
            createAndConfirmPaymentIntent(
                "nl",
                params,
                existingPaymentMethodId = EXISTING_PAYMENT_METHOD_ID,
                mandateDataParams = mandateData
            )
        }
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmButton.isEnabled = enabled
    }

    override fun onConfirmSuccess() {
        super.onConfirmSuccess()
        snackbarController.show("Confirmation succeeded.")
    }

    override fun onConfirmCanceled() {
        super.onConfirmCanceled()
        snackbarController.show("Confirmation canceled.")
    }

    override fun onConfirmError(failedResult: PaymentResult.Failed) {
        super.onConfirmError(failedResult)
        snackbarController.show("Error during confirmation: ${failedResult.throwable.message}")
    }

    private fun createPaymentMethodParams(iban: String): PaymentMethodCreateParams {
        return PaymentMethodCreateParams.create(
            PaymentMethodCreateParams.SepaDebit(iban),
            PaymentMethod.BillingDetails.Builder()
                .setAddress(
                    Address.Builder()
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

    private companion object {
        private const val TEST_ACCOUNT_NUMBER = "DE89370400440532013000"

        // set to an existing payment method id to use in integration
        private val EXISTING_PAYMENT_METHOD_ID: String? = null

        private val mandateData = MandateDataParams(
            MandateDataParams.Type.Online(
                ipAddress = "127.0.0.1",
                userAgent = "agent"
            )
        )
    }
}
