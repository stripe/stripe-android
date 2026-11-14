package com.stripe.android.paymentsheet.addresselement

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.addresselement.analytics.DefaultAddressLauncherEventReporter
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the input of an [InlineAddressForm] and, given a publishable key, suggests addresses with
 * Stripe-hosted autocomplete as the customer types. The input is not saved across configuration
 * changes or process death, so hold this in a `ViewModel`.
 *
 * @param initialAddress the address to prefill the fields with.
 * @param allowedCountries the countries to offer, or every country if empty. An [initialAddress]
 *   in another country doesn't prefill the fields.
 * @param nameField whether to show the name field. A hidden name keeps the one in
 *   [initialAddress] in [enteredAddress] and [completeAddress].
 * @param phone whether to show the phone number field, and whether it is required.
 * @param context the context that autocomplete analytics read the app's package details from.
 * @param publishableKey the key that authenticates autocomplete requests, or null to turn
 *   autocomplete off.
 * @param coroutineScope the scope that runs autocomplete requests, such as `viewModelScope`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InlineAddressFormState(
    initialAddress: AddressDetails?,
    allowedCountries: Set<String>,
    nameField: AddressLauncher.NameField,
    phone: AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration,
    context: Context,
    publishableKey: String?,
    coroutineScope: CoroutineScope,
) {
    private val prefilledAddress = initialAddress?.takeIfCountryAllowed(allowedCountries)

    internal val formController = AddressFormController(
        initialValues = prefilledAddress?.toIdentifierMap() ?: emptyMap(),
        config = AddressLauncher.Configuration(
            allowedCountries = allowedCountries,
            additionalFields = AddressLauncher.AdditionalFieldsConfiguration(phone = phone),
            billingAddress = null,
            nameField = nameField,
        ),
        interactor = if (publishableKey == null) {
            NoAutocompleteAddressInteractor
        } else {
            createStripeHostedAutocompleteInteractor(
                context = context,
                publishableKey = publishableKey,
                coroutineScope = coroutineScope,
            )
        },
    )

    // The customer can't edit a hidden name, so the address keeps the initial one.
    private val hiddenName = initialAddress?.name.takeIf {
        nameField == AddressLauncher.NameField.Never
    }

    /**
     * The address entered so far, including incomplete and invalid fields.
     */
    val enteredAddress: StateFlow<AddressDetails> =
        formController.uncompletedFormValues.mapAsStateFlow { formValues ->
            formValues.toAddressDetails(
                hiddenName = hiddenName,
                isCheckboxSelected = null,
            )
        }

    /**
     * The entered address once every required field is complete, or null otherwise.
     */
    val completeAddress: StateFlow<AddressDetails?> =
        formController.completeFormValues.mapAsStateFlow { formValues ->
            formValues?.toAddressDetails(
                hiddenName = hiddenName,
                isCheckboxSelected = null,
            )
        }

    /**
     * Whether the entered values differ from [initialAddress].
     */
    fun hasChanges(): Boolean = formController.hasChanges()

    /**
     * Shows an error on every field that is incomplete or invalid.
     */
    fun validate() {
        formController.elements.forEach { element ->
            element.onValidationStateChanged(true)
        }
    }
}

private object NoAutocompleteAddressInteractor : AutocompleteAddressInteractor {
    override val autocompleteConfig = AutocompleteAddressInteractor.Config(
        googlePlacesApiKey = null,
        autocompleteCountries = emptySet(),
    )

    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) = Unit
}

private fun createStripeHostedAutocompleteInteractor(
    context: Context,
    publishableKey: String,
    coroutineScope: CoroutineScope,
): AutocompleteAddressInteractor {
    val requestOptions = ApiRequest.Options(apiKey = publishableKey)
    val repository = DefaultStripeAutocompleteRepository(
        stripeNetworkClient = DefaultStripeNetworkClient(),
        apiRequestFactory = ApiRequest.Factory(),
        requestOptionsProvider = { requestOptions },
    )
    val eventReporter = DefaultAddressLauncherEventReporter(
        analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(),
        analyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = context,
            publishableKey = publishableKey,
            defaultProductUsageTokens = setOf(PRODUCT_USAGE),
        ),
        durationProvider = DefaultDurationProvider.instance,
        workContext = Dispatchers.IO,
    )
    return BillingInlineAutocompleteAddressInteractor(
        placesClient = StripeHostedPlacesClientProxy(
            repository = repository,
            eventReporter = eventReporter,
        ),
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = null,
            autocompleteCountries = AUTOCOMPLETE_STRIPE_HOSTED_DEFAULT_COUNTRIES,
            isInlineAutocompleteEnabled = true,
            shouldUseStripeHostedAutocomplete = true,
        ),
        coroutineScope = coroutineScope,
    )
}

// Tells this form's autocomplete analytics apart from the Address Element sheet's.
private const val PRODUCT_USAGE = "PaymentSheet.InlineAddressForm"
