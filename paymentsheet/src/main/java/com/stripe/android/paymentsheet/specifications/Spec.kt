package com.stripe.android.paymentsheet.specifications

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize

/**
 * Parcelable identifier of the form that should be displayed in a
 * ComposeFormDataCollectionFragment, passed to the fragment in the arguments bundle.
 *
 * This is needed to avoid a circular dependency from the 'paymentsheet' module to 'payments-core'.
 * Once PaymentSheet code has been moved into 'paymentsheet', SupportedPaymentMethod can be used
 * instead.
 */
@Parcelize
enum class FormType(val type: String) : Parcelable {
    Bancontact("bancontact"),
    Sofort("sofort");

    fun getFormSpec() = when (this) {
        Bancontact -> bancontact
        Sofort -> sofort
    }
}

/**
 * This class is used to define different forms full of fields.
 */
data class FormSpec(
    val layout: LayoutSpec,
    val paramKey: MutableMap<String, Any?>,
)

/**
 * This is a data representation of the layout of UI fields on the screen.
 */
data class LayoutSpec(val items: List<FormItemSpec>)

/**
 * This uniquely identifies a element in the form.
 */
data class IdentifierSpec(val value: String)

/**
 * Identifies a field that can be made optional.
 */
interface OptionalItemSpec {
    val identifier: IdentifierSpec
}
/**
 * This is used to define each section in the visual form layout specification
 */

sealed class FormItemSpec {
    data class SectionSpec(
        override val identifier: IdentifierSpec,
        val field: SectionFieldSpec
    ) : FormItemSpec(), OptionalItemSpec

    /**
     * This is for elements that do not receive user input
     */
    data class MandateTextSpec(
        override val identifier: IdentifierSpec,
        @StringRes val stringResId: Int,
        val color: Color
    ) : FormItemSpec(), OptionalItemSpec

    /**
     * This is an element that will make elements (as specified by identifer hidden
     * when save for future use is unchecked)
     */
    data class SaveForFutureUseSpec(
        val identifierRequiredForFutureUse: List<OptionalItemSpec>
    ) : FormItemSpec(), OptionalItemSpec {
        override val identifier = IdentifierSpec("save_for_future_use")
    }
}

/**
 * This represents a field in a section.
 */
sealed class SectionFieldSpec(val identifier: IdentifierSpec) {
    object Name : SectionFieldSpec(IdentifierSpec("name"))

    object Email : SectionFieldSpec(IdentifierSpec("email"))

    object Country : SectionFieldSpec(IdentifierSpec("country"))
}
