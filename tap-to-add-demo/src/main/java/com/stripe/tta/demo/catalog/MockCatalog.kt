package com.stripe.tta.demo.catalog

import java.util.Locale

internal data class MockProduct(
    val id: String,
    val name: String,
    val description: String,
    val unitPriceCents: Long,
)

internal data class CartLine(
    val product: MockProduct,
    val quantity: Int,
) {
    val lineTotalCents: Long
        get() = product.unitPriceCents * quantity
}

internal object MockCatalog {

    private const val CentsPerDollar = 100

    val products: List<MockProduct> = listOf(
        MockProduct(
            id = "demo_stripe_mug",
            name = "Stripe Ceramic Mug",
            description = "12 oz mug with glazed Stripe violet rim",
            unitPriceCents = 1299L,
        ),
        MockProduct(
            id = "demo_usb_hub",
            name = "4-Port USB-C Hub",
            description = "Compact aluminum hub for laptop setups",
            unitPriceCents = 3499L,
        ),
        MockProduct(
            id = "demo_headphones",
            name = "On-Ear Headphones",
            description = "Fold-flat design with inline mic",
            unitPriceCents = 5999L,
        ),
    )

    fun subtotalCents(quantities: Map<String, Int>): Long {
        if (quantities.isEmpty()) return 0L
        return products.sumOf { product ->
            val qty = quantities[product.id] ?: 0
            product.unitPriceCents * qty
        }
    }

    fun cartLines(quantities: Map<String, Int>): List<CartLine> {
        if (quantities.isEmpty()) return emptyList()
        return products.mapNotNull { product ->
            val qty = quantities[product.id] ?: 0
            if (qty > 0) CartLine(product, qty) else null
        }
    }

    fun formatUsd(cents: Long): String {
        val dollars = cents.toDouble() / CentsPerDollar
        return String.format(Locale.US, "$%.2f", dollars)
    }
}
