package com.stripe.android.paymentsheet.forms

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.StaticTextSpec
import com.stripe.android.paymentsheet.elements.billingParams

internal val BancontactRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),

    /**
     * Currently we will not support this PaymentMethod for use with PI w/SFU,
     * or SI until there is a way of retrieving valid mandates associated with a customer PM.
     *
     * The reason we are excluding it is because after PI w/SFU set or PI
     * is used, the payment method appears as a SEPA payment method attached
     * to a customer.  Without this block the SEPA payment method would
     * show in PaymentSheet.  If the user used this save payment method
     * we would have no way to know if the existing mandate was valid or how
     * to request the user to re-accept the mandate.
     *
     * SEPA Debit does support PI w/SFU and SI (both with and without a customer),
     * and it is Delayed in this configuration.
     */
    siRequirements = null,

    /**
     * This PM cannot be attached to a customer, it should be noted that it
     * will be attached as a SEPA Debit payment method and have the requirements
     * of that PaymentMethod, but for now SEPA is not supported either so we will
     * call it false.
     */
    confirmPMFromCustomer = false
)

internal val BancontactParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "bancontact",
    "billing_details" to billingParams
)

internal val bancontactNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val bancontactEmailSection =
    SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)
internal val bancontactMandate = StaticTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.stripe_paymentsheet_sepa_mandate,
    Color.Gray
)

internal val BancontactForm = LayoutSpec.create(
    bancontactNameSection,
    bancontactEmailSection,
    SaveForFutureUseSpec(
        listOf(
            bancontactEmailSection, bancontactMandate
        )
    ),
    bancontactMandate,
)
