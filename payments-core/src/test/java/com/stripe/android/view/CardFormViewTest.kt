package com.stripe.android.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardNumberFixtures
import com.stripe.android.CardNumberFixtures.CO_BRAND_CARTES_MASTERCARD_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.databinding.StripeCardFormViewBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.utils.CardElementTestHelper
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.createTestActivityRule
import com.stripe.android.view.CardFormViewTestActivity.Companion.VIEW_ID
import kotlinx.parcelize.Parcelize
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import com.stripe.android.uicore.R as UiCoreR

@RunWith(RobolectricTestRunner::class)
internal class CardFormViewTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val testActivityRule = createTestActivityRule<CardFormViewTestActivity>(useMaterial = true)

    @Before
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun `when locale is US then country should be US and postal config should be US`() {
        runCardFormViewTest(locale = Locale.US) {
            assertThat(
                binding.countryLayout.countryAutocomplete.text.toString()
            ).isEqualTo("United States")

            assertThat(
                binding.postalCode.config
            ).isEqualTo(PostalCodeEditText.Config.US)
        }
    }

    @Test
    fun `when locale is not US then country should not be US and postal config should be Global`() {
        runCardFormViewTest(locale = Locale.CANADA) {
            assertThat(
                binding.countryLayout.countryAutocomplete.text.toString()
            ).isEqualTo("Canada")

            assertThat(
                binding.postalCode.config
            ).isEqualTo(PostalCodeEditText.Config.Global)
        }
    }

    @Test
    fun `when all fields are valid then paymentMethodCreateParams should return correctly and errors is empty`() {
        runCardFormViewTest {
            binding.populate(VISA_WITH_SPACES, VALID_MONTH, VALID_YEAR, VALID_CVC, VALID_US_ZIP)

            assertThat(cardFormView.paymentMethodCreateParams)
                .isEqualTo(
                    PaymentMethodCreateParams.create(
                        card = PaymentMethodCreateParams.Card(
                            attribution = setOf(CardFormView.CARD_FORM_VIEW),
                            number = CardNumberFixtures.VISA_NO_SPACES,
                            expiryMonth = 12,
                            expiryYear = 2050,
                            cvc = VALID_CVC,
                        ),
                    )
                )

            idleLooper()

            binding.let {
                assertThat(it.errors.text).isEqualTo("")
                assertThat(it.errors.isVisible).isFalse()
            }
        }
    }

    @Test
    fun `when preferred network is set then paymentMethodCreateParams should return contain preferred network`() {
        runCardFormViewTest {
            binding.populate(CO_BRAND_CARTES_MASTERCARD_WITH_SPACES, VALID_MONTH, VALID_YEAR, VALID_CVC, VALID_US_ZIP)

            binding.cardMultilineWidget.setPreferredNetworks(listOf(CardBrand.CartesBancaires))

            assertThat(cardFormView.paymentMethodCreateParams?.card?.networks?.preferred)
                .isEqualTo(CardBrand.CartesBancaires.code)
        }
    }

    @Test
    fun `when postal field is invalid then paymentMethodCreateParams should return null and errors is not empty`() {
        runCardFormViewTest {
            binding.populate(VISA_WITH_SPACES, VALID_MONTH, VALID_YEAR, VALID_CVC, INVALID_US_ZIP)

            assertThat(cardFormView.paymentMethodCreateParams).isNull()

            idleLooper()

            binding.let {
                assertThat(it.errors.text).isEqualTo(context.getString(UiCoreR.string.stripe_address_zip_invalid))
                assertThat(it.errors.isVisible).isTrue()
            }
        }
    }

    @Test
    fun `when card number is invalid then paymentMethodCreateParams should return null and errors is not empty`() {
        runCardFormViewTest {
            binding.populate(INVALID_VISA, VALID_MONTH, VALID_YEAR, VALID_CVC, VALID_US_ZIP)

            assertThat(cardFormView.paymentMethodCreateParams).isNull()

            idleLooper()

            binding.let {
                assertThat(it.errors.text).isEqualTo(context.getString(R.string.stripe_invalid_card_number))
                assertThat(it.errors.isVisible).isTrue()
            }
        }
    }

    @Test
    fun `when expiration is invalid then paymentMethodCreateParams should return null and errors is not empty`() {
        runCardFormViewTest {
            binding.populate(VISA_WITH_SPACES, VALID_MONTH, INVALID_YEAR, VALID_CVC, VALID_US_ZIP)

            assertThat(cardFormView.paymentMethodCreateParams).isNull()

            idleLooper()

            binding.let {
                assertThat(it.errors.text).isEqualTo(context.getString(UiCoreR.string.stripe_invalid_expiry_year))
                assertThat(it.errors.isVisible).isTrue()
            }
        }
    }

    @Test
    fun `when cvc is invalid then paymentMethodCreateParams should return null and errors is not empty`() {
        runCardFormViewTest {
            binding.populate(VISA_WITH_SPACES, VALID_MONTH, VALID_YEAR, INVALID_CVC, VALID_US_ZIP)

            assertThat(cardFormView.paymentMethodCreateParams).isNull()

            idleLooper()

            binding.let {
                assertThat(it.errors.text).isEqualTo(context.getString(R.string.stripe_invalid_cvc))
                assertThat(it.errors.isVisible).isTrue()
            }
        }
    }

    @Test
    fun `when cvc becomes valid then postal should get focus`() = runCardFormViewTest {
        binding.populate(VISA_WITH_SPACES, VALID_MONTH, VALID_YEAR, INVALID_CVC)

        assertThat(binding.postalCode.hasFocus()).isFalse()

        binding.cardMultilineWidget.cvcEditText.append("3")
        idleLooper()

        assertThat(binding.postalCode.hasFocus()).isTrue()
    }

    @Test
    fun verifyStandardStyle() = runCardFormViewTest {
        idleLooper()
        binding.let {
            // 2 additional horizontal dividers added to card_multiline_widget.xml, now it has 4 child views
            assertThat(it.cardMultilineWidget.childCount).isEqualTo(4)
            // tl_card_number
            assertThat((it.cardMultilineWidget.getChildAt(0) as FrameLayout).getChildAt(0)).isInstanceOf(
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
    fun verifyBorderlessStyle() = runCardFormViewTest(borderless = true) {
        idleLooper()
        binding.let {
            // no child views added to card_multiline_widget.xml, it still has 2 child views
            assertThat(it.cardMultilineWidget.childCount).isEqualTo(2)
            // tl_card_number
            assertThat((it.cardMultilineWidget.getChildAt(0) as FrameLayout).getChildAt(0)).isInstanceOf(
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
    fun testCardValidCallback() = runCardFormViewTest {
        binding.let {
            var currentIsValid = false
            var currentInvalidFields = emptySet<CardValidCallback.Fields>()
            cardFormView.setCardValidCallback { isValid, invalidFields ->
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

    @Test
    fun `Returns the correct create params when user selects no brand in CBC flow`() {
        runCardFormViewTest(isCbcEligible = true) {
            binding.populate(
                visa = "4000002500001001",
                month = "12",
                year = "2030",
                cvc = "123",
                zip = "12345"
            )

            val cardParams = cardFormView.paymentMethodCreateParams?.card
            assertThat(cardParams?.networks?.preferred).isNull()
        }
    }

    @Test
    fun `Returns the correct create params when user selects preferred network`() {
        runCardFormViewTest(isCbcEligible = true) {
            binding.populate(CO_BRAND_CARTES_MASTERCARD_WITH_SPACES, VALID_MONTH, VALID_YEAR, VALID_CVC, VALID_US_ZIP)

            binding.cardMultilineWidget.setPreferredNetworks(listOf(CardBrand.CartesBancaires))

            val cardParams = cardFormView.paymentMethodCreateParams?.card
            assertThat(cardParams?.networks?.preferred).isEqualTo(CardBrand.CartesBancaires.code)
        }
    }

    @Test
    fun `Returns the correct create params when user selects a brand in CBC flow`() {
        runCardFormViewTest(isCbcEligible = true) {
            binding.populate(
                visa = "4000002500001001",
                month = "12",
                year = "2030",
                cvc = "123",
                zip = "12345"
            )

            binding.cardMultilineWidget.cardBrandView.brand = CardBrand.CartesBancaires

            val cardParams = cardFormView.paymentMethodCreateParams?.card
            assertThat(cardParams?.networks?.preferred).isNull()
        }
    }

    private fun StripeCardFormViewBinding.populate(
        visa: String,
        month: String,
        year: String,
        cvc: String,
        zip: String? = null
    ) {
        cardMultilineWidget.cardNumberEditText.setText(visa)
        cardMultilineWidget.expiryDateEditText.append(month)
        cardMultilineWidget.expiryDateEditText.append(year)
        cardMultilineWidget.cvcEditText.append(cvc)
        zip?.let { zipString ->
            postalCode.setText(zipString)
        }
    }

    private fun runCardFormViewTest(
        isCbcEligible: Boolean = false,
        borderless: Boolean = false,
        locale: Locale = Locale.US,
        block: TestContext.() -> Unit,
    ) {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(locale)

        val activityScenario = ActivityScenario.launch<CardFormViewTestActivity>(
            Intent(context, CardFormViewTestActivity::class.java).apply {
                putExtra("args", CardFormViewTestActivity.Args(isCbcEligible, borderless))
            }
        )

        activityScenario.onActivity { activity ->
            val cardFormView = activity.findViewById<CardFormView>(VIEW_ID)
            val binding = StripeCardFormViewBinding.bind(cardFormView)

            val testContext = TestContext(cardFormView, binding)
            block(testContext)
        }

        activityScenario.close()
        Locale.setDefault(originalLocale)
    }

    private class TestContext(
        val cardFormView: CardFormView,
        val binding: StripeCardFormViewBinding,
    )

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

internal class CardFormViewTestActivity : AppCompatActivity() {

    @Parcelize
    data class Args(
        val isCbcEligible: Boolean,
        val borderless: Boolean,
    ) : Parcelable

    private val args: Args by lazy {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra("args")!!
    }

    private val cardFormView: CardFormView by lazy {
        val style = if (args.borderless) "1" else "0"
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.cardFormStyle, style)
            .build()

        CardFormView(this, attributes).apply {
            id = VIEW_ID

            val storeOwner = CardElementTestHelper.createViewModelStoreOwner(isCbcEligible = args.isCbcEligible)
            viewModelStoreOwner = storeOwner
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.StripeDefaultTheme)

        val layout = LinearLayout(this).apply {
            addView(cardFormView)
        }
        setContentView(layout)
    }

    companion object {
        const val VIEW_ID = 12345
    }
}
