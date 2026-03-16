package com.stripe.android.stripecardscan.di

import android.app.Application
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.stripecardscan.cardscan.CardScanActivity
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanEventsReporter
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CardScanModule::class,
        CoroutineContextModule::class,
    ]
)
internal interface CardScanComponent {
    val cardScanEventsReporter: CardScanEventsReporter

    fun inject(activity: CardScanActivity)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun configuration(cardScanConfiguration: CardScanConfiguration): Builder

        fun build(): CardScanComponent
    }
}
