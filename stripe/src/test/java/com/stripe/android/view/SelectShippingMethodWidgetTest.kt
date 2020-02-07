package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
                it.setShippingMethods(listOf(
                    ShippingMethodFixtures.UPS,
                    ShippingMethodFixtures.FEDEX
                ))
            }
    }

    @Test
    fun selectedShippingMethodWidget_whenSelected_selectionChanges() {
        assertEquals(
            ShippingMethodFixtures.UPS,
            selectShippingMethodWidget.selectedShippingMethod
        )
        selectShippingMethodWidget.setSelectedShippingMethod(ShippingMethodFixtures.FEDEX)
        assertEquals(
            ShippingMethodFixtures.FEDEX,
            selectShippingMethodWidget.selectedShippingMethod
        )
    }
}
