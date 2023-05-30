package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryCode
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.CountryNotListedDestination
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentIndividualPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.elements.PHONE_NUMBER_TEXT_FIELD_TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class IndividualScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.individual).thenReturn(
            VerificationPageStaticContentIndividualPage(
                addressCountries = listOf(US, SG, BR),
                buttonText = BUTTON_TEXT,
                title = TITLE,
                idNumberCountries = listOf(US, SG),
                idNumberCountryNotListedTextButtonText = ID_COUNTRY_NOT_LISTED_BUTTON_TEXT,
                addressCountryNotListedTextButtonText = ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TEXT,
                phoneNumberCountries = listOf(US, SG)
            )
        )
    }

    private val missingRequirementsFlow = MutableStateFlow<Set<Requirement>>(setOf())

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn MutableLiveData(Resource.success(verificationPage))
        on { missingRequirements } doReturn missingRequirementsFlow
        on { addressSchemaRepository } doReturn mock()
    }
    private val mockNavController = mock<NavController>()

    @Test
    fun testMissingID() {
        setComposeTestRuleWith(
            setOf(Requirement.IDNUMBER)
        ) {
            onNodeWithTag(INDIVIDUAL_SUBMIT_BUTTON_TAG).onChildAt(0).assertTextEquals(
                BUTTON_TEXT.uppercase()
            )
            onNodeWithTag(INDIVIDUAL_TITLE_TAG).assertTextEquals(TITLE)

            onNodeWithTag(ID_NUMBER_COUNTRY_NOT_LISTED_BUTTON_TAG).assertTextEquals(
                ID_COUNTRY_NOT_LISTED_BUTTON_TEXT
            )
            onNodeWithTag(ID_NUMBER_COUNTRY_NOT_LISTED_BUTTON_TAG).performClick()

            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(CountryNotListedDestination.COUNTRY_NOT_LISTED)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun testMissingAddress() {
        setComposeTestRuleWith(
            setOf(Requirement.ADDRESS)
        ) {
            onNodeWithTag(INDIVIDUAL_SUBMIT_BUTTON_TAG).onChildAt(0).assertTextEquals(
                BUTTON_TEXT.uppercase()
            )
            onNodeWithTag(INDIVIDUAL_TITLE_TAG).assertTextEquals(TITLE)

            onNodeWithTag(ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TAG).assertTextEquals(
                ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TEXT
            )
            onNodeWithTag(ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TAG).performClick()

            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(CountryNotListedDestination.COUNTRY_NOT_LISTED)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun testMissingPhoneNumber() {
        setComposeTestRuleWith(
            setOf(Requirement.PHONE_NUMBER)
        ) {
            onNodeWithTag(INDIVIDUAL_SUBMIT_BUTTON_TAG).onChildAt(0).assertTextEquals(
                BUTTON_TEXT.uppercase()
            )
            onNodeWithTag(INDIVIDUAL_TITLE_TAG).assertTextEquals(TITLE)

            onNodeWithTag(INDIVIDUAL_SUBMIT_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()

            onNodeWithTag(PHONE_NUMBER_TEXT_FIELD_TAG).performTextInput("415123456") // incomplete US number
            onNodeWithTag(INDIVIDUAL_SUBMIT_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()

            onNodeWithTag(PHONE_NUMBER_TEXT_FIELD_TAG).performTextInput("4151234567") // complete US number
            onNodeWithTag(INDIVIDUAL_SUBMIT_BUTTON_TAG).onChildAt(0).assertIsEnabled()
        }
    }

    private fun setComposeTestRuleWith(
        missingRequirements: Set<Requirement>,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        missingRequirementsFlow.update { missingRequirements }
        composeTestRule.setContent {
            IndividualScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel
            )
        }

        with(composeTestRule, testBlock)
    }

    private companion object {
        private val US = Country(CountryCode("US"), "United States")
        private val SG = Country(CountryCode("SG"), "Singapore")
        private val BR = Country(CountryCode("BR"), "Brazil")
        private const val BUTTON_TEXT = "cancel"
        private const val TITLE = "Verify your information"
        private const val ID_COUNTRY_NOT_LISTED_BUTTON_TEXT = "Id Country not listed"
        private const val ADDRESS_COUNTRY_NOT_LISTED_BUTTON_TEXT = "Address Country not listed"
    }
}
