package com.stripe.android.payments.samsungpay

import dagger.Binds

interface SamsungPayModule {

    @Binds
    fun bindsGetSamsungPayStatus(impl: DefaultGetSamsungPayStatus): GetSamsungPayStatus
}