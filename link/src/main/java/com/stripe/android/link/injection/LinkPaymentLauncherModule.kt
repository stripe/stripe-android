package com.stripe.android.link.injection

import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.link.repositories.LinkRepository
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module(
    subcomponents = [
        SignUpViewModelSubcomponent::class
    ]
)
internal interface LinkPaymentLauncherModule {
    @Binds
    @Singleton
    fun bindLinkRepository(linkApiRepository: LinkApiRepository): LinkRepository
}
