package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration
import com.stripe.android.uicore.utils.AnimationConstants
import kotlinx.parcelize.Parcelize

/**
 * A drop-in class that presents a bottom sheet to collect a customer's address.
 */
class AddressLauncher internal constructor(
    private val application: Application,
    private val activityResultLauncher: ActivityResultLauncher<AddressElementActivityContract.Args>
) {
    /**
     * Constructor to be used when launching the address element from an Activity.
     *
     * @param activity  the Activity that is presenting the address element.
     * @param callback  called with the result after the address element is dismissed.
     */
    constructor(
        activity: ComponentActivity,
        callback: AddressLauncherResultCallback
    ) : this(
        application = activity.application,
        activityResultLauncher = activity.registerForActivityResult(
            AddressElementActivityContract
        ) {
            callback.onAddressLauncherResult(it)
        },
    )

    /**
     * Constructor to be used when launching the address element from a Fragment.
     *
     * @param fragment the Fragment that is presenting the payment sheet.
     * @param callback called with the result after the address element is dismissed.
     */
    constructor(
        fragment: Fragment,
        callback: AddressLauncherResultCallback
    ) : this(
        application = fragment.requireActivity().application,
        activityResultLauncher = fragment.registerForActivityResult(
            AddressElementActivityContract
        ) {
            callback.onAddressLauncherResult(it)
        },
    )

    @JvmOverloads
    fun present(
        publishableKey: String,
        configuration: Configuration = Configuration()
    ) {
        val args = AddressElementActivityContract.Args(
            publishableKey,
            configuration,
        )

        val options = ActivityOptionsCompat.makeCustomAnimation(
            application.applicationContext,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        activityResultLauncher.launch(args, options)
    }

    /** Configuration for [AddressLauncher] **/
    @Parcelize
    data class Configuration @JvmOverloads constructor(
        /**
         * Configuration for the look and feel of the UI
         */
        val appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),

        /**
         * The values to pre-populate shipping address fields with.
         */
        val address: AddressDetails? = null,

        /**
         * A list of two-letter country codes representing countries the customers can select.
         * If the list is empty (the default), we display all countries.
         */
        val allowedCountries: Set<String> = emptySet(),

        /**
         * The title of the primary button displayed at the bottom of the screen.
         * Defaults to "Save address".
         */
        val buttonTitle: String? = null,

        /**
         * Configuration for fields to collect in addition to the physical shipping address
         */
        val additionalFields: AdditionalFieldsConfiguration? = null,

        /**
         * Configuration for the title displayed at the top of the screen.
         * Defaults to "Address"
         */
        val title: String? = null,

        /**
         * Google Places api key used to provide autocomplete suggestions
         * When null, autocomplete is disabled.
         */
        val googlePlacesApiKey: String? = null,

        /**
         * A list of two-letter country codes that support autocomplete. Defaults to a list of
         * countries that Stripe has audited to ensure a good autocomplete experience.
         */
        val autocompleteCountries: Set<String> = setOf(
            "AU", "BE", "BR", "CA", "CH", "DE", "ES", "FR", "GB", "IE", "IT", "MX", "NO", "NL",
            "PL", "RU", "SE", "TR", "US", "ZA"
        )
    ) : Parcelable {
        /**
         * [Configuration] builder for cleaner object creation from Java.
         */
        class Builder {
            private var appearance: PaymentSheet.Appearance = PaymentSheet.Appearance()
            private var address: AddressDetails? = null
            private var allowedCountries: Set<String> = emptySet()
            private var buttonTitle: String? = null
            private var additionalFields: AdditionalFieldsConfiguration? = null
            private var title: String? = null
            private var googlePlacesApiKey: String? = null
            private var autocompleteCountries: Set<String>? = null

            fun appearance(appearance: PaymentSheet.Appearance) =
                apply { this.appearance = appearance }

            fun address(address: AddressDetails?) =
                apply { this.address = address }

            fun allowedCountries(allowedCountries: Set<String>) =
                apply { this.allowedCountries = allowedCountries }

            fun buttonTitle(buttonTitle: String?) =
                apply { this.buttonTitle = buttonTitle }

            fun additionalFields(additionalFields: AdditionalFieldsConfiguration) =
                apply { this.additionalFields = additionalFields }

            fun title(title: String?) =
                apply { this.title = title }

            fun googlePlacesApiKey(googlePlacesApiKey: String?) =
                apply { this.googlePlacesApiKey = googlePlacesApiKey }

            fun autocompleteCountries(autocompleteCountries: Set<String>) =
                apply { this.autocompleteCountries = autocompleteCountries }

            fun build() = Configuration(
                appearance,
                address,
                allowedCountries,
                buttonTitle,
                additionalFields,
                title,
                googlePlacesApiKey
            )
        }
    }

    /**
     * @param phone Configuration for the field that collects a phone number. Defaults to
     * [FieldConfiguration.HIDDEN]
     * @param checkboxLabel The label of a checkbox displayed below other fields. If null, the
     * checkbox is not displayed. Defaults to null
     */
    @Parcelize
    data class AdditionalFieldsConfiguration @JvmOverloads constructor(
        val phone: FieldConfiguration = FieldConfiguration.HIDDEN,
        val checkboxLabel: String? = null
    ) : Parcelable {
        @Parcelize
        enum class FieldConfiguration : Parcelable {
            /**
             * The field is not displayed.
             */
            HIDDEN,

            /**
             * The field is displayed but the customer can leave it blank.
             */
            OPTIONAL,

            /**
             * The field is displayed and the customer is required to fill it in.
             */
            REQUIRED
        }
    }
}

/**
 * Creates an [AddressLauncher] that is remembered across compositions.
 * This *must* be called unconditionally, as part of the initialization path.
 */
@Composable
fun rememberAddressLauncher(
    callback: AddressLauncherResultCallback
): AddressLauncher {
    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = AddressElementActivityContract,
        onResult = callback::onAddressLauncherResult
    )

    val context = LocalContext.current

    return remember {
        AddressLauncher(
            application = context.applicationContext as Application,
            activityResultLauncher = activityResultLauncher,
        )
    }
}
