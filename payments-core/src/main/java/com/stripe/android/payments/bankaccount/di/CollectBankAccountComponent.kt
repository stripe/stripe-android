package com.stripe.android.payments.bankaccount.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
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
        PaymentElementRequestSurfaceModule::class,
        CoreCommonModule::class
    ]
)
internal interface CollectBankAccountComponent {
    val viewModel: CollectBankAccountViewModel

    fun inject(factory: CollectBankAccountViewModel.Factory)

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            application: Application,
            @BindsInstance
            viewEffect: MutableSharedFlow<CollectBankAccountViewEffect>,
            @BindsInstance
            savedStateHandle: SavedStateHandle,
            @BindsInstance
            configuration: CollectBankAccountContract.Args,
        ): CollectBankAccountComponent
    }
}
