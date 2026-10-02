package com.stripe.android.identity

import android.os.Build
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.Colors
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.themeadapter.material.createMdcTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class IdentityActivityThemeTest {
    @Test
    fun `AppCompat host can create Identity content and permission dialog`() = runScenario(
        hostTheme = AppCompatR.style.Theme_AppCompat_Light_NoActionBar,
    ) {
        assertFailsWith<IllegalArgumentException> { readTheme() }

        activity.ensureCompatibleTheme()

        assertThat(readTheme().colors).isNotNull()
        assertContentAndDialogCanBeCreated()
    }

    @Test
    fun `platform host can create Identity content and permission dialog`() = runScenario(
        hostTheme = android.R.style.Theme_Material_Light_NoActionBar,
    ) {
        assertFailsWith<IllegalArgumentException> { readTheme() }

        activity.ensureCompatibleTheme()

        assertThat(readTheme().colors).isNotNull()
        assertContentAndDialogCanBeCreated()
    }

    @Test
    fun `Material host colors typography and shapes are preserved`() = runScenario(
        hostTheme = MaterialR.style.Theme_MaterialComponents_Light_NoActionBar,
    ) {
        // Give the host distinct styling without requiring test-only resources.
        activity.theme.applyStyle(MaterialR.style.ThemeOverlay_MaterialComponents_Dark, true)
        val hostParameters = readTheme()

        activity.ensureCompatibleTheme()

        val identityParameters = readTheme()
        assertColorsPreserved(requireNotNull(hostParameters.colors))
        assertThat(identityParameters.typography).isEqualTo(hostParameters.typography)
        assertThat(identityParameters.shapes).isEqualTo(hostParameters.shapes)
    }

    @Test
    fun `Material3 host styling is preserved`() = runScenario(
        hostTheme = MaterialR.style.Theme_Material3_Light_NoActionBar,
    ) {
        val hostParameters = readTheme()

        activity.ensureCompatibleTheme()

        assertColorsPreserved(requireNotNull(hostParameters.colors))
        assertContentAndDialogCanBeCreated()
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `fallback follows system light mode`() = runScenario(
        hostTheme = android.R.style.Theme_Material_Light_NoActionBar,
    ) {
        activity.ensureCompatibleTheme()

        assertThat(requireNotNull(readTheme().colors).isLight).isTrue()
    }

    @Test
    @Config(qualifiers = "night")
    fun `fallback follows system dark mode`() = runScenario(
        hostTheme = android.R.style.Theme_Material_Light_NoActionBar,
    ) {
        activity.ensureCompatibleTheme()

        assertThat(requireNotNull(readTheme().colors).isLight).isFalse()
    }

    private fun runScenario(
        hostTheme: Int,
        block: Scenario.() -> Unit,
    ) {
        Robolectric.buildActivity(IdentityActivity::class.java).use { controller ->
            val activity = controller.get()
            activity.setTheme(hostTheme)
            Scenario(activity).apply(block)
        }
    }

    private data class Scenario(val activity: IdentityActivity) {
        fun readTheme() = createMdcTheme(
            context = activity,
            layoutDirection = LayoutDirection.Ltr,
        )

        fun assertColorsPreserved(hostColors: Colors) {
            val identityColors = requireNotNull(readTheme().colors)
            assertThat(identityColors.primary).isEqualTo(hostColors.primary)
            assertThat(identityColors.secondary).isEqualTo(hostColors.secondary)
            assertThat(identityColors.background).isEqualTo(hostColors.background)
            assertThat(identityColors.onBackground).isEqualTo(hostColors.onBackground)
            assertThat(identityColors.surface).isEqualTo(hostColors.surface)
            assertThat(identityColors.onSurface).isEqualTo(hostColors.onSurface)
            assertThat(identityColors.isLight).isEqualTo(hostColors.isLight)
        }

        fun assertContentAndDialogCanBeCreated() {
            activity.setContentView(FrameLayout(activity))
            val dialog = AlertDialog.Builder(activity).setMessage("Camera permission").show()
            try {
                assertThat(dialog.isShowing).isTrue()
            } finally {
                dialog.dismiss()
            }
        }
    }
}
