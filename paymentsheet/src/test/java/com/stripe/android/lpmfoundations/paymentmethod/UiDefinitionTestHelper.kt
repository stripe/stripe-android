package com.stripe.android.lpmfoundations.paymentmethod

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.uicore.elements.FormElement
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal fun PaymentMethodDefinition.formElements(
    metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    paymentMethodExtraParams: PaymentMethodExtraParams? = null,
): List<FormElement> {
    val context: Context = try {
        ApplicationProvider.getApplicationContext()
    } catch (_: Throwable) {
        mock<Context> {
            whenever(it.applicationContext) doReturn it
        }
    }
    return requireNotNull(
        metadata.formElementsForCode(
            code = type.code,
            context = context,
            paymentMethodCreateParams = paymentMethodCreateParams,
            paymentMethodExtraParams = paymentMethodExtraParams,
        )
    )
}
