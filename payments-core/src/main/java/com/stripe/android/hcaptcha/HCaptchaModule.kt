package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object HCaptchaModule {
    @Provides
    internal fun provideHCaptchaProvider(): HCaptchaProvider {
        return DefaultHCaptchaProvider()
    }

    @Provides
    fun provideHCaptchaService(
        hCaptchaProvider: HCaptchaProvider
    ): HCaptchaService {
        return DefaultHCaptchaService(hCaptchaProvider)
    }
}
