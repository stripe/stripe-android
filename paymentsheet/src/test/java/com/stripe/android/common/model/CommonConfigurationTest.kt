package com.stripe.android.common.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.CardBrandAcceptance
import org.junit.Test

internal class CommonConfigurationTest {
    private val configuration = CommonConfigurationFactory.create()

    @Test
    fun `'containVolatileDifferences' should return false when no volatile differences are found`() {
        val changedConfiguration = configuration.copy(
            merchantDisplayName = "New merchant, Inc.",
        )

        assertThat(configuration.containsVolatileDifferences(changedConfiguration)).isFalse()
    }

    @Test
    fun `'containVolatileDifferences' should return true when volatile differences are found`() {
        val configWithCardBrandAcceptanceChanges = configuration.copy(
            cardBrandAcceptance = CardBrandAcceptance.disallowed(
                listOf(CardBrandAcceptance.BrandCategory.Visa)
            )
        )

        assertThat(configuration.containsVolatileDifferences(configWithCardBrandAcceptanceChanges)).isTrue()

        val configWithBillingDetailsChanges = configuration.copy(
            defaultBillingDetails = BillingDetails(
                name = "Jenny Richards",
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithBillingDetailsChanges)).isTrue()

        val configWithBillingConfigChanges = configuration.copy(
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                name = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithBillingConfigChanges)).isTrue()
    }
}
