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
import com.stripe.android.databinding.StripeCardFormViewBinding
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.CountryCode
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

    private var standardBinding: StripeCardFormViewBinding? = null
    private var borderlessBinding: StripeCardFormViewBinding? = null

    @Before
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        standardBinding = StripeCardFormViewBinding.bind(standardCardFormView)
        borderlessBinding = StripeCardFormViewBinding.bind(borderLessCardFormView)
    }

    @After
    fun teardown() {
        setLocale(Locale.US)
    }

    @Test
    fun `when locale is US then country should be US and postal config should be US`() {
        setLocale(Locale.US)
        val binding = StripeCardFormViewBinding.bind(
            activityScenarioFactory.createView {
                CardFormView(it)
            }
        )

        assertThat(
            binding.countryLayout.countryAutocomplete.text.toString()
        ).isEqualTo("United States")

        assertThat(
            binding.postalCode.config
        ).isEqualTo(PostalCodeEditText.Config.US)
    }

    @Test
    fun `when locale is not US then country should not be US and postal config should be Global`() {
        setLocale(Locale.CANADA)
        val binding = StripeCardFormViewBinding.bind(
            activityScenarioFactory.createView {
                CardFormView(it)
            }
        )

        assertThat(
            binding.countryLayout.countryAutocomplete.text.toString()
        ).isEqualTo("Canada")

        assertThat(
            binding.postalCode.config
        ).isEqualTo(PostalCodeEditText.Config.Global)
    }

    @Test
    fun `when all fields are valid then cardParams should return correctly and errors is empty`() {
        setValuesToStandardUI(VISA_WITH_SPACES, VALID_MONTH, VALID_YEAR, VALID_CVC, VALID_US_ZIP)

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
                        .setCountryCode(CountryCode.US)
                        .setPostalCode(VALID_US_ZIP)
                        .build()
                )
            )

        idleLooper()

        standardBinding!!.let {
            assertThat(it.errors.text).isEqualTo("")
            assertThat(it.errors.isVisible).isFalse()
        }
    }

    @Test
    fun `when postal field is invalid then cardParams should return null and errors is not empty`() {
        setValuesToStandardUI(VISA_WITH_SPACES, VALID_MONTH, VALID_YEAR, VALID_CVC, INVALID_US_ZIP)

        assertThat(standardCardFormView.cardParams).isNull()

        idleLooper()

        standardBinding!!.let {
            assertThat(it.errors.text).isEqualTo(context.getString(R.string.address_zip_invalid))
            assertThat(it.errors.isVisible).isTrue()
        }
    }

    @Test
    fun `when card number is invalid then cardParams should return null and errors is not empty`() {
        setValuesToStandardUI(INVALID_VISA, VALID_MONTH, VALID_YEAR, VALID_CVC, VALID_US_ZIP)

        assertThat(standardCardFormView.cardParams).isNull()

        idleLooper()

        standardBinding!!.let {
            assertThat(it.errors.text).isEqualTo(context.getString(R.string.invalid_card_number))
            assertThat(it.errors.isVisible).isTrue()
        }
    }

    @Test
    fun `when expiration is invalid then cardParams should return null and errors is not empty`() {
        setValuesToStandardUI(VISA_WITH_SPACES, VALID_MONTH, INVALID_YEAR, VALID_CVC, VALID_US_ZIP)

        assertThat(standardCardFormView.cardParams).isNull()

        idleLooper()

        standardBinding!!.let {
            assertThat(it.errors.text).isEqualTo(context.getString(R.string.invalid_expiry_year))
            assertThat(it.errors.isVisible).isTrue()
        }
    }

    @Test
    fun `when cvc is invalid then cardParams should return null and errors is not empty`() {
        setValuesToStandardUI(VISA_WITH_SPACES, VALID_MONTH, VALID_YEAR, INVALID_CVC, VALID_US_ZIP)

        assertThat(standardCardFormView.cardParams).isNull()

        idleLooper()

        standardBinding!!.let {
            assertThat(it.errors.text).isEqualTo(context.getString(R.string.invalid_cvc))
            assertThat(it.errors.isVisible).isTrue()
        }
    }

    @Test
    fun `when cvc becomes valid then postal should get focus`() {
        setValuesToStandardUI(VISA_WITH_SPACES, VALID_MONTH, VALID_YEAR, INVALID_CVC)

        assertThat(standardBinding!!.postalCode.hasFocus()).isFalse()

        standardBinding!!.cardMultilineWidget.cvcEditText.append("3")

        assertThat(standardBinding!!.postalCode.hasFocus()).isTrue()
    }

    @Test
    fun verifyStandardStyle() {
        idleLooper()
        standardBinding!!.let {
            // 2 additional horizontal dividers added to card_multiline_widget.xml, now it has 4 child views
            assertThat(it.cardMultilineWidget.childCount).isEqualTo(4)
            // tl_card_number
            assertThat(it.cardMultilineWidget.getChildAt(0)).isInstanceOf(
                CardNumberTextInputLayout::class.java
            )
            // horizontal divider
            assertThat(it.cardMultilineWidget.getChildAt(1)).isInstanceOf(
                View::class.java
            )
            // second_row_layout
            assertThat(it.cardMultilineWidget.getChildAt(2)).isInstanceOf(
                LinearLayout::class.java
            )
            // horizontal divider
            assertThat(it.cardMultilineWidget.getChildAt(3)).isInstanceOf(
                View::class.java
            )

            // 1 vertical divider added between exp and cvc, now secondRowLayout has 4 child views
            assertThat(it.cardMultilineWidget.secondRowLayout.childCount).isEqualTo(4)
            // tl_expiry
            assertThat(it.cardMultilineWidget.secondRowLayout.getChildAt(0)).isInstanceOf(
                TextInputLayout::class.java
            )
            // vertical divider
            assertThat(it.cardMultilineWidget.secondRowLayout.getChildAt(1)).isInstanceOf(
                View::class.java
            )
            // tl_cvc
            assertThat(it.cardMultilineWidget.secondRowLayout.getChildAt(2)).isInstanceOf(
                TextInputLayout::class.java
            )
            // tl_postal_code(invisible)
            assertThat(it.cardMultilineWidget.secondRowLayout.getChildAt(3)).isInstanceOf(
                TextInputLayout::class.java
            )

            // divider with width=match_parent is visible
            assertThat(it.countryPostalDivider.isVisible).isTrue()
        }
    }

    @Test
    fun verifyBorderlessStyle() {
        idleLooper()
        borderlessBinding?.let {
            // no child views added to card_multiline_widget.xml, it still has 2 child views
            assertThat(it.cardMultilineWidget.childCount).isEqualTo(2)
            // tl_card_number
            assertThat(it.cardMultilineWidget.getChildAt(0)).isInstanceOf(
                CardNumberTextInputLayout::class.java
            )
            // second_row_layout
            assertThat(it.cardMultilineWidget.getChildAt(1)).isInstanceOf(
                LinearLayout::class.java
            )

            // 1 horizontal divider added to tl_card_number, now it has 2 child views
            assertThat(it.cardMultilineWidget.cardNumberTextInputLayout.childCount).isEqualTo(
                2
            )

            // no vertical divider added between exp and cvc, secondRowLayout still has 3 child views
            assertThat(it.cardMultilineWidget.secondRowLayout.childCount).isEqualTo(
                3
            )

            // 1 horizontal divider added below tl_expiry, now it has 2 child views
            assertThat(it.cardMultilineWidget.expiryTextInputLayout.childCount).isEqualTo(
                2
            )

            // 1 horizontal divider added below tl_cvc, now it has 2 child views
            assertThat(it.cardMultilineWidget.cvcInputLayout.childCount).isEqualTo(2)

            // 1 horizontal divider added below countryLayout, now it has 2 child views
            assertThat(it.countryLayout.childCount).isEqualTo(2)

            // divider with width=match_parent is invisible
            assertThat(it.countryPostalDivider.isVisible).isFalse()
        }
    }

    @Test
    fun testCardValidCallback() {
        standardBinding!!.let {
            var currentIsValid = false
            var currentInvalidFields = emptySet<CardValidCallback.Fields>()
            standardCardFormView.setCardValidCallback { isValid, invalidFields ->
                currentIsValid = isValid
                currentInvalidFields = invalidFields
            }

            assertThat(currentIsValid)
                .isFalse()
            assertThat(currentInvalidFields)
                .containsExactly(
                    CardValidCallback.Fields.Number,
                    CardValidCallback.Fields.Expiry,
                    CardValidCallback.Fields.Cvc,
                    CardValidCallback.Fields.Postal
                )

            it.cardMultilineWidget.cardNumberEditText.setText(VISA_WITH_SPACES)
            assertThat(currentIsValid)
                .isFalse()
            assertThat(currentInvalidFields)
                .containsExactly(
                    CardValidCallback.Fields.Expiry,
                    CardValidCallback.Fields.Cvc,
                    CardValidCallback.Fields.Postal
                )

            it.cardMultilineWidget.expiryDateEditText.append("12")
            assertThat(currentIsValid)
                .isFalse()
            assertThat(currentInvalidFields)
                .containsExactly(
                    CardValidCallback.Fields.Expiry,
                    CardValidCallback.Fields.Cvc,
                    CardValidCallback.Fields.Postal
                )

            it.cardMultilineWidget.expiryDateEditText.append("50")
            assertThat(currentIsValid)
                .isFalse()
            assertThat(currentInvalidFields)
                .containsExactly(
                    CardValidCallback.Fields.Cvc,
                    CardValidCallback.Fields.Postal
                )

            it.cardMultilineWidget.cvcEditText.append("12")
            assertThat(currentIsValid)
                .isFalse()
            assertThat(currentInvalidFields)
                .containsExactly(
                    CardValidCallback.Fields.Cvc,
                    CardValidCallback.Fields.Postal
                )

            it.cardMultilineWidget.cvcEditText.append("3")
            assertThat(currentIsValid)
                .isFalse()
            assertThat(currentInvalidFields)
                .containsExactly(
                    CardValidCallback.Fields.Postal
                )

            it.postalCode.setText(VALID_US_ZIP)
            assertThat(currentIsValid)
                .isTrue()
            assertThat(currentInvalidFields)
                .isEmpty()
        }
    }

    private fun setValuesToStandardUI(
        visa: String,
        month: String,
        year: String,
        cvc: String,
        zip: String? = null
    ) {
        standardBinding!!.let {
            it.cardMultilineWidget.cardNumberEditText.setText(visa)
            it.cardMultilineWidget.expiryDateEditText.append(month)
            it.cardMultilineWidget.expiryDateEditText.append(year)
            it.cardMultilineWidget.cvcEditText.append(cvc)
            zip?.let { zipString ->
                it.postalCode.setText(zipString)
            }
        }
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
