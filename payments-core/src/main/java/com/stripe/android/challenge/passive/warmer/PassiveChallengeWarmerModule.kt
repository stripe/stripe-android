package com.stripe.android.challenge.passive.warmer

import androidx.annotation.RestrictTo
import com.stripe.android.hcaptcha.HCaptchaModule
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module(includes = [HCaptchaModule::class])
object PassiveChallengeWarmerModule {
    @Provides
    fun providePassiveChallengeWarmer(): PassiveChallengeWarmer {
        return DefaultPassiveChallengeWarmer()
    }
}
