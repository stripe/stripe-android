package com.stripe.android.view

import android.content.Context
import android.widget.AutoCompleteTextView
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSessionData
import com.stripe.android.PaymentSessionFixtures
import com.stripe.android.R
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.model.getCountryCode
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.createTestActivityRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for [CountryTextInputLayout]
 */
@RunWith(RobolectricTestRunner::class)
class CountryTextInputLayoutTest {
    private lateinit var countryTextInputLayout: CountryTextInputLayout
    private lateinit var autoCompleteTextView: AutoCompleteTextView

    private val ephemeralKeyProvider: EphemeralKeyProvider = mock()

    private val activityScenarioFactory = ActivityScenarioFactory(
        ApplicationProvider.getApplicationContext()
    )

    @get:Rule
    internal val testActivityRule = createTestActivityRule<ActivityScenarioFactory.TestActivity>()

    @BeforeTest
    fun setup() {
        Locale.setDefault(Locale.US)

        val context: Context = ApplicationProvider.getApplicationContext()
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        CustomerSession.initCustomerSession(context, ephemeralKeyProvider)

        val config = PaymentSessionFixtures.CONFIG.copy(
            prepopulatedShippingInfo = null,
            allowedShippingCountryCodes = emptySet()
        )
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args(
                paymentSessionConfig = config,
                paymentSessionData = PaymentSessionData(config)
            )
        ).use { activityScenario ->
            activityScenario.onActivity {
                countryTextInputLayout = it
                    .findViewById(R.id.country_autocomplete_aaw)
                autoCompleteTextView = countryTextInputLayout.countryAutocomplete
            }
        }
    }

    @Test
    fun countryAutoCompleteTextView_whenInitialized_displaysDefaultLocaleDisplayName() {
        assertEquals(Locale.US.getCountryCode(), countryTextInputLayout.selectedCountryCode)
        assertEquals(Locale.US.displayCountry, autoCompleteTextView.text.toString())
    }

    @Test
    fun updateUIForCountryEntered_whenInvalidCountry_revertsToLastCountry() {
        val previousValidCountryCode =
            countryTextInputLayout.selectedCountryCode?.value.orEmpty()
        countryTextInputLayout.setCountrySelected(CountryCode.create("FAKE COUNTRY CODE"))
        assertNull(autoCompleteTextView.error)
        assertEquals(
            autoCompleteTextView.text.toString(),
            Locale("", previousValidCountryCode).displayCountry
        )
        countryTextInputLayout.setCountrySelected(Locale.UK.getCountryCode())
        assertNotEquals(
            autoCompleteTextView.text.toString(),
            Locale("", previousValidCountryCode).displayCountry
        )
        assertEquals(autoCompleteTextView.text.toString(), Locale.UK.displayCountry)
    }

    @Test
    fun updateUIForCountryEntered_whenValidCountry_UIUpdates() {
        assertEquals(Locale.US.getCountryCode(), countryTextInputLayout.selectedCountryCode)
        countryTextInputLayout.setCountrySelected(Locale.UK.getCountryCode())
        assertEquals(Locale.UK.getCountryCode(), countryTextInputLayout.selectedCountryCode)
    }

    @Test
    fun countryAutoCompleteTextView_onInputFocus_displayDropDown() {
        autoCompleteTextView.clearFocus()
        assertFalse(autoCompleteTextView.isPopupShowing)
        autoCompleteTextView.requestFocus()
        assertTrue(autoCompleteTextView.isPopupShowing)
    }

    @Test
    fun setAllowedCountryCodes_withPopulatedSet_shouldUpdateSelectedCountry() {
        countryTextInputLayout.setAllowedCountryCodes(setOf("fr", "de"))
        assertEquals(
            "FR",
            countryTextInputLayout.selectedCountryCode?.value
        )
    }

    @Test
    fun validateCountry_withInvalidCountry_setsSelectedCountryToNull() {
        assertNotNull(countryTextInputLayout.selectedCountryCode)
        countryTextInputLayout.countryAutocomplete.setText("invalid country")
        countryTextInputLayout.validateCountry()
        assertNull(countryTextInputLayout.selectedCountryCode)
    }

    @Test
    fun validateCountry_withValidCountry_setsSelectedCountry() {
        assertNotNull(countryTextInputLayout.selectedCountryCode)
        countryTextInputLayout.countryAutocomplete.setText("Canada")
        countryTextInputLayout.validateCountry()
        assertEquals(
            "Canada",
            CountryUtils.getDisplayCountry(
                requireNotNull(countryTextInputLayout.selectedCountryCode),
                Locale.getDefault()
            )
        )
    }

    @Test
    fun `when screen rotates then selected country should carry over`() {
        val oldCountryTextInputLayout = activityScenarioFactory.createView { activity ->
            CountryTextInputLayout(activity)
        }

        idleLooper()

        assertEquals(CountryCode.US, oldCountryTextInputLayout.selectedCountryCode)
        assertEquals("United States", oldCountryTextInputLayout.countryAutocomplete.text.toString())

        oldCountryTextInputLayout.updatedSelectedCountryCode(CountryCode.CA)
        oldCountryTextInputLayout.updateUiForCountryEntered(CountryCode.CA)

        assertEquals(CountryCode.CA, oldCountryTextInputLayout.selectedCountryCode)
        assertEquals("Canada", oldCountryTextInputLayout.countryAutocomplete.text.toString())

        // mimic configuration change - the old instance's state is saved and passed to
        // a new instance's onRestoreInstanceState.
        // activityScenario.recreate() won't trigger onRestoreInstanceState
        val oldState = oldCountryTextInputLayout.onSaveInstanceState()

        val newCountryTextInputLayout = activityScenarioFactory.createView { activity ->
            CountryTextInputLayout(activity)
        }

        idleLooper()

        // newCountryTextInputLayout.onRestoreInstanceState() is triggered during configuration change
        newCountryTextInputLayout.restoreSelectedCountry(oldState as CountryTextInputLayout.SelectedCountryState)

        assertEquals(CountryCode.CA, oldCountryTextInputLayout.selectedCountryCode)
        assertEquals("Canada", oldCountryTextInputLayout.countryAutocomplete.text.toString())
    }

    @AfterTest
    fun teardown() {
        Locale.setDefault(Locale.US)
    }
}
