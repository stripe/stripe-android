package com.stripe.android.model;

import java.util.UUID;

public final class Stripe3ds2AuthResultFixtures {

    // TODO(mshafrir): fully hydrate fixtures

    public static final Stripe3ds2AuthResult ARES_CHALLENGE_FLOW =
            new Stripe3ds2AuthResult.Builder()
                    .setAres(new Stripe3ds2AuthResult.Ares.Builder()
                            .setAcsChallengeMandated(Stripe3ds2AuthResult.Ares.VALUE_YES)
                            .setAcsTransId(UUID.randomUUID().toString())
                            .setSdkTransId(UUID.randomUUID().toString())
                            .setThreeDSServerTransId(UUID.randomUUID().toString())
                            .setMessageVersion("2.1.0")
                            .setMessageType("ARes")
                            .build())
                    .build();

    public static final Stripe3ds2AuthResult ARES_FRICTIONLESS_FLOW =
            new Stripe3ds2AuthResult.Builder()
                    .setAres(new Stripe3ds2AuthResult.Ares.Builder()
                            .setAcsChallengeMandated("N")
                            .setAcsTransId(UUID.randomUUID().toString())
                            .setSdkTransId(UUID.randomUUID().toString())
                            .setThreeDSServerTransId(UUID.randomUUID().toString())
                            .setMessageVersion("2.1.0")
                            .setMessageType("ARes")
                            .build())
                    .build();

    public static final Stripe3ds2AuthResult ERROR =
            new Stripe3ds2AuthResult.Builder()
                    .setError(new Stripe3ds2AuthResult.ThreeDS2Error.Builder()
                            .setAcsTransId(UUID.randomUUID().toString())
                            .setDsTransId(UUID.randomUUID().toString())
                            .setSdkTransId(UUID.randomUUID().toString())
                            .setThreeDSServerTransId(UUID.randomUUID().toString())
                            .setErrorComponent("D")
                            .setErrorCode("302")
                            .setErrorDescription("Data could not be decrypted by the receiving system due to technical or other reason.")
                            .setErrorMessageType("Erro")
                    .build())
            .build();

    public static final Stripe3ds2AuthResult FALLBACK_REDIRECT_URL =
            new Stripe3ds2AuthResult.Builder()
                    .setId("threeds2_1FDy0vCRMbs6")
                    .setObjectType("three_d_secure_2")
                    .setSource("src_1FDy0uCRMb")
                    .setCreated(1567363381)
                    .setFallbackRedirectUrl("https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW")
                    .build();
}
