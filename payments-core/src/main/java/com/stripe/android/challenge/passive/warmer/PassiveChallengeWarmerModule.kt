package com.stripe.android.challenge.passive.warmer

import androidx.annotation.RestrictTo
import com.stripe.android.hcaptcha.HCaptchaModule
import com.stripe.android.hcaptcha.HCaptchaService
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module(includes = [HCaptchaModule::class])
object PassiveChallengeWarmerModule {
    @Provides
    fun providePassiveChallengeWarmer(hCaptchaService: HCaptchaService): PassiveChallengeWarmer {
        return DefaultPassiveChallengeWarmer(hCaptchaService)
    }
}
