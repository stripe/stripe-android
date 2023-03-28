package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldErrorController
import kotlin.math.exp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardDetailsSectionController(
    context: Context,
    initialValues: Map<IdentifierSpec, String?>,
    viewOnlyFields: Set<IdentifierSpec>,
    collectName: Boolean = false,
) : SectionFieldErrorController {

    internal val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("card_detail"),
        context,
        initialValues,
        viewOnlyFields,
        collectName,
    )

    internal val isCardScanEnabled = cardDetailsElement.isCardScanEnabled
    internal val isStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()

    override val error = cardDetailsElement.controller.error

    // The following functions are only for testing purposes.
    @RestrictTo(RestrictTo.Scope.TESTS)
    fun setName(name: String) =
        cardDetailsElement.controller.nameElement?.controller?.onValueChange(name)

    @RestrictTo(RestrictTo.Scope.TESTS)
    fun setCardNumber(number: String) =
        cardDetailsElement.controller.numberElement?.controller?.onValueChange(number)

    @RestrictTo(RestrictTo.Scope.TESTS)
    fun setExpirationDate(expirationDate: String) =
        cardDetailsElement.controller.expirationDateElement?.controller?.onValueChange(
            expirationDate
        )

    @RestrictTo(RestrictTo.Scope.TESTS)
    fun setCVC(cvc: String) =
        cardDetailsElement.controller.cvcElement?.controller?.onValueChange(cvc)

}
