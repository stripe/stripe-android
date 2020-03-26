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
 *
 * A company name is required to render this widget. A company name can either be specified by
 * passing it to the [BecsDebitWidget] constructor, or via XML:
 *
 * ```
 * <com.stripe.android.view.BecsDebitWidget
 *   android:id="@+id/element"
 *   android:layout_width="match_parent"
 *   android:layout_height="wrap_content"
 *   app:companyName="@string/becs_company_name" />
 * ```
 */
class BecsDebitWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    companyName: String = ""
) : FrameLayout(context, attrs, defStyleAttr) {

    internal val viewBinding: BecsDebitWidgetBinding by lazy {
        BecsDebitWidgetBinding.inflate(
            LayoutInflater.from(context),
            this
        )
    }

    var validParamsCallback: ValidParamsCallback = object : ValidParamsCallback {
        override fun onInputChanged(isValid: Boolean) {
            // no-op default implementation
        }
    }

    private val validParamsTextWatcher = object : StripeTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            super.afterTextChanged(s)
            validParamsCallback.onInputChanged(isInputValid)
        }
    }

    private val isInputValid: Boolean
        get() {
            val name = viewBinding.nameEditText.fieldText
            val email = viewBinding.emailEditText.email
            val bsbNumber = viewBinding.bsbEditText.bsb
            val accountNumber = viewBinding.accountNumberEditText.accountNumber

            return !(name.isBlank() || email.isNullOrBlank() || bsbNumber.isNullOrBlank() ||
                accountNumber.isNullOrBlank())
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewBinding.nameEditText.setAutofillHints(View.AUTOFILL_HINT_NAME)
            viewBinding.emailEditText.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS)
        }

        setOf(
            viewBinding.nameEditText,
            viewBinding.emailEditText,
            viewBinding.bsbEditText,
            viewBinding.accountNumberEditText
        ).forEach {
            it.addTextChangedListener(validParamsTextWatcher)
        }

        viewBinding.bsbEditText.onBankChangedCallback = { bank ->
            if (bank != null) {
                viewBinding.bsbTextInputLayout.helperText = bank.name
                viewBinding.bsbTextInputLayout.isHelperTextEnabled = true
            } else {
                viewBinding.bsbTextInputLayout.helperText = null
                viewBinding.bsbTextInputLayout.isHelperTextEnabled = false
            }

            viewBinding.accountNumberEditText.minLength = when (bank?.prefix?.take(2)) {
                // Stripe
                "00" -> 9

                // ANZ: 9 digits https://www.anz.com.au/support/help/
                "01" -> 9

                // NAB: 9 digits
                // https://www.nab.com.au/business/accounts/business-accounts-online-application-help
                "08" -> 9

                // Commonwealth/CBA: 8 digits
                // https://www.commbank.com.au/support.digital-banking.confirm-account-number-digits.html
                "06" -> 8

                // Westpac/WBC: 6 digits
                "03", "73" -> 6

                // Cuscal: 4 digits(?)
                "80" -> 4

                else -> BecsDebitAccountNumberEditText.DEFAULT_MIN_LENGTH
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

        viewBinding.emailEditText.setErrorMessageListener(
            ErrorListener(viewBinding.emailTextInputLayout)
        )

        viewBinding.bsbEditText.setErrorMessageListener(
            ErrorListener(viewBinding.bsbTextInputLayout)
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

        companyName.takeIf { it.isNotBlank() }?.let {
            viewBinding.mandateAcceptanceTextView.companyName = it
        }
        attrs?.let { applyAttributes(it) }

        verifyCompanyName()
    }

    private fun verifyCompanyName() {
        require(viewBinding.mandateAcceptanceTextView.isValid) {
            """
            A company name is required to render a BecsDebitWidget.
            """.trimIndent()
        }
    }

    private fun applyAttributes(attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.BecsDebitWidget, 0, 0
        )

        try {
            val companyName = typedArray.getString(
                R.styleable.BecsDebitWidget_companyName
            )
            companyName?.let {
                viewBinding.mandateAcceptanceTextView.companyName = it
            }
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * @return if the customer's input is valid, will return a [PaymentMethodCreateParams] instance;
     * otherwise, will return `null`
     */
    val params: PaymentMethodCreateParams?
        get() {
            val name = viewBinding.nameEditText.fieldText
            val email = viewBinding.emailEditText.email
            val bsbNumber = viewBinding.bsbEditText.bsb
            val accountNumber = viewBinding.accountNumberEditText.accountNumber

            viewBinding.nameEditText.shouldShowError = name.isBlank()
            viewBinding.emailEditText.shouldShowError = email.isNullOrBlank()
            viewBinding.bsbEditText.shouldShowError = bsbNumber.isNullOrBlank()
            viewBinding.accountNumberEditText.shouldShowError = accountNumber.isNullOrBlank()

            if (name.isBlank() || email.isNullOrBlank() || bsbNumber.isNullOrBlank() ||
                accountNumber.isNullOrBlank()) {
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

    interface ValidParamsCallback {
        /**
         * @param isValid if the current input is valid
         */
        fun onInputChanged(isValid: Boolean)
    }
}
