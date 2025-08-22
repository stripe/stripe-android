package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object HCaptchaModule {
    @Volatile
    private var hCaptchaService: HCaptchaService? = null

    @Provides
    internal fun provideHCaptchaProvider(): HCaptchaProvider {
        return DefaultHCaptchaProvider()
    }

    @Provides
    fun provideHCaptchaService(hCaptchaProvider: HCaptchaProvider): HCaptchaService {
        return hCaptchaService ?: synchronized(this) {
            hCaptchaService ?: DefaultHCaptchaService(hCaptchaProvider).also {
                this.hCaptchaService = it
            }
        }
    }
}
