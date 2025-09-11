package com.stripe.android.challenge.warmer

import androidx.annotation.RestrictTo
import dagger.Module
import dagger.Provides
import dagger.Reusable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object PassiveChallengeWarmerModule {
    @Reusable
    @Provides
    fun providePassiveChallengeWarmer(): PassiveChallengeWarmer {
        return DefaultPassiveChallengeWarmer()
    }
}
