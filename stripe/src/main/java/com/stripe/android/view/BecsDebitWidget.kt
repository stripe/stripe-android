package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.stripe.android.R
import com.stripe.android.databinding.BecsDebitWidgetBinding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams

/**
 * A form for accepting a customer's BECS account information.
 */
class BecsDebitWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    internal val viewBinding: BecsDebitWidgetBinding by lazy {
        BecsDebitWidgetBinding.inflate(
            LayoutInflater.from(context),
            this
        )
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewBinding.nameEditText.setAutofillHints(View.AUTOFILL_HINT_NAME)
            viewBinding.emailEditText.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS)
        }

        viewBinding.bsbEditText.onBankChangedCallback = { bank ->
            if (bank != null) {
                viewBinding.bsbTextInputLayout.helperText = bank.name
                viewBinding.bsbTextInputLayout.isHelperTextEnabled = true
            } else {
                viewBinding.bsbTextInputLayout.helperText = null
                viewBinding.bsbTextInputLayout.isHelperTextEnabled = false
            }
        }

        viewBinding.bsbEditText.onCompletedCallback = {
            viewBinding.accountNumberTextInputLayout.requestFocus()
        }

        // Focus on the previous `EditText` when tapping backspace on an empty `EditText`
        viewBinding.emailEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(viewBinding.nameEditText)
        )
        viewBinding.bsbEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(viewBinding.emailEditText)
        )
        viewBinding.accountNumberEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(viewBinding.bsbEditText)
        )

        viewBinding.nameEditText.errorMessage = resources.getString(
            R.string.becs_widget_name_required
        )
        viewBinding.nameEditText.setErrorMessageListener(
            ErrorListener(viewBinding.nameTextInputLayout)
        )

        viewBinding.emailEditText.errorMessage = resources.getString(
            R.string.becs_widget_email_required
        )
        viewBinding.emailEditText.setErrorMessageListener(
            ErrorListener(viewBinding.emailTextInputLayout)
        )

        viewBinding.bsbEditText.setErrorMessageListener(
            ErrorListener(viewBinding.bsbTextInputLayout)
        )

        viewBinding.accountNumberEditText.errorMessage = resources.getString(
            R.string.becs_widget_account_number_required
        )
        viewBinding.accountNumberEditText.setErrorMessageListener(
            ErrorListener(viewBinding.accountNumberTextInputLayout)
        )

        setOf(viewBinding.nameEditText, viewBinding.emailEditText).forEach { field ->
            field.addTextChangedListener(object : StripeTextWatcher() {
                override fun afterTextChanged(s: Editable?) {
                    field.shouldShowError = false
                }
            })
        }
    }

    /**
     * @return if the customer's input is valid, will return a [PaymentMethodCreateParams] instance;
     * otherwise, will return `null`
     */
    val params: PaymentMethodCreateParams?
        get() {
            val name = viewBinding.nameEditText.fieldText
            val email = viewBinding.emailEditText.fieldText
            val bsbNumber = viewBinding.bsbEditText.bsb
            val accountNumber = viewBinding.accountNumberEditText.fieldText

            viewBinding.nameEditText.shouldShowError = name.isBlank()
            viewBinding.emailEditText.shouldShowError = email.isBlank()
            viewBinding.bsbEditText.shouldShowError = bsbNumber.isNullOrBlank()
            viewBinding.accountNumberEditText.shouldShowError = accountNumber.isBlank()

            if (name.isBlank() || email.isBlank() || bsbNumber.isNullOrBlank() ||
                accountNumber.isBlank()) {
                return null
            }

            return PaymentMethodCreateParams.create(
                auBecsDebit = PaymentMethodCreateParams.AuBecsDebit(
                    bsbNumber = bsbNumber,
                    accountNumber = accountNumber
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = name,
                    email = email
                )
            )
        }
}
