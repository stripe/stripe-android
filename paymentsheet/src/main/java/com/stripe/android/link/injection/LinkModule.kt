package com.stripe.android.link.injection

import com.stripe.android.ApiConfiguration
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.account.DefaultLinkAccountManager
import com.stripe.android.link.account.DefaultLinkAuth
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.attestation.DefaultLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.link.repositories.LinkRepository
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface LinkModule {
    @Binds
    @LinkScope
    fun bindLinkAccountManager(linkAccountManager: DefaultLinkAccountManager): LinkAccountManager

    @Binds
    @LinkScope
    fun bindsLinkGate(linkGate: DefaultLinkGate): LinkGate

    @Binds
    @LinkScope
    fun bindsLinkAuth(linkGate: DefaultLinkAuth): LinkAuth

    @Binds
    @LinkScope
    fun bindsLinkAttestationCheck(linkAttestationCheck: DefaultLinkAttestationCheck): LinkAttestationCheck

    @Binds
    @LinkScope
    fun bindLinkRepository(linkApiRepository: LinkApiRepository): LinkRepository

    companion object {
        @Provides
        fun provideLinkLaunchMode(): LinkLaunchMode? = null

        @Provides
        @LinkScope
        fun provideApiConfiguration(configuration: LinkConfiguration): ApiConfiguration.State =
            configuration.apiConfiguration
    }
}
