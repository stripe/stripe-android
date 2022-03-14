package com.stripe.android.payments.bankaccount

import android.app.Application
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.payments.CollectBankAccountModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        CollectBankAccountModule::class,
        LoggingModule::class
    ]
)
internal interface CollectBankAccountComponent {
    val viewModel: CollectBankAccountViewModel

    fun inject(factory: CollectBankAccountViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun configuration(configuration: CollectBankAccountContract.Args): Builder

        fun build(): CollectBankAccountComponent
    }
}
