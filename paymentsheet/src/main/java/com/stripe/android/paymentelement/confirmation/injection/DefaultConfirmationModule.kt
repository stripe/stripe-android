package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.attestation.AttestationConfirmationModule
import com.stripe.android.paymentelement.confirmation.challenge.PassiveChallengeConfirmationModule
import dagger.Module

@Module(
    includes = [
        AttestationConfirmationModule::class,
        PassiveChallengeConfirmationModule::class,
        ConfirmationHandlerModule::class,
    ]
)
internal interface DefaultConfirmationModule
