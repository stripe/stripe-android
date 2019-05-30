package com.stripe.android.model;

public final class Stripe3ds2AuthResultFixtures {

    // TODO(mshafrir): fully hydrate fixtures

    public static final Stripe3ds2AuthResult ARES_CHALLENGE_FLOW =
            new Stripe3ds2AuthResult.Builder()
                    .setAres(new Stripe3ds2AuthResult.Ares.Builder()
                            .setAcsChallengeMandated(Stripe3ds2AuthResult.Ares.VALUE_YES)
                            .build())
                    .build();

    public static final Stripe3ds2AuthResult ARES_FRICTIONLESS_FLOW =
            new Stripe3ds2AuthResult.Builder()
                    .setAres(new Stripe3ds2AuthResult.Ares.Builder()
                            .setAcsChallengeMandated("N")
                            .build())
                    .build();
}
