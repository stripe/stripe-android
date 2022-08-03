package com.stripe.android.payments.bankaccount.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewModel
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        CollectBankAccountModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class
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
        fun viewEffect(application: MutableSharedFlow<CollectBankAccountViewEffect>): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun configuration(configuration: CollectBankAccountContract.Args): Builder

        fun build(): CollectBankAccountComponent
    }
}
