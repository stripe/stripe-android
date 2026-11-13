package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.stripebalance.DefaultLoadStripeBalanceFinancialAccounts
import com.stripe.android.paymentsheet.stripebalance.DefaultStripeBalanceRepository
import com.stripe.android.paymentsheet.stripebalance.LoadStripeBalanceFinancialAccounts
import com.stripe.android.paymentsheet.stripebalance.StripeBalanceRepository
import dagger.Binds
import dagger.Module

@Module
internal interface StripeBalanceModule {
    @Binds
    fun bindsStripeBalanceRepository(impl: DefaultStripeBalanceRepository): StripeBalanceRepository

    @Binds
    fun bindsLoadStripeBalanceFinancialAccounts(
        impl: DefaultLoadStripeBalanceFinancialAccounts,
    ): LoadStripeBalanceFinancialAccounts
}
