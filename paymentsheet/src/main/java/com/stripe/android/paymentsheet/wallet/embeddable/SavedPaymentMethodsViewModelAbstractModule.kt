package com.stripe.android.paymentsheet.wallet.embeddable

import com.stripe.android.paymentsheet.customer.CustomerAdapter
import com.stripe.android.paymentsheet.customer.StripeCustomerAdapter
import dagger.Binds
import dagger.Module

@Module(
)
internal abstract class SavedPaymentMethodsViewModelAbstractModule {
    @Binds
    abstract fun bindCustomerAdapter(adapter: StripeCustomerAdapter): CustomerAdapter
}
