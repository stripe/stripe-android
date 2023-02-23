package com.stripe.android.paymentsheet.example.playground.activity

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.MenuProvider
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.AddressLauncherResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.databinding.ActivityPaymentSheetPlaygroundBinding
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.model.InitializationType
import com.stripe.android.paymentsheet.example.playground.model.Shipping
import com.stripe.android.paymentsheet.example.playground.viewmodel.PaymentSheetPlaygroundViewModel
import com.stripe.android.paymentsheet.example.playground.viewmodel.PaymentSheetPlaygroundViewState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class PaymentSheetPlaygroundActivity : AppCompatActivity() {

    private val viewBinding by lazy {
        ActivityPaymentSheetPlaygroundBinding.inflate(layoutInflater)
    }

    private val menuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_playground, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.appearance_picker -> showAppearancePicker()
                else -> Unit
            }
            return true
        }
    }

    @VisibleForTesting
    val viewModel: PaymentSheetPlaygroundViewModel by lazy {
        PaymentSheetPlaygroundViewModel(application)
    }

    private val customLabelTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            viewModel.updateCustomLabel(s?.toString())
        }
    }

    private val settings by lazy {
        Settings(this)
    }

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var flowController: PaymentSheet.FlowController
    private lateinit var addressLauncher: AddressLauncher
    private var shippingAddress: AddressDetails? = null

    private var multiStepUIReadyIdlingResource: CountingIdlingResource? = null

    private var singleStepUIReadyIdlingResource: CountingIdlingResource? = null

    private fun ActivityPaymentSheetPlaygroundBinding.configureUi() {
        initializationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.normal_initialization_button -> InitializationType.Normal
                R.id.deferred_initialization_button -> InitializationType.Deferred
                else -> error("🤔")
            }
            viewModel.updateInitializationType(type)
        }

        modeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val checkoutMode = when (checkedId) {
                R.id.mode_payment_button -> CheckoutMode.Payment
                R.id.mode_payment_with_setup_button -> CheckoutMode.PaymentWithSetup
                else -> CheckoutMode.Setup
            }
            viewModel.updateCheckoutMode(checkoutMode)
        }

        linkRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val isEnabled = checkedId == R.id.link_on_button
            viewModel.updateLinkEnabled(isEnabled)
        }

        googlePayRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val isEnabled = checkedId == R.id.google_pay_on_button
            viewModel.updateGooglePayEnabled(isEnabled)
        }

        customerRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val customer = when (checkedId) {
                R.id.guest_customer_button -> CheckoutCustomer.Guest
                R.id.new_customer_button -> {
                    viewModel.temporaryCustomerId?.let {
                        CheckoutCustomer.WithId(it)
                    } ?: CheckoutCustomer.New
                }
                else -> {
                    val useSnapshotCustomer =
                        intent.extras?.getBoolean(USE_SNAPSHOT_RETURNING_CUSTOMER_EXTRA)

                    if (useSnapshotCustomer == true) {
                        CheckoutCustomer.Snapshot
                    } else {
                        CheckoutCustomer.Returning
                    }
                }
            }

            viewModel.updateCustomer(customer)
        }

        shippingRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val shipping = when (checkedId) {
                R.id.shipping_on_button -> Shipping.On
                R.id.shipping_on_with_defaults_button -> Shipping.OnWithDefaults
                else -> Shipping.Off
            }
            viewModel.updateShipping(shipping)
        }

        defaultBillingRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val setDefaultBillingAddress = checkedId == R.id.default_billing_on_button
            viewModel.updateSetDefaultBillingAddress(setDefaultBillingAddress)
        }

        automaticPmGroup.setOnCheckedChangeListener { _, checkedId ->
            val automaticPaymentMethods = checkedId == R.id.automatic_pm_on_button
            viewModel.updateAutomaticPaymentMethods(automaticPaymentMethods)
        }

        allowsDelayedPaymentMethodsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val allowsDelayed = checkedId == R.id.allowsDelayedPaymentMethods_on_button
            viewModel.updateAllowDelayedPaymentMethods(allowsDelayed)
        }

        currencySpinner.adapter = ArrayAdapter(
            this@PaymentSheetPlaygroundActivity,
            android.R.layout.simple_spinner_dropdown_item,
            PaymentSheetPlaygroundViewModel.stripeSupportedCurrencies,
        )

        viewBinding.merchantCountrySpinner.adapter = ArrayAdapter(
            this@PaymentSheetPlaygroundActivity,
            android.R.layout.simple_spinner_dropdown_item,
            PaymentSheetPlaygroundViewModel.countryCurrencyPairs.map { it.first },
        )

        viewBinding.resetButton.setOnClickListener {
            viewModel.reset()
        }

        viewBinding.reloadButton.setOnClickListener {
            val customPaymentMethods =
                intent.extras?.getStringArray(SUPPORTED_PAYMENT_METHODS_EXTRA)?.toList()

            viewModel.reload(supportedPaymentMethods = customPaymentMethods)
        }

        customLabelTextField.addTextChangedListener(
            this@PaymentSheetPlaygroundActivity.customLabelTextWatcher
        )

        completeCheckoutButton.setOnClickListener {
            startCompleteCheckout()
        }

        customCheckoutButton.setOnClickListener {
            flowController.shippingDetails = this@PaymentSheetPlaygroundActivity.shippingAddress
            flowController.confirm()
        }

        shippingAddressButton.setOnClickListener {
            startShippingAddressCollection()
        }

        paymentMethod.setOnClickListener {
            customLabelTextField.clearFocus()
            flowController.shippingDetails = this@PaymentSheetPlaygroundActivity.shippingAddress
            flowController.presentPaymentOptions()
        }
    }

    private fun ActivityPaymentSheetPlaygroundBinding.render(viewState: PaymentSheetPlaygroundViewState) {
        if (viewState.disableViews) {
            disableViews()
        } else {
            if (viewState.readyForCheckout) {
                completeCheckoutButton.isEnabled = true
            }

            if (viewState.isConfigured) {
                customCheckoutButton.isEnabled = viewState.paymentOption != null
                paymentMethod.isClickable = true
            }

            shippingAddressButton.isEnabled = true
        }

        progressBar.isVisible = viewState.isLoading

        if (viewState.status != null) {
            Snackbar
                .make(findViewById(android.R.id.content), viewState.status, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.black))
                .setTextColor(resources.getColor(R.color.white))
                .show()
            viewModel.statusDisplayed()
        }

        if (viewState.isLoading) {
            singleStepUIReadyIdlingResource?.increment()
            multiStepUIReadyIdlingResource?.increment()
        } else {
            singleStepUIReadyIdlingResource?.decrement()
        }

        when (viewState.initializationType) {
            InitializationType.Normal -> {
                initializationRadioGroup.check(R.id.normal_initialization_button)
            }
            InitializationType.Deferred -> {
                initializationRadioGroup.check(R.id.deferred_initialization_button)
            }
        }

        when (viewState.customer) {
            CheckoutCustomer.Guest -> customerRadioGroup.check(R.id.guest_customer_button)
            CheckoutCustomer.New -> customerRadioGroup.check(R.id.new_customer_button)
            else -> customerRadioGroup.check(R.id.returning_customer_button)
        }

        if (viewState.linkEnabled) {
            linkRadioGroup.check(R.id.link_on_button)
        } else {
            linkRadioGroup.check(R.id.link_off_button)
        }

        if (viewState.googlePayEnabled) {
            googlePayRadioGroup.check(R.id.google_pay_on_button)
        } else {
            googlePayRadioGroup.check(R.id.google_pay_off_button)
        }

        currencySpinner.onItemSelectedListener = null
        merchantCountrySpinner.onItemSelectedListener = null

        currencySpinner.setSelection(viewState.currencyIndex)
        merchantCountrySpinner.setSelection(viewState.merchantCountryIndex)

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position != viewState.currencyIndex) {
                    viewModel.updateCurrency(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        merchantCountrySpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position != viewState.merchantCountryIndex) {
                    viewModel.updateMerchantCountry(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        when (viewState.checkoutMode) {
            CheckoutMode.Payment -> modeRadioGroup.check(R.id.mode_payment_button)
            CheckoutMode.PaymentWithSetup -> modeRadioGroup.check(R.id.mode_payment_with_setup_button)
            CheckoutMode.Setup -> modeRadioGroup.check(R.id.mode_setup_button)
        }

        when (viewState.shipping) {
            Shipping.On -> shippingRadioGroup.check(R.id.shipping_on_button)
            Shipping.OnWithDefaults -> shippingRadioGroup.check(R.id.shipping_on_with_defaults_button)
            Shipping.Off -> shippingRadioGroup.check(R.id.shipping_off_button)
        }

        if (viewState.setDefaultBillingAddress) {
            defaultBillingRadioGroup.check(R.id.default_billing_on_button)
        } else {
            defaultBillingRadioGroup.check(R.id.default_billing_off_button)
        }

        if (viewState.setAutomaticPaymentMethods) {
            automaticPmGroup.check(R.id.automatic_pm_on_button)
        } else {
            automaticPmGroup.check(R.id.automatic_pm_off_button)
        }

        if (viewState.setDelayedPaymentMethods) {
            allowsDelayedPaymentMethodsRadioGroup.check(R.id.allowsDelayedPaymentMethods_on_button)
        } else {
            allowsDelayedPaymentMethodsRadioGroup.check(R.id.allowsDelayedPaymentMethods_off_button)
        }

        paymentMethodIcon.setImageDrawable(null)
        paymentMethod.setText(R.string.loading)

        if (viewState.isConfigured && viewState.paymentOption != null) {
            val iconDrawable = viewState.paymentOption.icon()
            paymentMethodIcon.setImageDrawable(iconDrawable)

            paymentMethod.text = viewState.paymentOption.label
            customCheckoutButton.isEnabled = true
        } else {
            // TODO is this right?
            if (viewState.isConfigured) {
                paymentMethod.setText(R.string.select)
            } else {
                paymentMethod.setText(R.string.loading)
            }

            paymentMethodIcon.setImageDrawable(null)
        }

        customLabelTextField.apply {
            removeTextChangedListener(this@PaymentSheetPlaygroundActivity.customLabelTextWatcher)
            setText(viewState.customLabel)
            addTextChangedListener(this@PaymentSheetPlaygroundActivity.customLabelTextWatcher)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val shouldUseDarkMode = intent.extras?.getBoolean(FORCE_DARK_MODE_EXTRA)
        if (shouldUseDarkMode != null) {
            val mode =
                if (shouldUseDarkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            AppCompatDelegate.setDefaultNightMode(mode)
        }
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        paymentSheet = PaymentSheet(
            activity = this,
            callback = viewModel::onPaymentSheetResult,
        )

        flowController = PaymentSheet.FlowController.create(
            activity = this,
            paymentOptionCallback = viewModel::onPaymentOption,
            paymentResultCallback = viewModel::onPaymentSheetResult,
        )

        addressLauncher = AddressLauncher(this, ::onAddressLauncherResult)

        viewBinding.configureUi()

        viewModel.viewState.filterNotNull().asLiveData().observe(this) {
            viewBinding.render(it)
        }

        viewModel.viewState.mapNotNull { it?.readyForCheckout }.asLiveData().observe(this) { isReady ->
            if (isReady) {
                configureCustomCheckout()
            }
        }

        addMenuProvider(menuProvider)
    }

    private fun disableViews() {
        viewBinding.completeCheckoutButton.isEnabled = false
        viewBinding.customCheckoutButton.isEnabled = false
        viewBinding.paymentMethod.isClickable = false
    }

    private fun startCompleteCheckout() {
        val viewState = viewModel.viewState.value ?: return
        val configuration = makeConfiguration(viewState)

        if (viewState.initializationType == InitializationType.Normal) {
            val clientSecret = requireNotNull(viewState.clientSecret)
            viewBinding.customLabelTextField.clearFocus()

            if (viewState.checkoutMode == CheckoutMode.Setup) {
                paymentSheet.presentWithSetupIntent(
                    setupIntentClientSecret = clientSecret,
                    configuration = configuration
                )
            } else {
                paymentSheet.presentWithPaymentIntent(
                    paymentIntentClientSecret = clientSecret,
                    configuration = configuration
                )
            }
        } else {
            val mode = when (viewState.checkoutMode) {
                CheckoutMode.Setup -> {
                    PaymentSheet.IntentConfiguration.Mode.Setup(
                        currency = "usd",
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                    )
                }
                CheckoutMode.Payment -> {
                    PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 12345,
                        currency = "usd",
                    )
                }
                CheckoutMode.PaymentWithSetup -> {
                    PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 12345,
                        currency = "usd",
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                    )
                }
            }

            val initMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = mode,
                    customer = viewState.customerConfig?.id,
                    paymentMethodTypes = viewState.paymentMethodTypes,
                )
            )

            paymentSheet.present(
                mode = initMode,
                configuration = configuration,
            )
        }
    }

    private fun startShippingAddressCollection() {
        val builder = AddressLauncher.Configuration.Builder()
        builder.googlePlacesApiKey(settings.googlePlacesApiKey)
        if (viewBinding.shippingAddressDefaultRadioGroup.checkedRadioButtonId ==
            viewBinding.shippingAddressDefaultOnButton.id) {
            builder.address(
                AddressDetails(
                    name = "Theo Parker",
                    address = PaymentSheet.Address(
                        city = "South San Francisco",
                        country = "United States",
                        line1 = "354 Oyster Point Blvd",
                        state = "CA",
                        postalCode = "94080",
                    ),
                    phoneNumber = "5555555555",
                    isCheckboxSelected = true
                )
            )
        }
        if (viewBinding.shippingAddressCountriesGroup.checkedRadioButtonId ==
            viewBinding.shippingAddressCountriesPartialButton.id) {
            builder.allowedCountries(
                setOf("US", "CA", "AU", "GB", "FR", "JP", "KR")
            )
        }
        val phone = when (viewBinding.shippingAddressPhoneRadioGroup.checkedRadioButtonId) {
            viewBinding.shippingAddressPhoneRequiredButton.id -> {
                AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.REQUIRED
            }
            viewBinding.shippingAddressPhoneOptionalButton.id -> {
                AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.OPTIONAL
            }
            viewBinding.shippingAddressPhoneHiddenButton.id -> {
                AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN
            }
            else -> {
                AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.OPTIONAL
            }
        }
        val checkboxLabel = if (viewBinding.shippingAddressCheckboxLabel.text.isNotBlank()) {
            viewBinding.shippingAddressCheckboxLabel.text.toString()
        } else {
            getString(R.string.stripe_paymentsheet_address_element_same_as_shipping)
        }
        builder.additionalFields(
            AddressLauncher.AdditionalFieldsConfiguration(
                phone = phone,
                checkboxLabel = checkboxLabel
            )
        )
        if (viewBinding.shippingAddressButtonTitle.text.isNotBlank()) {
            builder.buttonTitle(viewBinding.shippingAddressButtonTitle.text.toString())
        }
        if (viewBinding.shippingAddressTitle.text.isNotBlank()) {
            builder.title(viewBinding.shippingAddressTitle.text.toString())
        }
        addressLauncher.present(
            publishableKey = PaymentConfiguration.getInstance(this).publishableKey,
            configuration = builder.build()
        )
    }

    private fun configureCustomCheckout() {
        val viewState = viewModel.viewState.value ?: return
        val configuration = makeConfiguration(viewState)

        if (viewState.initializationType == InitializationType.Normal) {
            val clientSecret = requireNotNull(viewState.clientSecret)

            val mode = if (viewState.checkoutMode == CheckoutMode.Payment) {
                PaymentSheet.InitializationMode.PaymentIntent(clientSecret)
            } else {
                PaymentSheet.InitializationMode.SetupIntent(clientSecret)
            }

            flowController.configure(
                mode = mode,
                configuration = configuration,
                callback = ::onConfigured,
            )
        } else {
            val mode = when (viewState.checkoutMode) {
                CheckoutMode.Setup -> {
                    PaymentSheet.IntentConfiguration.Mode.Setup(
                        currency = "usd",
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                    )
                }
                CheckoutMode.Payment -> {
                    PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 12345,
                        currency = "usd",
                    )
                }
                CheckoutMode.PaymentWithSetup -> {
                    PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 12345,
                        currency = "usd",
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                    )
                }
            }

            val initMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = mode,
                    customer = viewState.customerConfig?.id,
                    paymentMethodTypes = viewState.paymentMethodTypes,
                )
            )

            flowController.configure(
                mode = initMode,
                configuration = configuration,
                callback = ::onConfigured,
            )
        }
    }

    private fun makeConfiguration(
        viewState: PaymentSheetPlaygroundViewState,
    ): PaymentSheet.Configuration {
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
        ).takeIf { viewState.setDefaultBillingAddress }

        val appearance = intent.extras?.getParcelable(APPEARANCE_EXTRA) ?: AppearanceStore.state

        val customPrimaryButtonLabel = viewState.customLabel?.takeUnless { it.isBlank() }

        return PaymentSheet.Configuration(
            merchantDisplayName = merchantName,
            customer = viewState.customerConfig,
            googlePay = viewState.googlePayConfig,
            defaultBillingDetails = defaultBilling,
            shippingDetails = shippingAddress,
            allowsDelayedPaymentMethods = viewState.setDelayedPaymentMethods,
            allowsPaymentMethodsRequiringShippingAddress = viewState.allowsPaymentMethodsRequiringShippingAddress,
            appearance = appearance,
            primaryButtonLabel = customPrimaryButtonLabel,
        )
    }

    private fun onConfigured(success: Boolean, error: Throwable?) {
        if (success) {
            multiStepUIReadyIdlingResource?.decrement()
        }

        // TODO Enable button
        viewModel.onConfigured(
            paymentOption = flowController.getPaymentOption(),
            error = error,
        )
    }

    private fun showAppearancePicker() {
        val bottomSheet = AppearanceBottomSheetDialogFragment.newInstance()
        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    }

    private fun onAddressLauncherResult(addressLauncherResult: AddressLauncherResult) {
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
        viewBinding.shippingAddressContainer.visibility = View.VISIBLE
        when (addressLauncherResult) {
            is AddressLauncherResult.Succeeded -> {
                shippingAddress = addressLauncherResult.address
                val address = addressLauncherResult.address.address
                viewBinding.shippingAddressName.text = addressLauncherResult.address.name
                viewBinding.shippingAddressDetails.text = address?.let {
                    """
                        ${address.line1}
                        ${address.city},
                        ${address.state}, ${address.country}, ${address.postalCode}
                        ${shippingAddress?.phoneNumber}
                    """.trimIndent()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    viewBinding.playground.scrollToDescendant(viewBinding.shippingAddressDetails)
                }
            }
            is AddressLauncherResult.Canceled -> {
                viewBinding.shippingAddressContainer.visibility = View.GONE
            }
        }
    }

    /**
     * Only called from test, creates and returns a [IdlingResource].
     */
    @VisibleForTesting
    fun getMultiStepReadyIdlingResource(): IdlingResource {
        if (multiStepUIReadyIdlingResource == null) {
            multiStepUIReadyIdlingResource =
                CountingIdlingResource("multiStepUIReadyIdlingResource")
        }
        return multiStepUIReadyIdlingResource!!
    }

    @VisibleForTesting
    fun getSingleStepReadyIdlingResource(): IdlingResource {
        if (singleStepUIReadyIdlingResource == null) {
            singleStepUIReadyIdlingResource =
                CountingIdlingResource("singleStepUIReadyIdlingResource")
        }
        return singleStepUIReadyIdlingResource!!
    }

    companion object {
        const val FORCE_DARK_MODE_EXTRA = "ForceDark"
        const val APPEARANCE_EXTRA = "Appearance"
        const val USE_SNAPSHOT_RETURNING_CUSTOMER_EXTRA = "UseSnapshotReturningCustomer"
        const val SUPPORTED_PAYMENT_METHODS_EXTRA = "SupportedPaymentMethods"
        private const val merchantName = "Example, Inc."
    }
}
