package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.ShippingMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test for [SelectShippingMethodWidget]
 */
@RunWith(RobolectricTestRunner::class)
class SelectShippingMethodWidgetTest {

    private val selectShippingMethodWidget: SelectShippingMethodWidget by lazy {
        SelectShippingMethodWidget(ApplicationProvider.getApplicationContext<Context>())
            .also {
                it.setShippingMethods(listOf(UPS, FEDEX), UPS)
            }
    }

    @Test
    fun selectedShippingMethodWidget_whenSelected_selectionChanges() {
        assertEquals(UPS, selectShippingMethodWidget.selectedShippingMethod)
        selectShippingMethodWidget.setSelectedShippingMethod(FEDEX)
        assertEquals(FEDEX, selectShippingMethodWidget.selectedShippingMethod)
    }

    private companion object {
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
