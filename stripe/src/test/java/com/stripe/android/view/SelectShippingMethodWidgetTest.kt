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
            "Arrives in 3-5 days",
            0,
            "USD"
        )
        private val FEDEX = ShippingMethod(
            "FedEx",
            "fedex",
            "Arrives tomorrow",
            599,
            "USD"
        )
    }
}
