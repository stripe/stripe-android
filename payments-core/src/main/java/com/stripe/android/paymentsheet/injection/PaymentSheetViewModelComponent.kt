package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.analytics.EventReporter
import dagger.BindsInstance
import dagger.Component
import javax.inject.Qualifier
import javax.inject.Singleton

@Singleton
@Component(modules = [PaymentSheetViewModelModule::class])
internal interface PaymentSheetViewModelComponent {
    val viewModel: PaymentSheetViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun starterArgs(starterArgs: PaymentSheetContract.Args): Builder

        @BindsInstance
        fun eventReporter(eventReporter: EventReporter): Builder

        fun build(): PaymentSheetViewModelComponent
    }
}

/**
 * [Qualifier] for coroutine context used for IO.
 */
@Qualifier
annotation class IOContext
