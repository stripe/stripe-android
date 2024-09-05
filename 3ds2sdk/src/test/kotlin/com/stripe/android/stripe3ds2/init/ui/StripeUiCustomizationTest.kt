package com.stripe.android.stripe3ds2.init.ui

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.stripe3ds2.utils.ParcelUtils
import com.stripe.android.stripe3ds2.views.ActivityScenarioFactory
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class StripeUiCustomizationTest {

    private val activityScenarioFactory =
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())

    @Test
    fun testDefaultButtonCustomizations() {
        val uiCustomization = StripeUiCustomization()

        val cancelButtonCustomization = mock(ButtonCustomization::class.java)
        uiCustomization.setButtonCustomization(
            cancelButtonCustomization,
            UiCustomization.ButtonType.CANCEL
        )
        assertSame(
            cancelButtonCustomization,
            uiCustomization.getButtonCustomization(UiCustomization.ButtonType.CANCEL)
        )

        val submitButtonCustomization = mock(ButtonCustomization::class.java)
        uiCustomization.setButtonCustomization(
            submitButtonCustomization,
            UiCustomization.ButtonType.SUBMIT
        )
        assertSame(
            submitButtonCustomization,
            uiCustomization.getButtonCustomization(UiCustomization.ButtonType.SUBMIT)
        )

        assertNull(uiCustomization.getButtonCustomization(UiCustomization.ButtonType.NEXT))
    }

    @Test
    fun testCustomButtonCustomizations() {
        val uiCustomization = StripeUiCustomization()

        val cancelButtonCustomization = mock(ButtonCustomization::class.java)
        uiCustomization.setButtonCustomization(cancelButtonCustomization, "custom1")
        assertSame(
            cancelButtonCustomization,
            uiCustomization.getButtonCustomization("custom1")
        )

        val submitButtonCustomization = mock(ButtonCustomization::class.java)
        uiCustomization.setButtonCustomization(submitButtonCustomization, "custom2")
        assertSame(
            submitButtonCustomization,
            uiCustomization.getButtonCustomization("custom2")
        )

        assertNull(uiCustomization.getButtonCustomization("custom3"))
    }

    @Test
    fun testSettersGetters() {
        val uiCustomization = StripeUiCustomization()

        val labelCustomization = StripeLabelCustomization()
        uiCustomization.setLabelCustomization(labelCustomization)
        assertEquals(labelCustomization, uiCustomization.labelCustomization)

        val toolbarCustomization = StripeToolbarCustomization()
        uiCustomization.setToolbarCustomization(toolbarCustomization)
        assertEquals(toolbarCustomization, uiCustomization.toolbarCustomization)

        val textBoxCustomization = StripeTextBoxCustomization()
        uiCustomization.setTextBoxCustomization(textBoxCustomization)
        assertEquals(textBoxCustomization, uiCustomization.textBoxCustomization)
    }

    @Test
    fun testParcelRoundtrip() {
        val uiCustomization = StripeUiCustomization()
        uiCustomization.setToolbarCustomization(StripeToolbarCustomization())
        uiCustomization.setLabelCustomization(StripeLabelCustomization())
        uiCustomization.setTextBoxCustomization(StripeTextBoxCustomization())
        uiCustomization.setButtonCustomization(StripeButtonCustomization(), "custom")
        uiCustomization.setButtonCustomization(StripeButtonCustomization(), UiCustomization.ButtonType.NEXT)
        val uiCustomizationFromParcel = ParcelUtils.get(uiCustomization)

        assertEquals(uiCustomization, uiCustomizationFromParcel)
    }

    @Test
    fun testContextConstructor() {
        activityScenarioFactory.create().use {
            it.onActivity { activity ->
                val customization = StripeUiCustomization.createWithAppTheme(activity)
                assertNotNull(customization.toolbarCustomization)
                assertEquals(
                    "#FF6C00F8",
                    customization.toolbarCustomization?.backgroundColor
                )
                assertEquals(
                    "#FFFFFFFF",
                    customization.toolbarCustomization?.textColor
                )

                val cancelButtonCustomization =
                    customization.getButtonCustomization(UiCustomization.ButtonType.CANCEL)
                assertEquals("#FFFFFFFF", cancelButtonCustomization?.textColor)

                val labelCustomization = customization.labelCustomization
                assertEquals("#FF000000", labelCustomization?.textColor)

                val resendButtonCustomization =
                    customization.getButtonCustomization(UiCustomization.ButtonType.RESEND)
                assertEquals(
                    "#FF6C00F8",
                    resendButtonCustomization?.textColor
                )

                val buttonCustomization = customization.getButtonCustomization(
                    UiCustomization.ButtonType.SUBMIT
                )
                assertNotNull(buttonCustomization)
                assertEquals(
                    "#FF6C00F8",
                    buttonCustomization.backgroundColor
                )

                assertNotNull(customization.getButtonCustomization(UiCustomization.ButtonType.CONTINUE))
                assertNotNull(customization.getButtonCustomization(UiCustomization.ButtonType.NEXT))
                assertNotNull(customization.getButtonCustomization(UiCustomization.ButtonType.SELECT))

                assertEquals(
                    "#61000000",
                    customization.textBoxCustomization?.hintTextColor
                )
            }
        }
    }
}
