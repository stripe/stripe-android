package com.stripe.android.challenge.warmer

import androidx.annotation.RestrictTo
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object PassiveChallengeWarmerModule {
    @Provides
    fun providePassiveChallengeWarmer(): PassiveChallengeWarmer {
        return DefaultPassiveChallengeWarmer()
    }
}