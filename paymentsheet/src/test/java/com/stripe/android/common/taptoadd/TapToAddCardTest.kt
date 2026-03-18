package com.stripe.android.common.taptoadd

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.ui.TapToAddCard
import com.stripe.android.common.taptoadd.ui.TapToAddTheme
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TapToAddCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testBitmap = TapToAddImageRepository.CardArt(
        bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888),
        textColor = Color.White,
    )

    @Test
    fun `when imageRepository is null shows placeholder and last4`() = runTest {
        composeTestRule.setContent {
            TapToAddTheme(imageRepository = null) {
                TapToAddCard(cardBrand = CardBrand.Visa, last4 = "4242")
            }
        }

        composeTestRule.onNodeWithTag("TAP_TO_ADD_CARD").assertExists()
        composeTestRule.onNodeWithText("···· 4242").assertExists()
    }

    @Test
    fun `when imageRepository is null and last4 is null shows only placeholder`() = runTest {
        composeTestRule.setContent {
            TapToAddTheme(imageRepository = null) {
                TapToAddCard(cardBrand = CardBrand.Visa, last4 = null)
            }
        }

        composeTestRule.onNodeWithTag("TAP_TO_ADD_CARD").assertExists()
        composeTestRule.onNodeWithText("····").assertDoesNotExist()
    }

    @Test
    fun `when get returns cached art does not call load and shows cached art with last4`() = runTest {
        val repository = FakeTapToAddImageRepository(
            onGet = testBitmap,
            onLoad = CompletableDeferred(null),
        )

        composeTestRule.setContent {
            TapToAddTheme(imageRepository = repository) {
                TapToAddCard(cardBrand = CardBrand.Visa, last4 = "1234")
            }
        }

        composeTestRule.awaitIdle()

        assertThat(repository.getCalls.awaitItem()).isEqualTo(CardBrand.Visa)
        assertThat(repository.getCalls.awaitItem()).isEqualTo(CardBrand.Visa)

        composeTestRule.onNodeWithText("···· 1234").assertExists()

        repository.validate()
    }

    @Test
    fun `when get returns null calls load and shows art and last4 after load completes`() = runTest {
        val repository = FakeTapToAddImageRepository(
            onGet = null,
            onLoad = CompletableDeferred(testBitmap),
        )

        composeTestRule.setContent {
            TapToAddTheme(imageRepository = repository) {
                TapToAddCard(cardBrand = CardBrand.MasterCard, last4 = "5678")
            }
        }

        composeTestRule.awaitIdle()

        assertThat(repository.getCalls.awaitItem()).isEqualTo(CardBrand.MasterCard)
        assertThat(repository.getCalls.awaitItem()).isEqualTo(CardBrand.MasterCard)
        assertThat(repository.loadCalls.awaitItem()).isEqualTo(CardBrand.MasterCard)

        composeTestRule.onNodeWithText("···· 5678").assertExists()

        repository.validate()
    }

    @Test
    fun `when get returns null and load returns null shows placeholder and last4`() = runTest {
        val repository = FakeTapToAddImageRepository(
            onGet = null,
            onLoad = CompletableDeferred(null),
        )

        composeTestRule.setContent {
            TapToAddTheme(imageRepository = repository) {
                TapToAddCard(cardBrand = CardBrand.Unknown, last4 = "9999")
            }
        }

        composeTestRule.awaitIdle()

        assertThat(repository.getCalls.awaitItem()).isEqualTo(CardBrand.Unknown)
        assertThat(repository.getCalls.awaitItem()).isEqualTo(CardBrand.Unknown)
        assertThat(repository.loadCalls.awaitItem()).isEqualTo(CardBrand.Unknown)

        composeTestRule.onNodeWithTag("TAP_TO_ADD_CARD").assertExists()
        composeTestRule.onNodeWithText("···· 9999").assertExists()

        repository.validate()
    }

    @Test
    fun `get is called with correct card brand`() = runTest {
        val repository = FakeTapToAddImageRepository(
            onGet = testBitmap,
            onLoad = CompletableDeferred(null),
        )

        composeTestRule.setContent {
            TapToAddTheme(imageRepository = repository) {
                TapToAddCard(cardBrand = CardBrand.Discover, last4 = "0000")
            }
        }

        composeTestRule.awaitIdle()

        assertThat(repository.getCalls.awaitItem()).isEqualTo(CardBrand.Discover)
        assertThat(repository.getCalls.awaitItem()).isEqualTo(CardBrand.Discover)

        repository.validate()
    }
}
