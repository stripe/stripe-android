package com.stripe.android.paymentsheet.addresselement

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.fragment.app.Fragment
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration
import kotlinx.parcelize.Parcelize
import java.util.Objects

/**
 * A drop-in class that presents a bottom sheet to collect a customer's address.
 */
class AddressLauncher internal constructor(
    private val activityResultLauncher: ActivityResultLauncher<AddressElementActivityContract.Args>
) {
    @InjectorKey
    private val injectorKey: String =
        WeakMapInjectorRegistry.nextKey(requireNotNull(AddressLauncher::class.simpleName))

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
        activity.registerForActivityResult(
            AddressElementActivityContract()
        ) {
            callback.onAddressLauncherResult(it)
        }
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
        fragment.registerForActivityResult(
            AddressElementActivityContract()
        ) {
            callback.onAddressLauncherResult(it)
        }
    )

    @JvmOverloads
    fun present(
        publishableKey: String,
        configuration: Configuration = Configuration()
    ) {
        activityResultLauncher.launch(
            AddressElementActivityContract.Args(
                publishableKey,
                configuration,
                injectorKey
            )
        )
    }

    /** Configuration for [AddressLauncher] **/
    @Parcelize
    class Configuration @JvmOverloads constructor(
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

        fun copy(
            appearance: PaymentSheet.Appearance = this.appearance,
            address: AddressDetails? = this.address,
            allowedCountries: Set<String> = this.allowedCountries,
            buttonTitle: String? = this.buttonTitle,
            additionalFields: AdditionalFieldsConfiguration? = this.additionalFields,
            title: String? = this.title,
            googlePlacesApiKey: String? = this.googlePlacesApiKey,
            autocompleteCountries: Set<String> = this.autocompleteCountries,
        ): Configuration {
            return Configuration(
                appearance = appearance,
                address = address,
                allowedCountries = allowedCountries,
                buttonTitle = buttonTitle,
                additionalFields = additionalFields,
                title = title,
                googlePlacesApiKey = googlePlacesApiKey,
                autocompleteCountries = autocompleteCountries
            )
        }

        override fun hashCode(): Int {
            return Objects.hash(
                appearance,
                address,
                allowedCountries,
                buttonTitle,
                additionalFields,
                title,
                googlePlacesApiKey,
                autocompleteCountries,
            )
        }

        override fun equals(other: Any?): Boolean {
            return other is Configuration &&
                appearance == other.appearance &&
                address == other.address &&
                allowedCountries == other.allowedCountries &&
                buttonTitle == other.buttonTitle &&
                additionalFields == other.additionalFields &&
                title == other.title &&
                googlePlacesApiKey == other.googlePlacesApiKey &&
                autocompleteCountries == other.autocompleteCountries
        }

        override fun toString(): String {
            return "AddressLauncher.Configuration(" +
                "appearance=$appearance, " +
                "address=$address, " +
                "allowedCountries=$allowedCountries, " +
                "buttonTitle=$buttonTitle, " +
                "additionalFields=$additionalFields, " +
                "title=$title, " +
                "googlePlacesApiKey=$googlePlacesApiKey, " +
                "autocompleteCountries=$autocompleteCountries)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): PaymentSheet.Appearance = appearance

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): AddressDetails? = address

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component3(): Set<String> = allowedCountries

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component4(): String? = buttonTitle

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component5(): AdditionalFieldsConfiguration? = additionalFields

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component6(): String? = title

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component7(): String? = googlePlacesApiKey

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component8(): Set<String> = autocompleteCountries

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
    class AdditionalFieldsConfiguration @JvmOverloads constructor(
        val phone: FieldConfiguration = FieldConfiguration.HIDDEN,
        val checkboxLabel: String? = null
    ) : Parcelable {

        fun copy(
            phone: FieldConfiguration = this.phone,
            checkboxLabel: String? = this.checkboxLabel,
        ): AdditionalFieldsConfiguration {
            return AdditionalFieldsConfiguration(phone, checkboxLabel)
        }

        override fun hashCode(): Int {
            return Objects.hash(phone, checkboxLabel)
        }

        override fun equals(other: Any?): Boolean {
            return other is AdditionalFieldsConfiguration &&
                phone == other.phone &&
                checkboxLabel == other.checkboxLabel
        }

        override fun toString(): String {
            return "PaymentSheet.AdditionalFieldsConfiguration(" +
                "phone=$phone, " +
                "checkboxLabel=$checkboxLabel)"
        }

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component1(): FieldConfiguration = phone

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release",
        )
        fun component2(): String? = checkboxLabel

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
        contract = AddressElementActivityContract(),
        onResult = callback::onAddressLauncherResult
    )

    return remember {
        AddressLauncher(activityResultLauncher)
    }
}
