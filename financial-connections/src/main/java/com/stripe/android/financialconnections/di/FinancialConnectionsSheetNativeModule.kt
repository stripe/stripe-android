package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.screens.ConsentSubcomponent
import dagger.Module

@Module(
    subcomponents = [
        ConsentSubcomponent::class
    ]
)
internal class FinancialConnectionsSheetNativeModule
