package com.stripe.android.challenge.confirmation.di

import com.stripe.android.challenge.confirmation.ConfirmationChallengeBridgeHandler
import com.stripe.android.challenge.confirmation.DefaultConfirmationChallengeBridgeHandler
import com.stripe.android.core.injection.ENABLE_LOGGING
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal interface IntentConfirmationChallengeModule {

    @Binds
    fun bindBridgeHandler(
        bridgeHandler: DefaultConfirmationChallengeBridgeHandler
    ): ConfirmationChallengeBridgeHandler

    companion object {
        @Provides
        @Named(ENABLE_LOGGING)
        fun provideEnableLogging(): Boolean = true
    }
}
