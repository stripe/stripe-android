package com.stripe.android.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.utils.createTestActivityRule
import com.stripe.android.view.CardFormViewTestActivity.Companion.VIEW_ID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
internal class CardBrandViewTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val testActivityRule = createTestActivityRule<CardBrandViewTestActivity>(useMaterial = true)

    @Test
    fun `networkParams should be present when brand is unknown and preferred networks is set`() = runCardBrandViewTest {
        cardBrandView.apply {
            isCbcEligible = true
            possibleBrands = listOf(CardBrand.CartesBancaires, CardBrand.MasterCard)
            merchantPreferredNetworks = listOf(CardBrand.CartesBancaires)
        }

        assertThat(cardBrandView.paymentMethodCreateParamsNetworks()?.preferred)
            .isEqualTo(CardBrand.CartesBancaires.code)
        assertThat(cardBrandView.cardParamsNetworks()?.preferred).isEqualTo(CardBrand.CartesBancaires.code)
    }

    @Test
    fun `networkParams should be present, cbc ineligible, brand is unknown and preferred networks is set`() =
        runCardBrandViewTest {
            cardBrandView.apply {
                possibleBrands = listOf(CardBrand.CartesBancaires, CardBrand.MasterCard)
                merchantPreferredNetworks = listOf(CardBrand.CartesBancaires)
            }

            assertThat(cardBrandView.paymentMethodCreateParamsNetworks()?.preferred)
                .isEqualTo(CardBrand.CartesBancaires.code)
            assertThat(cardBrandView.cardParamsNetworks()?.preferred).isEqualTo(CardBrand.CartesBancaires.code)
        }

    @Test
    fun `networkParams should be present when brand is known, possible cards empty and preferred networks is set`() =
        runCardBrandViewTest {
            cardBrandView.apply {
                isCbcEligible = true
                brand = CardBrand.MasterCard
                merchantPreferredNetworks = listOf(CardBrand.CartesBancaires)
            }

            assertThat(cardBrandView.paymentMethodCreateParamsNetworks()?.preferred)
                .isEqualTo(CardBrand.CartesBancaires.code)
            assertThat(cardBrandView.cardParamsNetworks()?.preferred)
                .isEqualTo(CardBrand.CartesBancaires.code)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `networkParams should be present when brand known, cbc ineligible, possible cards empty and preferred networks set`() =
        runCardBrandViewTest {
            cardBrandView.apply {
                brand = CardBrand.MasterCard
                merchantPreferredNetworks = listOf(CardBrand.CartesBancaires)
            }

            assertThat(cardBrandView.paymentMethodCreateParamsNetworks()?.preferred)
                .isEqualTo(CardBrand.CartesBancaires.code)
            assertThat(cardBrandView.cardParamsNetworks()?.preferred)
                .isEqualTo(CardBrand.CartesBancaires.code)
        }

    @Test
    fun `networkParams should not be present when brand is unknown and preferred networks is not set`() =
        runCardBrandViewTest {
            cardBrandView.apply {
                isCbcEligible = true
                possibleBrands = listOf(CardBrand.CartesBancaires, CardBrand.MasterCard)
            }

            assertThat(cardBrandView.paymentMethodCreateParamsNetworks()?.preferred).isEqualTo(null)
            assertThat(cardBrandView.cardParamsNetworks()?.preferred).isEqualTo(null)
        }

    @Test
    fun `networkParams shouldn't be present when brand is unknown, cbc ineligible and preferred networks is not set`() =
        runCardBrandViewTest {
            cardBrandView.apply {
                possibleBrands = listOf(CardBrand.CartesBancaires, CardBrand.MasterCard)
            }

            assertThat(cardBrandView.paymentMethodCreateParamsNetworks()?.preferred).isEqualTo(null)
            assertThat(cardBrandView.cardParamsNetworks()?.preferred).isEqualTo(null)
        }

    @Test
    fun `networkParams should be present when user selects brand and preferred networks is set`() =
        runCardBrandViewTest {
            cardBrandView.apply {
                isCbcEligible = true
                possibleBrands = listOf(CardBrand.CartesBancaires, CardBrand.MasterCard)
                handleBrandSelected(CardBrand.MasterCard)
                merchantPreferredNetworks = listOf(CardBrand.CartesBancaires)
            }

            assertThat(cardBrandView.paymentMethodCreateParamsNetworks()?.preferred)
                .isEqualTo(CardBrand.MasterCard.code)
            assertThat(cardBrandView.cardParamsNetworks()?.preferred)
                .isEqualTo(CardBrand.MasterCard.code)
        }

    private fun runCardBrandViewTest(
        locale: Locale = Locale.US,
        block: TestContext.() -> Unit,
    ) {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(locale)

        val activityScenario = ActivityScenario.launch<CardBrandViewTestActivity>(
            Intent(context, CardBrandViewTestActivity::class.java)
        )

        activityScenario.onActivity { activity ->
            val cardBrandView = activity.findViewById<CardBrandView>(VIEW_ID)

            val testContext = TestContext(cardBrandView)
            block(testContext)
        }

        activityScenario.close()
        Locale.setDefault(originalLocale)
    }

    private class TestContext(
        val cardBrandView: CardBrandView,
    )
}

internal class CardBrandViewTestActivity : AppCompatActivity() {

    private val cardBrandView: CardBrandView by lazy {
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.cardFormStyle, "0")
            .build()

        CardBrandView(this, attributes).apply {
            id = VIEW_ID
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.StripeDefaultTheme)

        val layout = LinearLayout(this).apply {
            addView(cardBrandView)
        }
        setContentView(layout)
    }

    companion object {
        const val VIEW_ID = 12345
    }
}
