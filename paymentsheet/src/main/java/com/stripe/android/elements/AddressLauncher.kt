package com.stripe.android.elements

import android.app.Activity
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
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.uicore.utils.AnimationConstants
import dev.drewhamilton.poko.Poko
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
        callback: ResultCallback
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
        callback: ResultCallback
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
        configuration: Configuration = Configuration.Builder().build()
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
    @Poko
    class Configuration @JvmOverloads constructor(
        internal val appearance: Appearance = Appearance(),
        internal val address: AddressDetails? = null,
        internal val allowedCountries: Set<String> = emptySet(),
        internal val buttonTitle: String? = null,
        internal val additionalFields: AdditionalFieldsConfiguration? = null,
        internal val title: String? = null,
        internal val googlePlacesApiKey: String? = null,
        internal val autocompleteCountries: Set<String> = AUTOCOMPLETE_DEFAULT_COUNTRIES,
        internal val billingAddress: BillingDetails? = null,
    ) : Parcelable {

        /**
         * [Configuration] builder for cleaner object creation from Java.
         */
        class Builder {
            private var appearance: Appearance = Appearance()
            private var address: AddressDetails? = null
            private var allowedCountries: Set<String> = emptySet()
            private var buttonTitle: String? = null
            private var additionalFields: AdditionalFieldsConfiguration? = null
            private var title: String? = null
            private var googlePlacesApiKey: String? = null
            private var autocompleteCountries: Set<String> = AUTOCOMPLETE_DEFAULT_COUNTRIES
            private var billingAddress: BillingDetails? = null

            fun appearance(appearance: Appearance) =
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

            @AddressElementSameAsBillingPreview
            fun billingAddress(billingAddress: BillingDetails?) =
                apply { this.billingAddress = billingAddress }

            fun build() = Configuration(
                appearance = appearance,
                address = address,
                allowedCountries = allowedCountries,
                buttonTitle = buttonTitle,
                additionalFields = additionalFields,
                title = title,
                googlePlacesApiKey = googlePlacesApiKey,
                autocompleteCountries = autocompleteCountries,
                billingAddress = billingAddress,
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
    @Poko
    class AdditionalFieldsConfiguration(
        internal val phone: FieldConfiguration = FieldConfiguration.HIDDEN,
        internal val checkboxLabel: String? = null,
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

    sealed class Result : Parcelable {

        internal abstract val resultCode: Int

        @Parcelize
        @Poko
        class Succeeded internal constructor(
            val address: AddressDetails
        ) : Result() {
            override val resultCode: Int
                get() = Activity.RESULT_OK
        }

        @Parcelize
        @Poko
        class Canceled internal constructor(
            @Suppress("unused") private val irrelevantValueForGeneratedCode: Boolean = true
        ) : Result() {
            override val resultCode: Int
                get() = Activity.RESULT_CANCELED
        }
    }

    /**
     * Callback that is invoked when a [AddressLauncher.Result] is available.
     */
    fun interface ResultCallback {
        fun onAddressLauncherResult(addressLauncherResult: AddressLauncher.Result)
    }

    @RequiresOptIn(
        level = RequiresOptIn.Level.ERROR,
        message = "This API is under construction. It can be changed or removed at any time (use at your own risk)."
    )
    @Retention(AnnotationRetention.BINARY)
    annotation class AddressElementSameAsBillingPreview
}

/**
 * Creates an [AddressLauncher] that is remembered across compositions.
 * This *must* be called unconditionally, as part of the initialization path.
 */
@Composable
fun rememberAddressLauncher(
    callback: AddressLauncher.ResultCallback
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
