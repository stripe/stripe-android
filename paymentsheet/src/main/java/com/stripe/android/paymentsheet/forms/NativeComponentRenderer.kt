package com.stripe.android.paymentsheet.forms

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.BlikElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement

/** Native renderer capabilities addressable by Mint-owned JSON form specs. */
internal object NativeComponentRenderer {
    private val emptyFormComponents = setOf(
        "us_bank_account_collection",
        "instant_debits_collection",
        "link_card_collection",
    )

    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
    fun render(
        component: String,
        paymentMethodCode: String,
        metadata: PaymentMethodMetadata,
        argumentsFactory: UiDefinitionFactory.Arguments.Factory,
    ): List<FormElement> {
        try {
            if (component == "external_confirmation") {
                return metadata.createServerSelectedExternalFormElements(
                    code = paymentMethodCode,
                    arguments = argumentsFactory.create(metadata, requiresMandate = false),
                )
            }
            if (component == "blik_confirmation") {
                return listOf(SectionElement.wrap(BlikElement()))
            }
            if (component.startsWith("card_")) {
                val arguments = argumentsFactory.create(
                    metadata = metadata,
                    requiresMandate = component == "card_mandate",
                )
                return when (component) {
                    "card_details" -> CardFormElementRenderer.details(metadata, arguments)
                    "card_billing_details" -> CardFormElementRenderer.billingDetails(metadata, arguments)
                    "card_save_payment_method" -> CardFormElementRenderer.savePaymentMethod(metadata, arguments)
                    "card_link_inline_signup" -> CardFormElementRenderer.linkInlineSignup(metadata, arguments)
                    "card_mandate" -> CardFormElementRenderer.mandate(metadata, arguments)
                    else -> throw ServerDrivenFormRenderException(
                        ServerDrivenFormRenderException.ErrorCode.UnsupportedNativeComponent
                    )
                }
            }
            if (component in emptyFormComponents) {
                return emptyList()
            }

            throw ServerDrivenFormRenderException(
                ServerDrivenFormRenderException.ErrorCode.UnsupportedNativeComponent
            )
        } catch (error: ServerDrivenFormRenderException) {
            throw error
        } catch (error: Throwable) {
            throw ServerDrivenFormRenderException(
                errorCode = ServerDrivenFormRenderException.ErrorCode.NativeComponentFailure,
                cause = error,
            )
        }
    }
}
