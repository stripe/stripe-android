package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import kotlin.test.Test

class SourceJsonParserTest {

    @Test
    fun `should parse Owner with verified fields correctly`() {
        assertThat(
            SourceJsonParser.OwnerJsonParser()
                .parse(SourceFixtures.SOURCE_OWNER_WITHOUT_NULLS)
        ).isEqualTo(
            Source.Owner(
                address = Address(
                    line1 = "123 Market St",
                    line2 = "#345",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94107"
                ),
                email = "jenny.rosen@example.com",
                name = "Jenny Rosen",
                phone = "4158675309",
                verifiedAddress = Address(
                    line1 = "123 Market St",
                    line2 = "#345",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94107"
                ),
                verifiedEmail = "jenny.rosen@example.com",
                verifiedName = "Jenny Rosen",
                verifiedPhone = "4158675309"
            )
        )
    }

    @Test
    fun `should parse Owner without verified fields correctly`() {
        assertThat(
            SourceJsonParser.OwnerJsonParser()
                .parse(SourceFixtures.SOURCE_OWNER_WITH_NULLS)
        ).isEqualTo(
            Source.Owner(
                address = null,
                email = "jenny.rosen@example.com",
                name = "Jenny Rosen",
                phone = "4158675309",
                verifiedAddress = null,
                verifiedEmail = null,
                verifiedName = null,
                verifiedPhone = null
            )
        )
    }
}
