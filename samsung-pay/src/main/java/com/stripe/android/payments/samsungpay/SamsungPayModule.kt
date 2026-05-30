package com.stripe.android.payments.samsungpay

import dagger.Binds
import dagger.Module

@Module
interface SamsungPayModule {

    @Binds
    fun bindsGetSamsungPayStatus(impl: DefaultGetSamsungPayStatus): GetSamsungPayStatus
}