package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Component
@Singleton
internal interface FormActivityComponent {

    val viewModel: FormActivityViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun paymentMethodMetadata(paymentMethodMetadata: PaymentMethodMetadata): Builder

        @BindsInstance
        fun selectedPaymentMethodCode(selectedPaymentMethodCode: PaymentMethodCode): Builder

        fun build(): FormActivityComponent
    }
}
