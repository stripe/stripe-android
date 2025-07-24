package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object HCaptchaModule {
    @Provides
    fun provideHCaptchaService(): HCaptchaService = DefaultHCaptchaService()
}