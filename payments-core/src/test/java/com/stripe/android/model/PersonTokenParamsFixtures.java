package com.stripe.android.model;

public final class PersonTokenParamsFixtures {

    public static final PersonTokenParams PARAMS = new PersonTokenParams.Builder()
            .setFirstName("Jenny")
            .setLastName("Rosen")
            .setDateOfBirth(
                    new DateOfBirth(1, 1, 1993)
            )
            .setGender("female")
            .setAddress(
                    new Address.Builder()
                            .setLine1("123 Market St")
                            .setCity("San Francisco")
                            .setState("CA")
                            .setPostalCode("94107")
                            .setCountry("US")
                            .build()
            )
            .setEmail("jenny@example.com")
            .setPhone("1-800-456-7890")
            .setSsnLast4("1234")
            .setRelationship(
                    new PersonTokenParams.Relationship.Builder()
                            .setDirector(true)
                            .setExecutive(true)
                            .setOwner(true)
                            .setPercentOwnership(95)
                            .build()
            )
            .setVerification(
                    new PersonTokenParams.Verification(
                            new PersonTokenParams.Document(),
                            new PersonTokenParams.Document()
                    )
            )
            .build();
}
