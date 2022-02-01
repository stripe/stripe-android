package com.stripe.android.stripe3ds2.views

import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class InformationZoneViewTest {

    private val informationZoneView = InformationZoneView(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun setWhyInfo_shouldSetWhyTextViews() {
        informationZoneView.setWhyInfo(WHY_INFO_LABEL, WHY_INFO_TEXT)
        assertEquals(WHY_INFO_LABEL, informationZoneView.whyLabel.text)
        assertEquals(WHY_INFO_TEXT, informationZoneView.whyText.text)
        assertEquals(View.VISIBLE, informationZoneView.whyContainer.visibility)
        assertEquals(View.GONE, informationZoneView.whyText.visibility)
    }

    @Test
    fun setWhyInfo_emptyLabel_shouldNotSetWhyTextViews() {
        informationZoneView.setWhyInfo("", WHY_INFO_TEXT)
        assertTrue(informationZoneView.whyLabel.text.isEmpty())
        assertTrue(informationZoneView.whyText.text.isEmpty())
        assertEquals(View.GONE, informationZoneView.whyContainer.visibility)
        assertEquals(View.GONE, informationZoneView.whyText.visibility)
    }

    @Test
    fun setExpandInfo_shouldSetExpandTextViews() {
        informationZoneView.setExpandInfo(EXPAND_INFO_LABEL, EXPAND_INFO_TEXT)
        assertEquals(EXPAND_INFO_LABEL, informationZoneView.expandLabel.text)
        assertEquals(EXPAND_INFO_TEXT, informationZoneView.expandText.text)
        assertEquals(View.VISIBLE, informationZoneView.expandContainer.visibility)
        assertEquals(View.GONE, informationZoneView.expandText.visibility)
    }

    @Test
    fun setExpandInfo_emptyLabel_shouldNotSetExpandTextViews() {
        informationZoneView.setExpandInfo(null, EXPAND_INFO_TEXT)
        assertTrue(informationZoneView.expandLabel.text.isEmpty())
        assertTrue(informationZoneView.expandText.text.isEmpty())
        assertEquals(
            View.GONE,
            informationZoneView.expandContainer.visibility
        )
        assertEquals(
            View.GONE,
            informationZoneView.expandText.visibility
        )
    }

    @Test
    fun toggleView_whyLabelClicked_whyTextViewVisibilityShouldToggle() {
        informationZoneView.setWhyInfo(WHY_INFO_LABEL, WHY_INFO_TEXT)
        assertEquals(informationZoneView.whyText.visibility, View.GONE)
        informationZoneView.whyContainer.performClick()
        assertEquals(informationZoneView.whyText.visibility, View.VISIBLE)
        informationZoneView.whyContainer.performClick()
        assertEquals(informationZoneView.whyText.visibility, View.GONE)
    }

    @Test
    fun toggleView_expandLabelClicked_expandTextViewVisibilityShouldToggle() {
        informationZoneView.setExpandInfo(EXPAND_INFO_LABEL, EXPAND_INFO_TEXT)
        assertEquals(informationZoneView.expandText.visibility, View.GONE)
        informationZoneView.expandContainer.performClick()
        assertEquals(informationZoneView.expandText.visibility, View.VISIBLE)
        informationZoneView.expandContainer.performClick()
        assertEquals(informationZoneView.expandText.visibility, View.GONE)
    }

    @Test
    fun expandViews_viewsExpanded() {
        informationZoneView.setExpandInfo(EXPAND_INFO_LABEL, EXPAND_INFO_TEXT)
        informationZoneView.setWhyInfo(WHY_INFO_LABEL, WHY_INFO_TEXT)
        informationZoneView.expandViews()
        assertEquals(informationZoneView.expandText.visibility, View.VISIBLE)
        assertEquals(informationZoneView.whyText.visibility, View.VISIBLE)
    }

    private companion object {
        private const val WHY_INFO_LABEL = "Why Info Label"
        private const val WHY_INFO_TEXT = "Why Info Text"
        private const val EXPAND_INFO_LABEL = "Expand Info Label"
        private const val EXPAND_INFO_TEXT = "Expand Info Text"
    }
}
