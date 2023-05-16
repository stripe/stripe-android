package com.stripe.android.paymentsheet.wallet.sheet

import com.stripe.android.paymentsheet.customer.CustomerAdapter
import com.stripe.android.paymentsheet.customer.StripeCustomerAdapter
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
internal abstract class SavedPaymentMethodsSheetAbstractModule {
    @Binds
    @Singleton
    abstract fun bindCustomerAdapter(adapter: StripeCustomerAdapter): CustomerAdapter
}
