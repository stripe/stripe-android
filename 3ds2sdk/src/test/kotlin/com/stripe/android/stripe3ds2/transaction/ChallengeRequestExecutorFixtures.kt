package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.security.DefaultMessageTransformer
import com.stripe.android.stripe3ds2.security.StripeEphemeralKeyPairGenerator
import java.util.UUID

internal object ChallengeRequestExecutorFixtures {
    private val KEYPAIR = StripeEphemeralKeyPairGenerator(FakeErrorReporter())
        .generate()

    val CONFIG = ChallengeRequestExecutor.Config(
        DefaultMessageTransformer(isLiveMode = true),
        UUID.randomUUID().toString(),
        ChallengeMessageFixtures.CREQ,
        "https://bank.com",
        ChallengeRequestExecutor.Config.Keys(
            KEYPAIR.private.encoded,
            KEYPAIR.public.encoded
        )
    )
}
