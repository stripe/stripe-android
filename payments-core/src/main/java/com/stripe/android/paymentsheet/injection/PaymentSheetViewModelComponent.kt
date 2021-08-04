package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.googlepaylauncher.GooglePayLauncherModule
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.payments.core.injection.PaymentCommonModule
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.analytics.EventReporter
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PaymentCommonModule::class,
        PaymentSheetViewModelModule::class,
        AnalyticsRequestExecutor.DaggerModule::class,
        GooglePayLauncherModule::class
    ]
)
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
