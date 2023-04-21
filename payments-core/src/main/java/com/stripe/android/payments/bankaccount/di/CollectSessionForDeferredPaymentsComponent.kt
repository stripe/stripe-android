package com.stripe.android.payments.bankaccount.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsContract
import com.stripe.android.payments.bankaccount.ui.CollectSessionForDeferredPaymentsViewEffect
import com.stripe.android.payments.bankaccount.ui.CollectSessionForDeferredPaymentsViewModel
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        CollectSessionForDeferredPaymentsModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class
    ]
)
internal interface CollectSessionForDeferredPaymentsComponent {
    val viewModel: CollectSessionForDeferredPaymentsViewModel

    fun inject(factory: CollectSessionForDeferredPaymentsViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun viewEffect(application: MutableSharedFlow<CollectSessionForDeferredPaymentsViewEffect>): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun configuration(configuration: CollectSessionForDeferredPaymentsContract.Args): Builder

        fun build(): CollectSessionForDeferredPaymentsComponent
    }
}
