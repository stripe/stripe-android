package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

/**
 * Test for [SelectShippingMethodWidget]
 */
@RunWith(RobolectricTestRunner::class)
class SelectShippingMethodWidgetTest {

    private val selectShippingMethodWidget =
        SelectShippingMethodWidget(ApplicationProvider.getApplicationContext()).also {
            it.setShippingMethods(
                listOf(
                    ShippingMethodFixtures.UPS,
                    ShippingMethodFixtures.FEDEX
                )
            )
        }

    @Test
    fun selectedShippingMethodWidget_whenSelected_selectionChanges() {
        assertThat(selectShippingMethodWidget.selectedShippingMethod)
            .isEqualTo(ShippingMethodFixtures.UPS)

        selectShippingMethodWidget.setSelectedShippingMethod(ShippingMethodFixtures.FEDEX)
        assertThat(selectShippingMethodWidget.selectedShippingMethod)
            .isEqualTo(ShippingMethodFixtures.FEDEX)
    }
}
