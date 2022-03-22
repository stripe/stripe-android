package com.stripe.android.link.injection

import androidx.core.os.LocaleListCompat
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.link.repositories.LinkRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    subcomponents = [
        SignedInViewModelSubcomponent::class,
        SignUpViewModelSubcomponent::class
    ]
)
internal interface LinkPaymentLauncherModule {
    @Binds
    @Singleton
    fun bindLinkRepository(linkApiRepository: LinkApiRepository): LinkRepository

    companion object {

        @Provides
        @Singleton
        fun provideLocale() =
            LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)
    }
}
