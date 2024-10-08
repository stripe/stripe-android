package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.utils.createTestActivityRule
import kotlinx.parcelize.Parcelize
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

/**
 * Tests that [LifecycleOwnerDelegate] is properly stubbing required dependencies to display CardElement views
 * on base [Activity]. Removing [LifecycleOwnerDelegate] calls from `onAttachedToWindow` for [CardInputWidget],
 * [CardFormView], or [CardMultilineWidget] will cause these to fail.
 */
@RunWith(RobolectricTestRunner::class)
internal class CardElementActivityTests {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val testActivityRule = createTestActivityRule<CardElementTestActivity>(useMaterial = true)

    @Before
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun `displays CardFormView on Activity`() {
        runCardElementTest(CardElementTestActivity.CardElementView.CardFormView)
    }

    @Test
    fun `displays CardInputWidget on Activity`() {
        runCardElementTest(CardElementTestActivity.CardElementView.CardInputWidget)
    }

    @Test
    fun `displays CardMultilineWidget on Activity`() {
        runCardElementTest(CardElementTestActivity.CardElementView.CardMultilineWidget)
    }

    private fun runCardElementTest(
        viewType: CardElementTestActivity.CardElementView,
        locale: Locale = Locale.US
    ) {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(locale)

        val activityScenario = ActivityScenario.launch<CardElementTestActivity>(
            Intent(context, CardElementTestActivity::class.java).apply {
                putExtra("args", CardElementTestActivity.Args(viewType))
            }
        )

        activityScenario.close()
        Locale.setDefault(originalLocale)
    }
}

internal class CardElementTestActivity : Activity() {

    @Parcelize
    data class Args(
        val viewType: CardElementView
    ) : Parcelable

    enum class CardElementView {
        CardFormView,
        CardInputWidget,
        CardMultilineWidget
    }

    private val args: Args by lazy {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra("args")!!
    }

    private val view by lazy {
        when (args.viewType) {
            CardElementView.CardFormView -> CardFormView(this)
            CardElementView.CardInputWidget -> CardInputWidget(this)
            CardElementView.CardMultilineWidget -> CardMultilineWidget(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.StripeDefaultTheme)

        val layout = LinearLayout(this).apply {
            addView(view)
        }
        setContentView(layout)
    }
}
