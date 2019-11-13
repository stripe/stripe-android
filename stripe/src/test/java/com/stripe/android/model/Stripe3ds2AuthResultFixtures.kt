package com.stripe.android.model

import java.util.UUID

internal object Stripe3ds2AuthResultFixtures {
    internal val ARES_CHALLENGE_FLOW = Stripe3ds2AuthResult(
        id = "threeds2_1FDy0vCRMbs6",
        objectType = "three_d_secure_2",
        source = "src_1FDy0uCRMb",
        created = 1567363381,
        ares = Stripe3ds2AuthResult.Ares(
            acsChallengeMandated = "Y",
            acsTransId = UUID.randomUUID().toString(),
            sdkTransId = UUID.randomUUID().toString(),
            threeDSServerTransId = UUID.randomUUID().toString(),
            transStatus = Stripe3ds2AuthResult.Ares.VALUE_CHALLENGE,
            messageType = "ARes",
            messageVersion = "2.1.0"
        )
    )

    internal val ARES_FRICTIONLESS_FLOW = Stripe3ds2AuthResult(
        id = "threeds2_1Ecwz3CRMbs6FrXfThtfogua",
        objectType = "three_d_secure_2",
        liveMode = false,
        created = 1558541285L,
        source = "src_1Ecwz1CRMbs6FrXfUwt98lxf",
        ares = Stripe3ds2AuthResult.Ares(
            acsChallengeMandated = "N",
            acsTransId = UUID.randomUUID().toString(),
            sdkTransId = UUID.randomUUID().toString(),
            threeDSServerTransId = UUID.randomUUID().toString(),
            messageVersion = "2.1.0",
            messageType = "ARes"
        )
    )

    internal val ERROR = Stripe3ds2AuthResult(
        id = "threeds2_1FDy0vCRMbs6",
        objectType = "three_d_secure_2",
        source = "src_1FDy0uCRMb",
        created = 1567363381,
        error = Stripe3ds2AuthResult.ThreeDS2Error(
            acsTransId = UUID.randomUUID().toString(),
            dsTransId = UUID.randomUUID().toString(),
            sdkTransId = UUID.randomUUID().toString(),
            threeDSServerTransId = UUID.randomUUID().toString(),
            messageVersion = "2.1.0",
            messageType = "Erro",
            errorComponent = "D",
            errorCode = "302",
            errorDescription = "Data could not be decrypted by the receiving system due to technical or other reason.",
            errorMessageType = "AReq"
        )
    )

    internal val FALLBACK_REDIRECT_URL = Stripe3ds2AuthResult(
        id = "threeds2_1FDy0vCRMbs6",
        objectType = "three_d_secure_2",
        source = "src_1FDy0uCRMb",
        created = 1567363381,
        fallbackRedirectUrl = "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW"
    )
}
