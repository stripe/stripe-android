package com.stripe.android.checkout

import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentElement internal constructor() {

    @Composable
    fun PaymentOptionsContent() {
        TODO("Not yet implemented")
    }

    fun presentPaymentOptions() {
        TODO("Not yet implemented")
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var embeddedViewDisplaysMandateText: Boolean = true
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()

        fun embeddedViewDisplaysMandateText(
            embeddedViewDisplaysMandateText: Boolean
        ): Configuration = apply {
            this.embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText
        }

        fun billingDetailsCollectionConfiguration(
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
        ): Configuration = apply {
            this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        }

        @Parcelize
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
            val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
        ) : Parcelable

        internal fun build(): State = State(
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        )

        /**
         * Configuration for how billing details are collected during checkout.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Poko
        @Parcelize
        class BillingDetailsCollectionConfiguration(
            /**
             * How to collect the name field.
             */
            val name: CollectionMode = CollectionMode.Automatic,
            /**
             * How to collect the phone field.
             */
            val phone: CollectionMode = CollectionMode.Automatic,
            /**
             * How to collect the email field.
             */
            val email: CollectionMode = CollectionMode.Automatic,
            /**
             * How to collect the billing address.
             */
            val address: AddressCollectionMode = AddressCollectionMode.Automatic,
            /**
             * Whether the values included in [CheckoutController.Configuration]'s default billing
             * details should be attached to the payment method, including fields that aren't
             * displayed in the form.
             *
             * If `false` (the default), those values will only be used to prefill the corresponding
             * fields in the form.
             */
            val attachDefaultsToPaymentMethod: Boolean = false,
        ) : Parcelable {

            /**
             * Billing details fields collection options.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class CollectionMode {
                /**
                 * The field will be collected depending on the Payment Method's requirements.
                 */
                Automatic,

                /**
                 * The field will never be collected.
                 * If this field is required by the Payment Method, you must provide it as part of
                 * the default billing details.
                 */
                Never,

                /**
                 * The field will always be collected, even if it isn't required for the Payment
                 * Method.
                 */
                Always,
            }

            /**
             * Billing address collection options.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class AddressCollectionMode {
                /**
                 * Only the fields required by the Payment Method will be collected, this may be
                 * none.
                 */
                Automatic,

                /**
                 * Address will never be collected.
                 * If the Payment Method requires a billing address, you must provide it as part of
                 * the default billing details.
                 */
                Never,

                /**
                 * Collect the full billing address, regardless of the Payment Method requirements.
                 */
                Full,
            }
        }
    }

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class PaymentOptionDisplayData internal constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val imageLoader: suspend () -> Drawable,
        val label: String,
        val billingDetails: PaymentSheet.BillingDetails?,
        val paymentMethodType: String,
        val mandateText: AnnotatedString?,
    ) {
        private val iconDrawable: Drawable by lazy {
            DelegateDrawable(imageLoader)
        }

        val iconPainter: Painter
            @Composable
            get() = rememberDrawablePainter(iconDrawable)
    }
}
