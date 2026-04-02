package com.stripe.android.connect.webview.serialization

import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.appearance.Action
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.BadgeDefaults
import com.stripe.android.connect.appearance.Button
import com.stripe.android.connect.appearance.ButtonDefaults
import com.stripe.android.connect.appearance.Colors
import com.stripe.android.connect.appearance.Form
import com.stripe.android.connect.appearance.TextTransform
import com.stripe.android.connect.appearance.Typography
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class AppearanceToJsTest {

    private fun appearanceVariables(appearance: Appearance): JsonObject {
        val root = ConnectJson.encodeToJsonElement(
            AppearanceJs.serializer(),
            appearance.toJs(),
        ).jsonObject
        return root["variables"]!!.jsonObject
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.content

    @Test
    @Suppress("LongMethod")
    fun `Checks new tokens map to correct variables and inherits correct values`() {
        val dangerColorBackground = 0xFFFF_0000.toInt()
        val dangerColorBorder = 0xFF00_FF00.toInt()
        val dangerColorText = 0xFF00_00FF.toInt()
        val actionPrimaryColor = 0xFF11_2233.toInt()
        val actionSecondaryColor = 0xFF44_5566.toInt()
        val formPlaceholder = 0xFFAA_BBCC.toInt()

        val appearance = Appearance.Builder()
            .buttonDanger(
                Button(
                    colorBackground = dangerColorBackground,
                    colorBorder = dangerColorBorder,
                    colorText = dangerColorText,
                )
            )
            .buttonDefaults(
                ButtonDefaults.Builder()
                    .paddingX(5f)
                    .paddingY(6f)
                    .labelTypography(
                        Typography.Style(
                            fontSize = 11f,
                            fontWeight = 700,
                            textTransform = TextTransform.Lowercase,
                        )
                    )
                    .build()
            )
            .badgeDefaults(
                BadgeDefaults.Builder()
                    .paddingX(2f)
                    .paddingY(3f)
                    .labelTypography(
                        Typography.Style(
                            fontSize = 9f,
                            fontWeight = 400,
                            textTransform = TextTransform.Uppercase,
                        )
                    )
                    .build()
            )
            .actionPrimaryText(
                Action.Builder()
                    .colorText(actionPrimaryColor)
                    .textTransform(TextTransform.Capitalize)
                    .build()
            )
            .actionSecondaryText(
                Action.Builder()
                    .colorText(actionSecondaryColor)
                    .textTransform(TextTransform.None)
                    .build()
            )
            .form(
                Form.Builder()
                    .placeholderTextColor(formPlaceholder)
                    .inputFieldPaddingX(12f)
                    .inputFieldPaddingY(14f)
                    .build()
            )
            .tableRowPaddingY(16f)
            .spacingUnit(8f)
            .build()

        val v = appearanceVariables(appearance)

        assertThat(v.string("buttonDangerColorBackground")).isEqualTo("#FF0000")
        assertThat(v.string("buttonDangerColorBorder")).isEqualTo("#00FF00")
        assertThat(v.string("buttonDangerColorText")).isEqualTo("#0000FF")

        assertThat(v.string("buttonPaddingX")).isEqualTo("5px")
        assertThat(v.string("buttonPaddingY")).isEqualTo("6px")
        assertThat(v.string("buttonLabelFontSize")).isEqualTo("11px")
        assertThat(v.string("buttonLabelFontWeight")).isEqualTo("700")
        assertThat(v.string("buttonLabelTextTransform")).isEqualTo("Lowercase")

        assertThat(v.string("badgePaddingX")).isEqualTo("2px")
        assertThat(v.string("badgePaddingY")).isEqualTo("3px")
        assertThat(v.string("badgeLabelFontSize")).isEqualTo("9px")
        assertThat(v.string("badgeLabelFontWeight")).isEqualTo("400")
        assertThat(v.string("badgeLabelTextTransform")).isEqualTo("Uppercase")

        assertThat(v.string("actionPrimaryColorText")).isEqualTo("#112233")
        assertThat(v.string("actionPrimaryTextTransform")).isEqualTo("Capitalize")
        assertThat(v.string("actionSecondaryColorText")).isEqualTo("#445566")
        assertThat(v.string("actionSecondaryTextTransform")).isEqualTo("None")

        assertThat(v.string("formPlaceholderTextColor")).isEqualTo("#AABBCC")
        assertThat(v.string("inputFieldPaddingX")).isEqualTo("12px")
        assertThat(v.string("inputFieldPaddingY")).isEqualTo("14px")

        assertThat(v.string("tableRowPaddingY")).isEqualTo("16px")
        assertThat(v.string("spacingUnit")).isEqualTo("8px")
    }

    @Test
    @Suppress("DEPRECATION")
    fun `Uses deprecated Colors fields and not the Form fields to ensure there are no breaking changes`() {
        val colors = Colors.Builder()
            .formBackground(0xFF11_2233.toInt())
            .formHighlightBorder(0xFFDE_AD00.toInt())
            .formAccent(0xFFBE_EF00.toInt())
            .build()

        val appearance = Appearance.Builder()
            .colors(colors)
            .form(Form.Builder().build())
            .build()

        val v = appearanceVariables(appearance)

        assertThat(v.string("formBackgroundColor")).isEqualTo("#112233")
        assertThat(v.string("formHighlightColorBorder")).isEqualTo("#DEAD00")
        assertThat(v.string("formAccentColor")).isEqualTo("#BEEF00")
    }

    @Test
    @Suppress("DEPRECATION")
    fun `Prefers Form over deprecated Colors for form background highlight and accent`() {
        val colors = Colors.Builder()
            .formBackground(0xFF11_1111.toInt())
            .formHighlightBorder(0xFF22_2222.toInt())
            .formAccent(0xFF33_3333.toInt())
            .build()

        val form = Form.Builder()
            .colorBackground(0xFFAA_AAAA.toInt())
            .highlightBorder(0xFFBB_BBBB.toInt())
            .accent(0xFFCC_CCCC.toInt())
            .build()

        val appearance = Appearance.Builder()
            .colors(colors)
            .form(form)
            .build()

        val v = appearanceVariables(appearance)

        assertThat(v.string("formBackgroundColor")).isEqualTo("#AAAAAA")
        assertThat(v.string("formHighlightColorBorder")).isEqualTo("#BBBBBB")
        assertThat(v.string("formAccentColor")).isEqualTo("#CCCCCC")
    }
}
