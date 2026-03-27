package com.stripe.android.link.injection

import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.account.DefaultLinkAccountManager
import com.stripe.android.link.account.DefaultLinkAuth
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.attestation.DefaultLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface LinkModule {
    @Binds
    fun bindLinkAccountManager(linkAccountManager: DefaultLinkAccountManager): LinkAccountManager

    @Binds
    fun bindsLinkGate(linkGate: DefaultLinkGate): LinkGate

    @Binds
    fun bindsLinkAuth(linkGate: DefaultLinkAuth): LinkAuth

    @Binds
    fun bindsLinkAttestationCheck(linkAttestationCheck: DefaultLinkAttestationCheck): LinkAttestationCheck

    companion object {
        @Provides
        fun provideLinkLaunchMode(): LinkLaunchMode? = null
    }
}
