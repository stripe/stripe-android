package com.stripe.android.paymentsheet.addresselement

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

/**
 * A drop-in class that presents a bottom sheet to collect a customer's address.
 */
internal class AddressLauncher internal constructor(
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
    internal data class Configuration @JvmOverloads constructor(
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
        val googlePlacesApiKey: String? = null
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
     * [FieldConfiguration.OPTIONAL]
     * @param checkboxLabel The label of a checkbox displayed below other fields. If null, the
     * checkbox is not displayed. Defaults to null
     */
    @Parcelize
    data class AdditionalFieldsConfiguration @JvmOverloads constructor(
        val phone: FieldConfiguration = FieldConfiguration.OPTIONAL,
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
