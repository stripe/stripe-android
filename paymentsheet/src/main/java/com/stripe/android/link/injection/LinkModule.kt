package com.stripe.android.link.injection

import android.app.Application
import com.stripe.android.link.account.DefaultLinkAccountManager
import com.stripe.android.link.account.DefaultLinkAuth
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.attestation.DefaultLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.attestation.IntegrityRequestManager
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

    companion object {
        @Provides
        @LinkScope
        fun provideIntegrityStandardRequestManager(
            context: Application
        ): IntegrityRequestManager = createIntegrityStandardRequestManager(context)
    }
}
