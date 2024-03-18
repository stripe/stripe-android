package com.stripe.android.lpmfoundations.paymentmethod

// TODO: Enable tests.
internal class CardDefinitionTest {
    //    @Test
//    fun `Card contains fields according to billing details collection configuration`() {
//        lpmRepository.update(
//            PaymentIntentFactory.create(paymentMethodTypes = listOf("card")),
//            """
//          [
//            {
//                "type": "card",
//                "async": false,
//                "fields": []
//            }
//         ]
//            """.trimIndent(),
//            PaymentSheet.BillingDetailsCollectionConfiguration(
//                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
//                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
//                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
//                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
//            )
//        )
//
//        val card = lpmRepository.fromCode("card")!!
//        // Contact information, Card information, Billing address and Save for future use.
//        assertThat(card.formSpec.items.size).isEqualTo(4)
//        assertThat(card.formSpec.items[0]).isInstanceOf(ContactInformationSpec::class.java)
//        assertThat(card.formSpec.items[1]).isInstanceOf(CardDetailsSectionSpec::class.java)
//        assertThat(card.formSpec.items[2]).isInstanceOf(CardBillingSpec::class.java)
//        assertThat(card.formSpec.items[3]).isInstanceOf(SaveForFutureUseSpec::class.java)
//
//        val contactInfoSpec = card.formSpec.items[0] as ContactInformationSpec
//        // Name is collected in the card details section.
//        assertThat(contactInfoSpec.collectName).isFalse()
//        assertThat(contactInfoSpec.collectEmail).isTrue()
//        assertThat(contactInfoSpec.collectPhone).isFalse()
//
//        val cardSpec = card.formSpec.items[1] as CardDetailsSectionSpec
//        assertThat(cardSpec.collectName).isTrue()
//
//        val addressSpec = card.formSpec.items[2] as CardBillingSpec
//        assertThat(addressSpec.collectionMode)
//            .isEqualTo(BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
//    }
}
