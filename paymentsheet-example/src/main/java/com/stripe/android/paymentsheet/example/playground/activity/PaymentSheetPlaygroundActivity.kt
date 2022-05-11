package com.stripe.android.paymentsheet.example.playground.activity

import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.databinding.ActivityPaymentSheetPlaygroundBinding
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.model.Toggle
import com.stripe.android.paymentsheet.example.playground.viewmodel.PaymentSheetPlaygroundViewModel
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.launch

class PaymentSheetPlaygroundActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetPlaygroundBinding.inflate(layoutInflater)
    }

    @VisibleForTesting
    val viewModel: PaymentSheetPlaygroundViewModel by lazy {
        PaymentSheetPlaygroundViewModel(application)
    }

    private val customer: CheckoutCustomer
        get() = when (viewBinding.customerRadioGroup.checkedRadioButtonId) {
            R.id.guest_customer_button -> CheckoutCustomer.Guest
            R.id.new_customer_button -> {
                viewModel.temporaryCustomerId?.let {
                    CheckoutCustomer.WithId(it)
                } ?: CheckoutCustomer.New
            }
            else -> {
                val useSnapshotCustomer = intent.extras?.get(USE_SNAPSHOT_RETURNING_CUSTOMER) as Boolean?
                if (useSnapshotCustomer != null) {
                    CheckoutCustomer.Snapshot
                } else {
                    CheckoutCustomer.Returning
                }
            }
        }

    private val googlePayConfig: PaymentSheet.GooglePayConfiguration?
        get() = when (viewBinding.googlePayRadioGroup.checkedRadioButtonId) {
            R.id.google_pay_on_button -> {
                PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US",
                    currencyCode = currency.value
                )
            }
            else -> null
        }

    private val currency: CheckoutCurrency
        get() = when (viewBinding.currencyRadioGroup.checkedRadioButtonId) {
            R.id.currency_usd_button -> CheckoutCurrency.USD
            R.id.currency_aud_button -> CheckoutCurrency.AUD
            else -> CheckoutCurrency.EUR
        }

    private val mode: CheckoutMode
        get() = when (viewBinding.modeRadioGroup.checkedRadioButtonId) {
            R.id.mode_payment_button -> CheckoutMode.Payment
            R.id.mode_payment_with_setup_button -> CheckoutMode.PaymentWithSetup
            else -> CheckoutMode.Setup
        }

    private val linkEnabled: Boolean
        get() = viewBinding.linkRadioGroup.checkedRadioButtonId == R.id.link_on_button

    private val setShippingAddress: Boolean
        get() = viewBinding.shippingRadioGroup.checkedRadioButtonId == R.id.shipping_on_button

    private val setDefaultBillingAddress: Boolean
        get() = viewBinding.defaultBillingRadioGroup.checkedRadioButtonId == R.id.default_billing_on_button

    private val setAutomaticPaymentMethods: Boolean
        get() = viewBinding.automaticPmGroup.checkedRadioButtonId == R.id.automatic_pm_on_button

    private val setDelayedPaymentMethods: Boolean
        get() = viewBinding.allowsDelayedPaymentMethodsRadioGroup.checkedRadioButtonId == R.id.allowsDelayedPaymentMethods_on_button

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var flowController: PaymentSheet.FlowController

    @Nullable
    private var multiStepUIReadyIdlingResource: CountingIdlingResource? = null

    @Nullable
    private var singleStepUIReadyIdlingResource: CountingIdlingResource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val shouldUseDarkMode = intent.extras?.get(FORCE_DARK_MODE_EXTRA) as Boolean?
        if (shouldUseDarkMode != null) {
            val mode = if (shouldUseDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        flowController = PaymentSheet.FlowController.create(
            this,
            ::onPaymentOption,
            ::onPaymentSheetResult
        )
        val backendUrl = Settings(this).playgroundBackendUrl

        viewBinding.resetDefaultsButton.setOnClickListener {
            setToggles(
                Toggle.Customer.default.toString(),
                Toggle.Link.default as Boolean,
                Toggle.GooglePay.default as Boolean,
                Toggle.Currency.default.toString(),
                Toggle.Mode.default.toString(),
                Toggle.SetShippingAddress.default as Boolean,
                Toggle.SetDefaultBillingAddress.default as Boolean,
                Toggle.SetAutomaticPaymentMethods.default as Boolean,
                Toggle.SetDelayedPaymentMethods.default as Boolean,
            )
        }

        viewBinding.reloadButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.prepareCheckout(
                    customer,
                    currency,
                    mode,
                    linkEnabled,
                    setShippingAddress,
                    setAutomaticPaymentMethods,
                    backendUrl
                )
            }
        }

        viewBinding.completeCheckoutButton.setOnClickListener {
            startCompleteCheckout()
        }

        viewBinding.customCheckoutButton.setOnClickListener {
            flowController.confirm()
        }

        viewBinding.paymentMethod.setOnClickListener {
            flowController.presentPaymentOptions()
        }

        viewModel.status.observe(this) {
            Snackbar.make(
                findViewById(android.R.id.content), it, Snackbar.LENGTH_SHORT
            )
                .setBackgroundTint(resources.getColor(R.color.black))
                .setTextColor(resources.getColor(R.color.white))
                .show()
        }

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.isInvisible = !it
            if (it) {
                singleStepUIReadyIdlingResource?.increment()
                multiStepUIReadyIdlingResource?.increment()
            } else {
                singleStepUIReadyIdlingResource?.decrement()
            }
        }

        viewModel.readyToCheckout.observe(this) { isReady ->
            if (isReady) {
                viewBinding.completeCheckoutButton.isEnabled = true
                configureCustomCheckout()
            } else {
                disableViews()
            }
        }

        disableViews()
    }

    override fun onPause() {
        super.onPause()
        viewModel.storeToggleState(
            customer.value,
            linkEnabled,
            googlePayConfig != null,
            currency.value,
            mode.value,
            setShippingAddress,
            setDefaultBillingAddress,
            setAutomaticPaymentMethods,
            setDelayedPaymentMethods
        )
    }

    private fun setToggles(
        customer: String?,
        link: Boolean,
        googlePay: Boolean,
        currency: String?,
        mode: String?,
        setShippingAddress: Boolean,
        setDefaultBillingAddress: Boolean,
        setAutomaticPaymentMethods: Boolean,
        setDelayedPaymentMethods: Boolean,
    ) {
        when (customer) {
            CheckoutCustomer.Guest.value -> viewBinding.customerRadioGroup.check(R.id.guest_customer_button)
            CheckoutCustomer.New.value -> viewBinding.customerRadioGroup.check(R.id.new_customer_button)
            else -> viewBinding.customerRadioGroup.check(R.id.returning_customer_button)
        }

        when (link) {
            true -> viewBinding.linkRadioGroup.check(R.id.link_on_button)
            false -> viewBinding.linkRadioGroup.check(R.id.link_off_button)
        }

        when (googlePay) {
            true -> viewBinding.googlePayRadioGroup.check(R.id.google_pay_on_button)
            false -> viewBinding.googlePayRadioGroup.check(R.id.google_pay_off_button)
        }

        when (currency) {
            CheckoutCurrency.USD.value -> viewBinding.currencyRadioGroup.check(R.id.currency_usd_button)
            CheckoutCurrency.AUD.value -> viewBinding.currencyRadioGroup.check(R.id.currency_aud_button)
            else -> viewBinding.currencyRadioGroup.check(R.id.currency_eur_button)
        }

        when (mode) {
            CheckoutMode.Payment.value -> viewBinding.modeRadioGroup.check(R.id.mode_payment_button)
            CheckoutMode.PaymentWithSetup.value -> viewBinding.modeRadioGroup.check(R.id.mode_payment_with_setup_button)
            else -> viewBinding.modeRadioGroup.check(R.id.mode_setup_button)
        }

        when (setShippingAddress) {
            true -> viewBinding.shippingRadioGroup.check(R.id.shipping_on_button)
            false -> viewBinding.shippingRadioGroup.check(R.id.shipping_off_button)
        }

        when (setDefaultBillingAddress) {
            true -> viewBinding.defaultBillingRadioGroup.check(R.id.default_billing_on_button)
            false -> viewBinding.defaultBillingRadioGroup.check(R.id.default_billing_off_button)
        }

        when (setAutomaticPaymentMethods) {
            true -> viewBinding.automaticPmGroup.check(R.id.automatic_pm_on_button)
            false -> viewBinding.automaticPmGroup.check(R.id.automatic_pm_off_button)
        }

        when (setDelayedPaymentMethods) {
            true -> viewBinding.allowsDelayedPaymentMethodsRadioGroup.check(R.id.allowsDelayedPaymentMethods_on_button)
            false -> viewBinding.allowsDelayedPaymentMethodsRadioGroup.check(R.id.allowsDelayedPaymentMethods_off_button)
        }
    }

    private fun disableViews() {
        viewBinding.completeCheckoutButton.isEnabled = false
        viewBinding.customCheckoutButton.isEnabled = false
        viewBinding.paymentMethod.isClickable = false
    }

    private fun startCompleteCheckout() {
        val clientSecret = viewModel.clientSecret.value ?: return

        if (viewModel.checkoutMode == CheckoutMode.Setup) {
            paymentSheet.presentWithSetupIntent(
                clientSecret,
                makeConfiguration()
            )
        } else {
            paymentSheet.presentWithPaymentIntent(
                clientSecret,
                makeConfiguration()
            )
        }
    }

    private fun configureCustomCheckout() {
        val clientSecret = viewModel.clientSecret.value ?: return

        if (viewModel.checkoutMode == CheckoutMode.Setup) {
            flowController.configureWithSetupIntent(
                clientSecret,
                makeConfiguration(),
                ::onConfigured
            )
        } else {
            flowController.configureWithPaymentIntent(
                clientSecret,
                makeConfiguration(),
                ::onConfigured
            )
        }
    }

    private fun makeConfiguration(): PaymentSheet.Configuration {
        val defaultBilling = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(
                line1 = "123 Main Street",
                line2 = null,
                city = "Blackrock",
                state = "Co. Dublin",
                postalCode = "T37 F8HK",
                country = "IE",
            ),
            email = "email@email.com",
            name = "Jenny Rosen",
            phone = "+18008675309"
        ).takeIf { viewBinding.defaultBillingOnButton.isChecked }

        return PaymentSheet.Configuration(
            merchantDisplayName = merchantName,
            customer = viewModel.customerConfig.value,
            googlePay = googlePayConfig,
            defaultBillingDetails = defaultBilling,
            allowsDelayedPaymentMethods = viewBinding.allowsDelayedPaymentMethodsOnButton.isChecked
        )
    }

    private fun onConfigured(success: Boolean, error: Throwable?) {
        if (success) {
            viewBinding.paymentMethod.isClickable = true
            onPaymentOption(flowController.getPaymentOption())
            multiStepUIReadyIdlingResource?.decrement()
        } else {
            viewModel.status.value =
                "Failed to configure PaymentSheetFlowController: ${error?.message}"
        }
    }

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        if (paymentOption != null) {
            viewBinding.paymentMethod.text = paymentOption.label
            viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
                paymentOption.drawableResourceId,
                0,
                0,
                0
            )
            viewBinding.customCheckoutButton.isEnabled = true
        } else {
            viewBinding.paymentMethod.setText(R.string.select)
            viewBinding.paymentMethod.setCompoundDrawables(null, null, null, null)
            viewBinding.customCheckoutButton.isEnabled = false
        }
    }

    private fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        if (paymentResult !is PaymentSheetResult.Canceled) {
            disableViews()
        }

        viewModel.status.value = paymentResult.toString()
    }

    /**
     * Only called from test, creates and returns a [IdlingResource].
     */
    @VisibleForTesting
    @NonNull
    fun getMultiStepReadyIdlingResource(): IdlingResource? {
        if (multiStepUIReadyIdlingResource == null) {
            multiStepUIReadyIdlingResource =
                CountingIdlingResource("multiStepUIReadyIdlingResource")
        }
        return multiStepUIReadyIdlingResource
    }

    @VisibleForTesting
    @NonNull
    fun getSingleStepReadyIdlingResource(): IdlingResource? {
        if (singleStepUIReadyIdlingResource == null) {
            singleStepUIReadyIdlingResource =
                CountingIdlingResource("singleStepUIReadyIdlingResource")
        }
        return singleStepUIReadyIdlingResource
    }


    companion object {
        const val FORCE_DARK_MODE_EXTRA = "ForceDark"
        const val USE_SNAPSHOT_RETURNING_CUSTOMER = "UseSnapshotReturningCustomer"
        private const val merchantName = "Example, Inc."
        private const val sharedPreferencesName = "playgroundToggles"
    }
}
