package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.ShippingMethod
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test for [SelectShippingMethodWidget]
 */
@RunWith(RobolectricTestRunner::class)
class SelectShippingMethodWidgetTest {

    private lateinit var shippingMethodAdapter: ShippingMethodAdapter

    @BeforeTest
    fun setup() {
        Locale.setDefault(Locale.US)
        val selectShippingMethodWidget =
            SelectShippingMethodWidget(ApplicationProvider.getApplicationContext<Context>())
        selectShippingMethodWidget.setShippingMethods(listOf(UPS, FEDEX), UPS)
        shippingMethodAdapter = selectShippingMethodWidget.shippingMethodAdapter
    }

    @Test
    fun selectShippingMethodWidget_whenSelected_selectionChanges() {
        assertEquals(shippingMethodAdapter.selectedShippingMethod, UPS)
        shippingMethodAdapter.onShippingMethodSelected(1)
        assertEquals(shippingMethodAdapter.selectedShippingMethod, FEDEX)
    }

    companion object {
        private val UPS = ShippingMethod(
            "UPS Ground",
            "ups-ground",
            0,
            "USD",
            "Arrives in 3-5 days"
        )
        private val FEDEX = ShippingMethod(
            "FedEx",
            "fedex",
            599,
            "USD",
            "Arrives tomorrow"
        )
    }
}
