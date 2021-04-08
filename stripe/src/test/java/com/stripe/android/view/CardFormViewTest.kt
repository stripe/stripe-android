package com.stripe.android.view

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class CardFormViewTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private val standardCardFormView: CardFormView by lazy {
        activityScenarioFactory.createView {
            CardFormView(it)
        }
    }

    private val borderLessCardFormView: CardFormView by lazy {
        activityScenarioFactory.createView {
            CardFormView(
                context = it,
                attrs = Robolectric.buildAttributeSet()
                    .addAttribute(R.attr.cardFormStyle, "1")
                    .build()
            )
        }
    }

    @Before
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @After
    fun teardown() {
        setLocale(Locale.US)
    }

    @Test
    fun `when locale is US then country should be US and postal config should be US`() {
        setLocale(Locale.US)
        assertThat(
            standardCardFormView.countryLayout.countryAutocomplete.text.toString()
        ).isEqualTo("United States")

        assertThat(
            standardCardFormView.postalCodeView.config
        ).isEqualTo(PostalCodeEditText.Config.US)
    }

    @Test
    fun `when locale is not US then country should not be US and postal config should be Global`() {
        setLocale(Locale.CANADA)
        assertThat(
            standardCardFormView.countryLayout.countryAutocomplete.text.toString()
        ).isEqualTo("Canada")

        assertThat(
            standardCardFormView.postalCodeView.config
        ).isEqualTo(PostalCodeEditText.Config.Global)
    }

    @Test
    fun `when all fields are valid then cardParams should return correctly and errors is empty`() {
        standardCardFormView.postalCodeView.setText(VALID_US_ZIP)
        standardCardFormView.cardMultilineWidget.cardNumberEditText.setText(VISA_WITH_SPACES)
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append("12")
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append("50")
        standardCardFormView.cardMultilineWidget.cvcEditText.append(VALID_CVC)

        assertThat(standardCardFormView.cardParams)
            .isEqualTo(
                CardParams(
                    brand = CardBrand.Visa,
                    loggingTokens = setOf(CardFormView.CARD_FORM_VIEW),
                    number = CardNumberFixtures.VISA_NO_SPACES,
                    expMonth = 12,
                    expYear = 2050,
                    cvc = VALID_CVC,
                    address = Address.Builder()
                        .setCountry("United States")
                        .setPostalCode(VALID_US_ZIP)
                        .build()
                )
            )

        idleLooper()

        assertThat(standardCardFormView.errors.text).isEqualTo("")
        assertThat(standardCardFormView.errors.isVisible).isFalse()
    }

    @Test
    fun `when postal field is invalid then cardParams should return null and errors is not empty`() {
        standardCardFormView.postalCodeView.setText(INVALID_US_ZIP)
        standardCardFormView.cardMultilineWidget.cardNumberEditText.setText(VISA_WITH_SPACES)
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append(VALID_MONTH)
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append(VALID_YEAR)
        standardCardFormView.cardMultilineWidget.cvcEditText.append(VALID_CVC)

        assertThat(standardCardFormView.cardParams).isNull()

        idleLooper()

        assertThat(standardCardFormView.errors.text).isEqualTo(context.getString(R.string.address_zip_invalid))
        assertThat(standardCardFormView.errors.isVisible).isTrue()
    }

    @Test
    fun `when card number is invalid then cardParams should return null and errors is not empty`() {
        standardCardFormView.postalCodeView.setText(VALID_US_ZIP)
        standardCardFormView.cardMultilineWidget.cardNumberEditText.setText(INVALID_VISA)
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append(VALID_MONTH)
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append(VALID_YEAR)
        standardCardFormView.cardMultilineWidget.cvcEditText.append(VALID_CVC)

        assertThat(standardCardFormView.cardParams).isNull()

        idleLooper()

        assertThat(standardCardFormView.errors.text).isEqualTo(context.getString(R.string.invalid_card_number))
        assertThat(standardCardFormView.errors.isVisible).isTrue()
    }

    @Test
    fun `when expiration is invalid then cardParams should return null and errors is not empty`() {
        standardCardFormView.postalCodeView.setText(VALID_US_ZIP)
        standardCardFormView.cardMultilineWidget.cardNumberEditText.setText(VISA_WITH_SPACES)
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append(VALID_MONTH)
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append(INVALID_YEAR)
        standardCardFormView.cardMultilineWidget.cvcEditText.append(VALID_CVC)

        assertThat(standardCardFormView.cardParams).isNull()

        idleLooper()

        assertThat(standardCardFormView.errors.text).isEqualTo(context.getString(R.string.invalid_expiry_year))
        assertThat(standardCardFormView.errors.isVisible).isTrue()
    }

    @Test
    fun `when cvc is invalid then cardParams should return null and errors is not empty`() {
        standardCardFormView.postalCodeView.setText(VALID_US_ZIP)
        standardCardFormView.cardMultilineWidget.cardNumberEditText.setText(VISA_WITH_SPACES)
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append(VALID_MONTH)
        standardCardFormView.cardMultilineWidget.expiryDateEditText.append(VALID_YEAR)
        standardCardFormView.cardMultilineWidget.cvcEditText.append(INVALID_CVC)

        assertThat(standardCardFormView.cardParams).isNull()

        idleLooper()

        assertThat(standardCardFormView.errors.text).isEqualTo(context.getString(R.string.invalid_cvc))
        assertThat(standardCardFormView.errors.isVisible).isTrue()
    }

    @Test
    fun verifyStandardStyle() {
        idleLooper()

        // 2 additional horizontal dividers added to card_multiline_widget.xml, now it has 4 child views
        assertThat(standardCardFormView.cardMultilineWidget.childCount).isEqualTo(4)
        // tl_card_number
        assertThat(standardCardFormView.cardMultilineWidget.getChildAt(0)).isInstanceOf(
            CardNumberTextInputLayout::class.java
        )
        // horizontal divider
        assertThat(standardCardFormView.cardMultilineWidget.getChildAt(1)).isInstanceOf(
            View::class.java
        )
        // second_row_layout
        assertThat(standardCardFormView.cardMultilineWidget.getChildAt(2)).isInstanceOf(
            LinearLayout::class.java
        )
        // horizontal divider
        assertThat(standardCardFormView.cardMultilineWidget.getChildAt(3)).isInstanceOf(
            View::class.java
        )

        // 1 vertical divider added between exp and cvc, now secondRowLayout has 4 child views
        assertThat(standardCardFormView.cardMultilineWidget.secondRowLayout.childCount).isEqualTo(4)
        // tl_expiry
        assertThat(standardCardFormView.cardMultilineWidget.secondRowLayout.getChildAt(0)).isInstanceOf(
            TextInputLayout::class.java
        )
        // vertical divider
        assertThat(standardCardFormView.cardMultilineWidget.secondRowLayout.getChildAt(1)).isInstanceOf(
            View::class.java
        )
        // tl_cvc
        assertThat(standardCardFormView.cardMultilineWidget.secondRowLayout.getChildAt(2)).isInstanceOf(
            TextInputLayout::class.java
        )
        // tl_postal_code(invisible)
        assertThat(standardCardFormView.cardMultilineWidget.secondRowLayout.getChildAt(3)).isInstanceOf(
            TextInputLayout::class.java
        )

        // divider with width=match_parent is visible
        assertThat(standardCardFormView.countryPostalDivider.isVisible).isTrue()
    }

    @Test
    fun verifyBorderlessStyle() {
        idleLooper()

        // no child views added to card_multiline_widget.xml, it still has 2 child views
        assertThat(borderLessCardFormView.cardMultilineWidget.childCount).isEqualTo(2)
        // tl_card_number
        assertThat(borderLessCardFormView.cardMultilineWidget.getChildAt(0)).isInstanceOf(
            CardNumberTextInputLayout::class.java
        )
        // second_row_layout
        assertThat(borderLessCardFormView.cardMultilineWidget.getChildAt(1)).isInstanceOf(
            LinearLayout::class.java
        )

        // 1 horizontal divider added to tl_card_number, now it has 2 child views
        assertThat(borderLessCardFormView.cardMultilineWidget.cardNumberTextInputLayout.childCount).isEqualTo(
            2
        )

        // no vertical divider added between exp and cvc, secondRowLayout still has 3 child views
        assertThat(borderLessCardFormView.cardMultilineWidget.secondRowLayout.childCount).isEqualTo(
            3
        )

        // 1 horizontal divider added below tl_expiry, now it has 2 child views
        assertThat(borderLessCardFormView.cardMultilineWidget.expiryTextInputLayout.childCount).isEqualTo(
            2
        )

        // 1 horizontal divider added below tl_cvc, now it has 2 child views
        assertThat(borderLessCardFormView.cardMultilineWidget.cvcInputLayout.childCount).isEqualTo(2)

        // 1 horizontal divider added below countryLayout, now it has 2 child views
        assertThat(borderLessCardFormView.countryLayout.childCount).isEqualTo(2)

        // divider with width=match_parent is invisible
        assertThat(borderLessCardFormView.countryPostalDivider.isVisible).isFalse()
    }

    private fun setLocale(locale: Locale) {
        Locale.setDefault(locale)
        context.resources.configuration.let { config ->
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    companion object {
        const val VALID_MONTH = "12"
        const val VALID_YEAR = "50"
        const val INVALID_YEAR = "11"
        const val VALID_CVC = "123"
        const val INVALID_CVC = "12"
        const val VALID_US_ZIP = "95051"
        const val INVALID_US_ZIP = "9505"
        const val INVALID_VISA = "1234 1234 1234 1234"
    }
}
