package com.stripe.android.financialconnections.screens

import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(
    modules = [BankPickerModule::class]
)
internal interface BankPickerSubcomponent {

    val viewModel: BankPickerViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: BankPickerState): Builder

        fun build(): BankPickerSubcomponent
    }
}

@Module
internal object BankPickerModule {

    @Provides
    fun providesTestClass() = TestClass()
}

class TestClass