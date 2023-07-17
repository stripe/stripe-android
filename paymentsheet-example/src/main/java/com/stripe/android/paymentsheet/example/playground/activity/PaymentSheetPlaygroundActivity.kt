package com.stripe.android.paymentsheet.example.playground.activity

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.MenuProvider
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.AddressLauncherResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.databinding.ActivityPaymentSheetPlaygroundBinding
import com.stripe.android.paymentsheet.example.playground.model.BillingCollectionMode
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.model.InitializationType
import com.stripe.android.paymentsheet.example.playground.model.Shipping
import com.stripe.android.paymentsheet.example.playground.model.Toggle
import com.stripe.android.paymentsheet.example.playground.viewmodel.ConfirmIntentEndpointException
import com.stripe.android.paymentsheet.example.playground.viewmodel.ConfirmIntentNetworkException
import com.stripe.android.paymentsheet.example.playground.viewmodel.PaymentSheetPlaygroundViewModel
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.launch
import java.util.Locale
import com.stripe.android.uicore.R as UiCoreR

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
    val viewModel: PaymentSheetPlaygroundViewModel by viewModels()

    private val customer: CheckoutCustomer
        get() = when (viewBinding.customerRadioGroup.checkedRadioButtonId) {
            R.id.guest_customer_button -> CheckoutCustomer.Guest
            R.id.new_customer_button -> {
                viewModel.temporaryCustomerId?.let {
                    CheckoutCustomer.WithId(it)
                } ?: CheckoutCustomer.New
            }
            else -> {
                val useSnapshotCustomer = intent.extras?.get(
                    USE_SNAPSHOT_RETURNING_CUSTOMER_EXTRA
                ) as Boolean?
                if (useSnapshotCustomer != null && useSnapshotCustomer) {
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

    private val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration
        get() = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = collectName.asBillingDetailsCollectionConfigurationMode,
            email = collectEmail.asBillingDetailsCollectionConfigurationMode,
            phone = collectPhone.asBillingDetailsCollectionConfigurationMode,
            address = collectAddress.asBillingAddressCollectionConfigurationMode,
            attachDefaultsToPaymentMethod = attachDefaultBillingAddress
        )

    private val currency: CheckoutCurrency
        get() = CheckoutCurrency(stripeSupportedCurrencies[viewBinding.currencySpinner.selectedItemPosition])

    private val merchantCountryCode: CountryCode
        get() = countryCurrencyPairs[viewBinding.merchantCountrySpinner.selectedItemPosition].first.code

    private val mode: CheckoutMode
        get() = when (viewBinding.modeRadioGroup.checkedRadioButtonId) {
            R.id.mode_payment_button -> CheckoutMode.Payment
            R.id.mode_payment_with_setup_button -> CheckoutMode.PaymentWithSetup
            else -> CheckoutMode.Setup
        }

    private val linkEnabled: Boolean
        get() = viewBinding.linkRadioGroup.checkedRadioButtonId == R.id.link_on_button

    private val shipping: Shipping
        get() = when (viewBinding.shippingRadioGroup.checkedRadioButtonId) {
            R.id.shipping_on_button -> Shipping.On
            R.id.shipping_on_with_defaults_button -> Shipping.OnWithDefaults
            else -> Shipping.Off
        }

    private val setDefaultShippingAddress: Boolean
        get() = shipping == Shipping.OnWithDefaults

    private val setDefaultBillingAddress: Boolean
        get() = viewBinding.defaultBillingRadioGroup.checkedRadioButtonId == R.id.default_billing_on_button

    private val setAutomaticPaymentMethods: Boolean
        get() = viewBinding.automaticPmGroup.checkedRadioButtonId == R.id.automatic_pm_on_button

    private val setDelayedPaymentMethods: Boolean
        get() = viewBinding.allowsDelayedPaymentMethodsRadioGroup.checkedRadioButtonId == R.id.allowsDelayedPaymentMethods_on_button

    private val attachDefaultBillingAddress: Boolean
        get() = viewBinding.attachDefaultsRadioGroup.checkedRadioButtonId == R.id.attach_defaults_on_button

    private val collectName: BillingCollectionMode
        get() = when (viewBinding.collectNameRadioGroup.checkedRadioButtonId) {
            R.id.collect_name_radio_never -> BillingCollectionMode.Never
            R.id.collect_name_radio_always -> BillingCollectionMode.Always
            else -> BillingCollectionMode.Auto
        }

    private val collectEmail: BillingCollectionMode
        get() = when (viewBinding.collectEmailRadioGroup.checkedRadioButtonId) {
            R.id.collect_email_radio_never -> BillingCollectionMode.Never
            R.id.collect_email_radio_always -> BillingCollectionMode.Always
            else -> BillingCollectionMode.Auto
        }

    private val collectPhone: BillingCollectionMode
        get() = when (viewBinding.collectPhoneRadioGroup.checkedRadioButtonId) {
            R.id.collect_phone_radio_never -> BillingCollectionMode.Never
            R.id.collect_phone_radio_always -> BillingCollectionMode.Always
            else -> BillingCollectionMode.Auto
        }

    private val collectAddress: BillingCollectionMode
        get() = when (viewBinding.collectAddressRadioGroup.checkedRadioButtonId) {
            R.id.collect_address_radio_never -> BillingCollectionMode.Never
            R.id.collect_address_radio_full -> BillingCollectionMode.Always
            else -> BillingCollectionMode.Auto
        }

    private val settings by lazy {
        Settings(this)
    }

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var flowController: PaymentSheet.FlowController
    private lateinit var addressLauncher: AddressLauncher
    private var shippingAddress: AddressDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val shouldUseDarkMode = intent.extras?.get(FORCE_DARK_MODE_EXTRA) as Boolean?
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
            createIntentCallback = { paymentMethod, shouldSavePaymentMethod ->
                viewModel.createAndConfirmIntent(
                    paymentMethodId = paymentMethod.id!!,
                    shouldSavePaymentMethod = shouldSavePaymentMethod,
                    merchantCountryCode = merchantCountryCode.value,
                    mode = mode.value,
                    returnUrl = returnUrl,
                    backendUrl = settings.playgroundBackendUrl,
                )
            },
            paymentResultCallback = ::onPaymentSheetResult,
        )

        flowController = PaymentSheet.FlowController.create(
            activity = this,
            paymentOptionCallback = ::onPaymentOption,
            createIntentCallback = { paymentMethod, shouldSavePaymentMethod ->
                viewModel.createAndConfirmIntent(
                    paymentMethodId = paymentMethod.id!!,
                    shouldSavePaymentMethod = shouldSavePaymentMethod,
                    merchantCountryCode = merchantCountryCode.value,
                    mode = mode.value,
                    returnUrl = returnUrl,
                    backendUrl = settings.playgroundBackendUrl,
                )
            },
            paymentResultCallback = ::onPaymentSheetResult,
        )

        addressLauncher = AddressLauncher(this, ::onAddressLauncherResult)

        val initializationTypes = InitializationType.values().map { it.value }
        viewBinding.initializationTypeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            initializationTypes,
        )

        viewBinding.initializationTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val type = InitializationType.values()[position]
                viewModel.initializationType.value = type
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nothing to do here
            }
        }

        viewBinding.modeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val checkoutMode = when (checkedId) {
                R.id.mode_payment_button -> CheckoutMode.Payment
                R.id.mode_payment_with_setup_button -> CheckoutMode.PaymentWithSetup
                R.id.mode_setup_button -> CheckoutMode.Setup
                else -> error("🤔")
            }
            viewModel.checkoutMode.value = checkoutMode
        }

        viewBinding.currencySpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                stripeSupportedCurrencies
            )

        viewBinding.merchantCountrySpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                countryCurrencyPairs.map { it.first }
            )

        viewBinding.resetButton.setOnClickListener {
            PaymentSheet.resetCustomer(this)

            setToggles(
                initialization = Toggle.Initialization.default.toString(),
                customer = Toggle.Customer.default.toString(),
                link = Toggle.Link.default as Boolean,
                googlePay = Toggle.GooglePay.default as Boolean,
                currency = Toggle.Currency.default.toString(),
                merchantCountryCode = Toggle.MerchantCountryCode.default.toString(),
                mode = Toggle.Mode.default.toString(),
                shippingAddress = Toggle.ShippingAddress.default.toString(),
                setDefaultBillingAddress = Toggle.SetDefaultBillingAddress.default as Boolean,
                setAutomaticPaymentMethods = Toggle.SetAutomaticPaymentMethods.default as Boolean,
                setDelayedPaymentMethods = Toggle.SetDelayedPaymentMethods.default as Boolean,
                attachDefaultBillingAddress = Toggle.AttachDefaults.default as Boolean,
                collectName = Toggle.CollectName.default.toString(),
                collectEmail = Toggle.CollectEmail.default.toString(),
                collectPhone = Toggle.CollectPhone.default.toString(),
                collectAddress = Toggle.CollectAddress.default.toString(),
            )

            viewBinding.customLabelTextField.text.clear()
        }

        viewBinding.reloadButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.storeToggleState(
                    customer = customer.value,
                    link = linkEnabled,
                    googlePay = googlePayConfig != null,
                    currency = currency.value,
                    merchantCountryCode = merchantCountryCode.value,
                    mode = mode.value,
                    shipping = shipping.value,
                    setDefaultBillingAddress = setDefaultBillingAddress,
                    setAutomaticPaymentMethods = setAutomaticPaymentMethods,
                    setDelayedPaymentMethods = setDelayedPaymentMethods,
                    attachDefaultBillingAddress = attachDefaultBillingAddress,
                    collectName = collectName.value,
                    collectEmail = collectEmail.value,
                    collectPhone = collectPhone.value,
                    collectAddress = collectAddress.value,
                )

                viewModel.prepareCheckout(
                    customer = customer,
                    currency = currency,
                    merchantCountry = merchantCountryCode,
                    mode = mode,
                    linkEnabled = linkEnabled,
                    setShippingAddress = setDefaultShippingAddress,
                    setAutomaticPaymentMethod = setAutomaticPaymentMethods,
                    backendUrl = settings.playgroundBackendUrl,
                    supportedPaymentMethods = intent.extras?.getStringArray(SUPPORTED_PAYMENT_METHODS_EXTRA)?.toList(),
                )
            }
        }

        viewBinding.completeCheckoutButton.setOnClickListener {
            startCompleteCheckout()
        }

        viewBinding.customCheckoutButton.setOnClickListener {
            flowController.shippingDetails = shippingAddress
            flowController.confirm()
        }

        viewBinding.shippingAddressButton.setOnClickListener {
            startShippingAddressCollection()
        }

        viewBinding.shippingAddressContainer.visibility = View.GONE

        viewBinding.paymentMethod.setOnClickListener {
            viewBinding.customLabelTextField.clearFocus()
            flowController.shippingDetails = shippingAddress
            flowController.presentPaymentOptions()
        }

        viewModel.status.observe(this) {
            Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(resources.getColor(R.color.black))
                .setTextColor(resources.getColor(R.color.white))
                .show()
        }

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.isInvisible = !it
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.readyToCheckout.collect { isReady ->
                    if (isReady) {
                        viewBinding.completeCheckoutButton.isEnabled = true
                        configureCustomCheckout()
                    } else {
                        disableViews()
                    }
                }
            }
        }

        viewBinding.merchantCountrySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewBinding.currencySpinner.setSelection(
                        stripeSupportedCurrencies.indexOf(
                            countryCurrencyPairs[position].second
                        )
                    )

                    // when the merchant changes, so the new customer id
                    // created might not match the previous new customer
                    viewModel.temporaryCustomerId = null
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        disableViews()

        addMenuProvider(menuProvider)

        lifecycleScope.launch {
            val savedToggles = viewModel.getSavedToggleState()
            setToggles(
                initialization = savedToggles.initialization,
                customer = savedToggles.customer,
                link = savedToggles.link,
                googlePay = savedToggles.googlePay,
                currency = savedToggles.currency,
                merchantCountryCode = savedToggles.merchantCountryCode,
                mode = savedToggles.mode,
                shippingAddress = savedToggles.shippingAddress,
                setAutomaticPaymentMethods = savedToggles.setAutomaticPaymentMethods,
                setDelayedPaymentMethods = savedToggles.setDelayedPaymentMethods,
                setDefaultBillingAddress = savedToggles.setDefaultBillingAddress,
                attachDefaultBillingAddress = savedToggles.attachDefaults,
                collectName = savedToggles.collectName,
                collectEmail = savedToggles.collectEmail,
                collectPhone = savedToggles.collectPhone,
                collectAddress = savedToggles.collectAddress,
            )
        }
    }

    private fun setToggles(
        initialization: String?,
        customer: String?,
        link: Boolean,
        googlePay: Boolean,
        currency: String?,
        merchantCountryCode: String,
        mode: String?,
        shippingAddress: String,
        setDefaultBillingAddress: Boolean,
        setAutomaticPaymentMethods: Boolean,
        setDelayedPaymentMethods: Boolean,
        attachDefaultBillingAddress: Boolean,
        collectName: String,
        collectEmail: String,
        collectPhone: String,
        collectAddress: String,
    ) {
        val initializationTypes = InitializationType.values().map { it.value }
        viewBinding.initializationTypeSpinner.setSelection(
            initializationTypes.indexOf(initialization)
        )

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

        viewBinding.currencySpinner.setSelection(
            stripeSupportedCurrencies.indexOf(currency)
        )
        viewBinding.merchantCountrySpinner.setSelection(
            countryCurrencyPairs.map { it.first.code.value }.indexOf(merchantCountryCode)
        )

        when (mode) {
            CheckoutMode.Payment.value -> viewBinding.modeRadioGroup.check(R.id.mode_payment_button)
            CheckoutMode.PaymentWithSetup.value -> viewBinding.modeRadioGroup.check(R.id.mode_payment_with_setup_button)
            else -> viewBinding.modeRadioGroup.check(R.id.mode_setup_button)
        }

        when (shippingAddress) {
            Shipping.On.value -> viewBinding.shippingRadioGroup.check(R.id.shipping_on_button)
            Shipping.OnWithDefaults.value -> viewBinding.shippingRadioGroup.check(R.id.shipping_on_with_defaults_button)
            Shipping.Off.value -> viewBinding.shippingRadioGroup.check(R.id.shipping_off_button)
        }

        if (setDefaultBillingAddress) {
            viewBinding.defaultBillingRadioGroup.check(R.id.default_billing_on_button)
        } else {
            viewBinding.defaultBillingRadioGroup.check(R.id.default_billing_off_button)
        }

        if (setAutomaticPaymentMethods) {
            viewBinding.automaticPmGroup.check(R.id.automatic_pm_on_button)
        } else {
            viewBinding.automaticPmGroup.check(R.id.automatic_pm_off_button)
        }

        if (setDelayedPaymentMethods) {
            viewBinding.allowsDelayedPaymentMethodsRadioGroup.check(R.id.allowsDelayedPaymentMethods_on_button)
        } else {
            viewBinding.allowsDelayedPaymentMethodsRadioGroup.check(R.id.allowsDelayedPaymentMethods_off_button)
        }

        if (attachDefaultBillingAddress) {
            viewBinding.attachDefaultsRadioGroup.check(R.id.attach_defaults_on_button)
        } else {
            viewBinding.attachDefaultsRadioGroup.check(R.id.attach_defaults_off_button)
        }

        when (collectName) {
            BillingCollectionMode.Auto.value -> {
                viewBinding.collectNameRadioGroup.check(R.id.collect_name_radio_auto)
            }
            BillingCollectionMode.Always.value -> {
                viewBinding.collectNameRadioGroup.check(R.id.collect_name_radio_always)
            }
            BillingCollectionMode.Never.value -> {
                viewBinding.collectNameRadioGroup.check(R.id.collect_name_radio_never)
            }
        }

        when (collectEmail) {
            BillingCollectionMode.Auto.value -> {
                viewBinding.collectEmailRadioGroup.check(R.id.collect_email_radio_auto)
            }
            BillingCollectionMode.Always.value -> {
                viewBinding.collectEmailRadioGroup.check(R.id.collect_email_radio_always)
            }
            BillingCollectionMode.Never.value -> {
                viewBinding.collectEmailRadioGroup.check(R.id.collect_email_radio_never)
            }
        }

        when (collectPhone) {
            BillingCollectionMode.Auto.value -> {
                viewBinding.collectPhoneRadioGroup.check(R.id.collect_phone_radio_auto)
            }
            BillingCollectionMode.Always.value -> {
                viewBinding.collectPhoneRadioGroup.check(R.id.collect_phone_radio_always)
            }
            BillingCollectionMode.Never.value -> {
                viewBinding.collectPhoneRadioGroup.check(R.id.collect_phone_radio_never)
            }
        }

        when (collectAddress) {
            BillingCollectionMode.Auto.value -> {
                viewBinding.collectAddressRadioGroup.check(R.id.collect_address_radio_auto)
            }
            BillingCollectionMode.Always.value -> {
                viewBinding.collectAddressRadioGroup.check(R.id.collect_address_radio_full)
            }
            BillingCollectionMode.Never.value -> {
                viewBinding.collectAddressRadioGroup.check(R.id.collect_address_radio_never)
            }
        }
    }

    private fun disableViews() {
        viewBinding.completeCheckoutButton.isEnabled = false
        viewBinding.customCheckoutButton.isEnabled = false
        viewBinding.paymentMethod.isClickable = false
    }

    private fun startCompleteCheckout() {
        if (viewModel.initializationType.value == InitializationType.Normal) {
            val clientSecret = viewModel.clientSecret.value ?: return
            viewBinding.customLabelTextField.clearFocus()

            if (viewModel.checkoutMode.value == CheckoutMode.Setup) {
                paymentSheet.presentWithSetupIntent(
                    setupIntentClientSecret = clientSecret,
                    configuration = makeConfiguration()
                )
            } else {
                paymentSheet.presentWithPaymentIntent(
                    paymentIntentClientSecret = clientSecret,
                    configuration = makeConfiguration()
                )
            }
        } else {
            val mode = when (viewModel.checkoutMode.value) {
                CheckoutMode.Setup -> {
                    PaymentSheet.IntentConfiguration.Mode.Setup(
                        currency = currency.value,
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                    )
                }
                CheckoutMode.Payment -> {
                    PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = viewModel.amount.value,
                        currency = currency.value,
                    )
                }
                CheckoutMode.PaymentWithSetup -> {
                    PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = viewModel.amount.value,
                        currency = currency.value,
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                    )
                }
            }

            paymentSheet.presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = mode,
                    paymentMethodTypes = if (setAutomaticPaymentMethods) {
                        emptyList()
                    } else {
                        viewModel.paymentMethodTypes.value
                    },
                ),
                configuration = makeConfiguration(),
            )
        }
    }

    private fun startShippingAddressCollection() {
        val builder = AddressLauncher.Configuration.Builder()
        builder.googlePlacesApiKey(settings.googlePlacesApiKey)
        if (viewBinding.shippingAddressDefaultRadioGroup.checkedRadioButtonId ==
            viewBinding.shippingAddressDefaultOnButton.id
        ) {
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
            viewBinding.shippingAddressCountriesPartialButton.id
        ) {
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
            getString(UiCoreR.string.stripe_billing_same_as_shipping)
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
        if (viewModel.initializationType.value == InitializationType.Normal) {
            val clientSecret = viewModel.clientSecret.value ?: return

            if (viewModel.checkoutMode.value == CheckoutMode.Setup) {
                flowController.configureWithSetupIntent(
                    setupIntentClientSecret = clientSecret,
                    configuration = makeConfiguration(),
                    callback = ::onConfigured,
                )
            } else {
                flowController.configureWithPaymentIntent(
                    paymentIntentClientSecret = clientSecret,
                    configuration = makeConfiguration(),
                    callback = ::onConfigured,
                )
            }
        } else {
            val mode = when (viewModel.checkoutMode.value) {
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

            flowController.configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = mode,
                    paymentMethodTypes = if (setAutomaticPaymentMethods) {
                        emptyList()
                    } else {
                        viewModel.paymentMethodTypes.value
                    },
                ),
                configuration = makeConfiguration(),
                callback = ::onConfigured,
            )
        }
    }

    private fun makeConfiguration(): PaymentSheet.Configuration {
        val defaultBilling = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(
                line1 = "354 Oyster Point Blvd",
                line2 = null,
                city = "South San Francisco",
                state = "CA",
                postalCode = "94080",
                country = "US",
            ),
            email = "email@email.com",
            name = "Jenny Rosen",
            phone = "+18008675309"
        ).takeIf { viewBinding.defaultBillingOnButton.isChecked }

        val appearance = intent.extras?.getParcelable(APPEARANCE_EXTRA) ?: AppearanceStore.state

        val customPrimaryButtonLabel = viewBinding.customLabelTextField.text.toString().takeUnless {
            it.isBlank()
        }

        return PaymentSheet.Configuration(
            merchantDisplayName = merchantName,
            customer = viewModel.customerConfig.value,
            googlePay = googlePayConfig,
            defaultBillingDetails = defaultBilling,
            shippingDetails = shippingAddress,
            allowsDelayedPaymentMethods = viewBinding.allowsDelayedPaymentMethodsOnButton.isChecked,
            allowsPaymentMethodsRequiringShippingAddress = viewBinding.shippingOnButton.isChecked,
            appearance = appearance,
            primaryButtonLabel = customPrimaryButtonLabel,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        )
    }

    private fun onConfigured(success: Boolean, error: Throwable?) {
        if (success) {
            viewBinding.paymentMethod.isClickable = true
            onPaymentOption(flowController.getPaymentOption())
        } else {
            viewModel.status.value =
                "Failed to configure PaymentSheetFlowController: ${error?.message}"
        }
    }

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        if (paymentOption != null) {
            viewBinding.paymentMethod.text = paymentOption.label
            val iconDrawable = paymentOption.icon()
            viewBinding.paymentMethodIcon.setImageDrawable(iconDrawable)
            viewBinding.customCheckoutButton.isEnabled = true
        } else {
            viewBinding.paymentMethod.setText(R.string.select)
            viewBinding.paymentMethodIcon.setImageDrawable(null)
            viewBinding.customCheckoutButton.isEnabled = false
        }
    }

    private fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        if (paymentResult !is PaymentSheetResult.Canceled) {
            disableViews()
        }

        val status = when (paymentResult) {
            is PaymentSheetResult.Canceled -> {
                "Canceled"
            }
            is PaymentSheetResult.Completed -> {
                "Success"
            }
            is PaymentSheetResult.Failed -> {
                when (paymentResult.error) {
                    is ConfirmIntentEndpointException -> {
                        "Couldn't process your payment."
                    }
                    is ConfirmIntentNetworkException -> {
                        "No internet. Try again later."
                    }
                    else -> {
                        paymentResult.error.message
                    }
                }
            }
        }

        viewModel.status.value = status
    }

    private fun showAppearancePicker() {
        val bottomSheet = AppearanceBottomSheetDialogFragment.newInstance()
        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    }

    @SuppressLint("SetTextI18n")
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

    companion object {
        const val FORCE_DARK_MODE_EXTRA = "ForceDark"
        const val APPEARANCE_EXTRA = "Appearance"
        const val USE_SNAPSHOT_RETURNING_CUSTOMER_EXTRA = "UseSnapshotReturningCustomer"
        const val SUPPORTED_PAYMENT_METHODS_EXTRA = "SupportedPaymentMethods"
        private const val merchantName = "Example, Inc."

        private const val returnUrl = "stripesdk://payment_return_url/" +
            "com.stripe.android.paymentsheet.example"

        /**
         * This is a pairing of the countries to their default currency
         **/
        private val countryCurrencyPairs = CountryUtils.getOrderedCountries(Locale.getDefault())
            .filter { country ->
                /**
                 * Modify this list if you want to change the countries displayed in the playground.
                 */
                country.code.value in setOf("US", "GB", "AU", "FR", "IN")
            }.map { country ->
                /**
                 * Modify this statement to change the default currency associated with each
                 * country.  The currency values should match the stripeSupportedCurrencies.
                 */
                when (country.code.value) {
                    "GB" -> {
                        country to "GBP"
                    }
                    "FR" -> {
                        country to "EUR"
                    }
                    "AU" -> {
                        country to "AUD"
                    }
                    "US" -> {
                        country to "USD"
                    }
                    "IN" -> {
                        country to "INR"
                    }
                    else -> {
                        country to "USD"
                    }
                }
            }

        // List was created from: https://stripe.com/docs/currencies
        /** Modify this list if you want to change the currencies displayed in the playground **/
        private val stripeSupportedCurrencies = listOf("AUD", "EUR", "GBP", "USD", "INR", "PLN")
//            "AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS",  "AWG", "AZN", "BAM",
//            "BBD", "BDT", "BGN", "BIF", "BMD", "BND", "BOB", "BRL", "BSD", "BWP", "BYN", "BZD",
//            "CAD", "CDF", "CHF", "CLP", "CNY", "COP", "CRC", "CVE", "CZK", "DJF", "DKK", "DOP",
//            "DZD", "EGP", "ETB", "FJD", "FKP", "GEL", "GIP", "GMD", "GNF", "GTQ",
//            "GYD", "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "ISK", "JMD", "JPY",
//            "KES", "KGS", "KHR", "KMF", "KRW", "KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL",
//            "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRO", "MUR", "MVR", "MWK", "MXN",
//            "MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "PAB", "PEN", "PGK", "PHP",
//            "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR", "SBD", "SCR", "SEK",
//            "SGD", "SHP", "SLL", "SOS", "SRD", "STD", "SZL", "THB", "TJS", "TOP", "TRY", "TTD",
//            "TWD", "TZS", "UAH", "UGX", "UYU", "UZS", "VND", "VUV", "WST", "XAF", "XCD", "XOF",
//            "XPF", "YER", "ZAR", "ZMW"
    }
}
