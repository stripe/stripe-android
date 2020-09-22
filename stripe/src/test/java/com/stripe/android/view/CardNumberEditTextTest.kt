package com.stripe.android.view

import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.AnalyticsDataFactory
import com.stripe.android.AnalyticsRequest
import com.stripe.android.AnalyticsRequestExecutor
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.ApiRequest
import com.stripe.android.CardNumberFixtures
import com.stripe.android.CardNumberFixtures.AMEX_BIN
import com.stripe.android.CardNumberFixtures.AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_14_WITH_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_16_NO_SPACES
import com.stripe.android.CardNumberFixtures.DINERS_CLUB_16_WITH_SPACES
import com.stripe.android.CardNumberFixtures.JCB_NO_SPACES
import com.stripe.android.CardNumberFixtures.JCB_WITH_SPACES
import com.stripe.android.CardNumberFixtures.UNIONPAY_BIN
import com.stripe.android.CardNumberFixtures.UNIONPAY_NO_SPACES
import com.stripe.android.CardNumberFixtures.UNIONPAY_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VISA_BIN
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VISA_WITH_SPACES
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.cards.AccountRangeFixtures
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.LegacyCardAccountRangeRepository
import com.stripe.android.cards.NullCardAccountRangeRepository
import com.stripe.android.cards.StaticCardAccountRangeSource
import com.stripe.android.cards.StaticCardAccountRanges
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.testharness.ViewTestUtils
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test class for [CardNumberEditText].
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
internal class CardNumberEditTextTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripeDefaultTheme
    )
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private var completionCallbackInvocations = 0
    private val completionCallback: () -> Unit = { completionCallbackInvocations++ }

    private var lastBrandChangeCallbackInvocation: CardBrand? = null
    private val brandChangeCallback: (CardBrand) -> Unit = {
        lastBrandChangeCallbackInvocation = it
    }

    private val cardAccountRangeRepository = LegacyCardAccountRangeRepository(
        StaticCardAccountRangeSource()
    )

    private val analyticsRequestExecutor = AnalyticsRequestExecutor {}
    private val analyticsRequestFactory = AnalyticsRequest.Factory()
    private val analyticsDataFactory = AnalyticsDataFactory(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    private val publishableKeySupplier = { ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY }

    private val cardNumberEditText = CardNumberEditText(
        context,
        workContext = testDispatcher,
        cardAccountRangeRepository = cardAccountRangeRepository,
        analyticsRequestExecutor = analyticsRequestExecutor,
        analyticsRequestFactory = analyticsRequestFactory,
        analyticsDataFactory = analyticsDataFactory,
        publishableKeySupplier = publishableKeySupplier
    ).also {
        it.completionCallback = completionCallback
        it.brandChangeCallback = brandChangeCallback
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun calculateCursorPosition_whenVisa_increasesIndexWhenGoingPastTheSpaces() = testDispatcher.runBlockingTest {
        // Adding 1 character, starting at position 4, with a final string length 6
        assertThat(
            cardNumberEditText.calculateCursorPosition(6, 4, 1)
        ).isEqualTo(6)
        assertThat(
            cardNumberEditText.calculateCursorPosition(8, 4, 1)
        ).isEqualTo(6)
        assertThat(
            cardNumberEditText.calculateCursorPosition(11, 9, 1)
        ).isEqualTo(11)
        assertThat(
            cardNumberEditText.calculateCursorPosition(16, 14, 1)
        ).isEqualTo(16)
    }

    @Test
    fun `calculateCursorPosition() when pasting 19 digit number should return expected value`() = testDispatcher.runBlockingTest {
        assertThat(
            cardNumberEditText.calculateCursorPosition(
                newFormattedLength = 23,
                start = 0,
                addedDigits = 19,
                panLength = 19
            )
        ).isEqualTo(23)
    }

    @Test
    fun calculateCursorPosition_whenAmEx_increasesIndexWhenGoingPastTheSpaces() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.AMERICANEXPRESS
        )

        assertThat(
            cardNumberEditText.calculateCursorPosition(6, 4, 1)
        ).isEqualTo(6)
        assertThat(
            cardNumberEditText.calculateCursorPosition(13, 11, 1)
        ).isEqualTo(13)
    }

    @Test
    fun calculateCursorPosition_whenDinersClub16_decreasesIndexWhenDeletingPastTheSpaces() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.DINERSCLUB16
        )

        assertThat(
            cardNumberEditText.calculateCursorPosition(6, 5, 0)
        ).isEqualTo(4)
        assertThat(
            cardNumberEditText.calculateCursorPosition(13, 10, 0)
        ).isEqualTo(9)
        assertThat(
            cardNumberEditText.calculateCursorPosition(17, 15, 0)
        ).isEqualTo(14)
    }

    @Test
    fun calculateCursorPosition_whenDeletingNotOnGaps_doesNotDecreaseIndex() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.DINERSCLUB14
        )

        assertThat(
            cardNumberEditText.calculateCursorPosition(12, 7, 0)
        ).isEqualTo(7)
    }

    @Test
    fun calculateCursorPosition_whenAmEx_decreasesIndexWhenDeletingPastTheSpaces() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.AMERICANEXPRESS
        )

        assertThat(
            cardNumberEditText.calculateCursorPosition(10, 5, 0)
        ).isEqualTo(4)
        assertThat(
            cardNumberEditText.calculateCursorPosition(13, 12, 0)
        ).isEqualTo(11)
    }

    @Test
    fun calculateCursorPosition_whenSelectionInTheMiddle_increasesIndexOverASpace() = testDispatcher.runBlockingTest {
        Dispatchers.setMain(testDispatcher)
        cardNumberEditText.onAccountRangeResult(
            AccountRangeFixtures.VISA
        )

        assertThat(
            cardNumberEditText.calculateCursorPosition(10, 4, 1)
        ).isEqualTo(6)
    }

    @Test
    fun calculateCursorPosition_whenPastingIntoAGap_includesTheGapJump() {
        cardNumberEditText.cardBrand = CardBrand.Unknown

        assertThat(
            cardNumberEditText.calculateCursorPosition(12, 8, 2)
        ).isEqualTo(11)
    }

    @Test
    fun calculateCursorPosition_whenPastingOverAGap_includesTheGapJump() {
        cardNumberEditText.cardBrand = CardBrand.Unknown
        assertThat(
            cardNumberEditText.calculateCursorPosition(12, 3, 5)
        ).isEqualTo(9)
    }

    @Test
    fun calculateCursorPosition_whenIndexWouldGoOutOfBounds_setsToEndOfString() {
        cardNumberEditText.cardBrand = CardBrand.Visa

        // This case could happen when you paste over 5 digits with only 2
        assertThat(
            cardNumberEditText.calculateCursorPosition(3, 3, 2)
        ).isEqualTo(3)
    }

    @Test
    fun setText_whenTextIsValidCommonLengthNumber_changesCardValidState() {
        Dispatchers.setMain(testDispatcher)
        updateCardNumberAndIdle(VISA_WITH_SPACES)

        assertThat(cardNumberEditText.isCardNumberValid)
            .isTrue()
        assertThat(completionCallbackInvocations)
            .isEqualTo(1)
    }

    @Test
    fun setText_whenTextIsSpacelessValidNumber_changesToSpaceNumberAndValidates() {
        Dispatchers.setMain(testDispatcher)
        updateCardNumberAndIdle(VISA_NO_SPACES)

        assertThat(cardNumberEditText.isCardNumberValid)
            .isTrue()
        assertThat(completionCallbackInvocations)
            .isEqualTo(1)
    }

    @Test
    fun `when 15 digit non-UnionPay PAN is pasted, should call completion callback`() {
        val cardNumberEditText = CardNumberEditText(
            context,
            workContext = testDispatcher,
            cardAccountRangeRepository = NullCardAccountRangeRepository(),
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsDataFactory = analyticsDataFactory,
            publishableKeySupplier = publishableKeySupplier
        )

        var callbacks = 0
        cardNumberEditText.completionCallback = {
            callbacks++
        }

        cardNumberEditText.setText(AMEX_NO_SPACES)
        idleLooper()

        assertThat(callbacks)
            .isEqualTo(1)
    }

    @Test
    fun `when 19 digit PAN is pasted, call completion callback`() {
        val cardNumberEditText = CardNumberEditText(
            context,
            workContext = testDispatcher,
            cardAccountRangeRepository = NullCardAccountRangeRepository(),
            staticCardAccountRanges = object : StaticCardAccountRanges {
                override fun match(
                    cardNumber: CardNumber.Unvalidated
                ): AccountRange? = AccountRangeFixtures.UNIONPAY19
            },
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsDataFactory = analyticsDataFactory,
            publishableKeySupplier = publishableKeySupplier
        )

        var callbacks = 0
        cardNumberEditText.completionCallback = {
            callbacks++
        }

        cardNumberEditText.setText("6216828050000000000")
        idleLooper()

        assertThat(callbacks)
            .isEqualTo(1)
    }

    @Test
    fun `when 19 digit PAN is pasted, full PAN is accepted and formatted`() {
        val cardNumberEditText = CardNumberEditText(
            context,
            workContext = testDispatcher,
            cardAccountRangeRepository = NullCardAccountRangeRepository(),
            staticCardAccountRanges = object : StaticCardAccountRanges {
                override fun match(
                    cardNumber: CardNumber.Unvalidated
                ): AccountRange? = null
            },
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsDataFactory = analyticsDataFactory,
            publishableKeySupplier = publishableKeySupplier
        )

        cardNumberEditText.setText("6216828050000000000")
        idleLooper()

        assertThat(cardNumberEditText.fieldText)
            .isEqualTo("6216 8280 5000 0000 000")
    }

    @Test
    fun `updating text with null account range should format text correctly but not set card brand`() {
        val cardNumberEditText = CardNumberEditText(
            context,
            workContext = testDispatcher,
            cardAccountRangeRepository = NullCardAccountRangeRepository(),
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsDataFactory = analyticsDataFactory,
            publishableKeySupplier = publishableKeySupplier
        )

        cardNumberEditText.setText(UNIONPAY_NO_SPACES)
        idleLooper()

        assertThat(cardNumberEditText.fieldText)
            .isEqualTo(UNIONPAY_WITH_SPACES)
        assertThat(cardNumberEditText.cardBrand)
            .isEqualTo(CardBrand.Unknown)
    }

    @Test
    fun `full Amex typed as BIN followed by remaining number should change isCardNumberValid to true and invoke completion callback`() {
        // type Amex BIN
        updateCardNumberAndIdle(AMEX_BIN)
        // type rest of card number
        cardNumberEditText.append(AMEX_NO_SPACES.drop(6))
        idleLooper()

        assertThat(cardNumberEditText.isCardNumberValid)
            .isTrue()
        assertThat(completionCallbackInvocations)
            .isEqualTo(1)
    }

    @Test
    fun `full Amex typed typed at once should change isCardNumberValid to true and invoke completion callback`() {
        Dispatchers.setMain(testDispatcher)
        updateCardNumberAndIdle(AMEX_NO_SPACES)
        idleLooper()

        assertThat(cardNumberEditText.isCardNumberValid)
            .isTrue()
        assertThat(completionCallbackInvocations)
            .isEqualTo(1)
    }

    @Test
    fun setText_whenTextChangesFromValidToInvalid_changesCardValidState() {
        updateCardNumberAndIdle(VISA_WITH_SPACES)
        // Simply setting the value interacts with this mock once -- that is tested elsewhere
        completionCallbackInvocations = 0

        // Removing last character should make this invalid
        cardNumberEditText.setText(
            withoutLastCharacter(cardNumberEditText.text.toString())
        )

        assertThat(cardNumberEditText.isCardNumberValid)
            .isFalse()
        assertThat(completionCallbackInvocations)
            .isEqualTo(0)
    }

    @Test
    fun setText_whenTextIsInvalidCommonLengthNumber_doesNotNotifyListener() {
        // This creates a full-length but not valid number: 4242 4242 4242 4243
        updateCardNumberAndIdle(
            withoutLastCharacter(VISA_WITH_SPACES) + "3"
        )

        assertThat(cardNumberEditText.isCardNumberValid)
            .isFalse()
        assertThat(completionCallbackInvocations)
            .isEqualTo(0)
    }

    @Test
    fun whenNotFinishedTyping_doesNotSetErrorValue() {
        // We definitely shouldn't start out in an error state.
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        updateCardNumberAndIdle("123")
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenValidCard_doesNotSetErrorValue() {
        updateCardNumberAndIdle(withoutLastCharacter(VISA_WITH_SPACES))
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        // We now have the valid 4242 Visa
        cardNumberEditText.append("2")
        idleLooper()

        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenInvalidCard_setsErrorValue() {
        updateCardNumberAndIdle(withoutLastCharacter(UNIONPAY_NO_SPACES))

        // This makes the number officially invalid
        cardNumberEditText.append("3")
        idleLooper()

        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()
    }

    @Test
    fun finishTypingInvalidCardNumber_whenFollowedByDelete_setsErrorBackToFalse() {
        updateCardNumberAndIdle(
            withoutLastCharacter(VISA_WITH_SPACES) + "3"
        )
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingDinersClub14_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        // type DinersClub BIN
        updateCardNumberAndIdle(CardNumberFixtures.DINERS_CLUB_14_BIN)
        // type rest of card number
        cardNumberEditText.append(
            withoutLastCharacter(DINERS_CLUB_14_WITH_SPACES.drop(6)) + "3"
        )
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        idleLooper()
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.append(DINERS_CLUB_14_WITH_SPACES.last().toString())
        idleLooper()

        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingDinersClub16_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        updateCardNumberAndIdle(
            withoutLastCharacter(DINERS_CLUB_16_WITH_SPACES) + "3"
        )
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.append(DINERS_CLUB_16_WITH_SPACES.last().toString())
        idleLooper()

        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun finishTypingAmEx_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        // type Amex BIN
        updateCardNumberAndIdle(AMEX_BIN)
        // type rest of card number
        cardNumberEditText.append(
            withoutLastCharacter(AMEX_NO_SPACES.drop(6)) + "3"
        )
        idleLooper()
        assertThat(cardNumberEditText.shouldShowError)
            .isTrue()

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        idleLooper()
        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()

        cardNumberEditText.append("5")
        idleLooper()

        assertThat(cardNumberEditText.shouldShowError)
            .isFalse()
    }

    @Test
    fun setCardBrandChangeListener_callsSetCardBrand() {
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterVisaBin_callsBrandListener() {
        updateCardNumberAndIdle(VISA_BIN)
        assertEquals(CardBrand.Visa, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun addAmExBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.AmericanExpress, AMEX_BIN)
    }

    @Test
    fun addDinersClubBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.DinersClub, CardNumberFixtures.DINERS_CLUB_14_BIN)
        verifyCardBrandBin(CardBrand.DinersClub, CardNumberFixtures.DINERS_CLUB_16_BIN)
    }

    @Test
    fun addDiscoverBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.Discover, CardNumberFixtures.DISCOVER_BIN)
    }

    @Test
    fun addMasterCardBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.MasterCard, CardNumberFixtures.MASTERCARD_BIN)
    }

    @Test
    fun addJcbBin_callsBrandListener() {
        verifyCardBrandBin(CardBrand.JCB, CardNumberFixtures.JCB_BIN)
    }

    @Test
    fun enterCompleteNumberInParts_onlyCallsBrandListenerOnce() {
        cardNumberEditText.append(AMEX_WITH_SPACES.take(2))
        idleLooper()
        cardNumberEditText.append(AMEX_WITH_SPACES.drop(2))
        idleLooper()
        assertEquals(CardBrand.AmericanExpress, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterBrandBin_thenDelete_callsUpdateWithUnknown() {
        updateCardNumberAndIdle(UNIONPAY_BIN)
        assertEquals(CardBrand.UnionPay, lastBrandChangeCallbackInvocation)

        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        idleLooper()
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun enterBrandBin_thenClearAllText_callsUpdateWithUnknown() {
        updateCardNumberAndIdle(VISA_BIN)
        assertEquals(CardBrand.Visa, lastBrandChangeCallbackInvocation)

        // Just adding some other text. Not enough to invalidate the card or complete it.
        lastBrandChangeCallbackInvocation = null
        cardNumberEditText.append("123")
        idleLooper()

        assertNull(lastBrandChangeCallbackInvocation)

        // This simulates the user selecting all text and deleting it.
        updateCardNumberAndIdle("")

        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun cardNumber_withSpaces_returnsCardNumberWithoutSpaces() {
        updateCardNumberAndIdle(VISA_WITH_SPACES)
        assertThat(cardNumberEditText.validatedCardNumber?.value)
            .isEqualTo(VISA_NO_SPACES)

        updateCardNumberAndIdle("")
        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        assertThat(cardNumberEditText.validatedCardNumber?.value)
            .isEqualTo(AMEX_NO_SPACES)

        updateCardNumberAndIdle("")
        updateCardNumberAndIdle(DINERS_CLUB_14_WITH_SPACES)
        assertThat(cardNumberEditText.validatedCardNumber?.value)
            .isEqualTo(DINERS_CLUB_14_NO_SPACES)

        updateCardNumberAndIdle("")
        updateCardNumberAndIdle(DINERS_CLUB_16_WITH_SPACES)
        assertThat(cardNumberEditText.validatedCardNumber?.value)
            .isEqualTo(DINERS_CLUB_16_NO_SPACES)
    }

    @Test
    fun getCardNumber_whenIncompleteCard_returnsNull() {
        updateCardNumberAndIdle(
            DINERS_CLUB_14_WITH_SPACES.take(DINERS_CLUB_14_WITH_SPACES.length - 2)
        )
        assertThat(cardNumberEditText.validatedCardNumber)
            .isNull()
    }

    @Test
    fun getCardNumber_whenInvalidCardNumber_returnsNull() {
        updateCardNumberAndIdle(
            withoutLastCharacter(VISA_WITH_SPACES) + "3" // creates the 4242 4242 4242 4243 bad number
        )
        assertThat(cardNumberEditText.validatedCardNumber)
            .isNull()
    }

    @Test
    fun getCardNumber_whenValidNumberIsChangedToInvalid_returnsNull() {
        updateCardNumberAndIdle(AMEX_WITH_SPACES)
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)

        assertThat(cardNumberEditText.validatedCardNumber)
            .isNull()
    }

    @Test
    fun `pasting a full number, fully deleting it via delete key, then pasting a new full number should format the new number`() {
        // paste a full number
        updateCardNumberAndIdle(VISA_NO_SPACES)

        // fully delete it with delete key
        repeat(cardNumberEditText.fieldText.length) {
            ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        }
        assertThat(cardNumberEditText.fieldText)
            .isEmpty()

        // paste a new number
        updateCardNumberAndIdle(JCB_NO_SPACES)

        assertThat(cardNumberEditText.fieldText)
            .isEqualTo(JCB_WITH_SPACES)
    }

    @Test
    fun `queryAccountRangeRepository() should update cardBrand value`() {
        cardNumberEditText.queryAccountRangeRepository(CardNumberFixtures.DINERS_CLUB_14)
        idleLooper()
        assertEquals(CardBrand.DinersClub, lastBrandChangeCallbackInvocation)

        cardNumberEditText.queryAccountRangeRepository(CardNumberFixtures.AMEX)
        idleLooper()
        assertEquals(CardBrand.AmericanExpress, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun `queryAccountRangeRepository() with null bin should set cardBrand to Unknown`() {
        cardNumberEditText.queryAccountRangeRepository(CardNumber.Unvalidated(""))
        assertEquals(CardBrand.Unknown, lastBrandChangeCallbackInvocation)
    }

    @Test
    fun `onDetachedFromWindow() should cancel accountRangeRepositoryJob`() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        activityScenarioFactory.createAddPaymentMethodActivity()
            .use { activityScenario ->
                activityScenario.onActivity { activity ->
                    val cardNumberEditText = CardNumberEditText(
                        activity,
                        workContext = testDispatcher,
                        cardAccountRangeRepository = DelayedCardAccountRangeRepository(),
                        analyticsRequestExecutor = analyticsRequestExecutor,
                        analyticsRequestFactory = analyticsRequestFactory,
                        analyticsDataFactory = analyticsDataFactory,
                        publishableKeySupplier = publishableKeySupplier
                    )

                    val root = activity.findViewById<ViewGroup>(R.id.add_payment_method_card).also {
                        it.removeAllViews()
                        it.addView(cardNumberEditText)
                    }

                    cardNumberEditText.setText(UNIONPAY_NO_SPACES)
                    assertThat(cardNumberEditText.accountRangeRepositoryJob)
                        .isNotNull()

                    root.removeView(cardNumberEditText)
                    assertThat(cardNumberEditText.accountRangeRepositoryJob)
                        .isNull()
                }
            }
    }

    @Test
    fun `getAccountRange() should not be called with VISA BIN`() {
        var repositoryCalls = 0
        val cardNumberEditText = CardNumberEditText(
            context,
            workContext = testDispatcher,
            cardAccountRangeRepository = object : CardAccountRangeRepository {
                override suspend fun getAccountRange(
                    cardNumber: CardNumber.Unvalidated
                ): AccountRange? {
                    repositoryCalls++
                    return cardAccountRangeRepository.getAccountRange(cardNumber)
                }

                override val loading: Flow<Boolean> = flowOf(false)
            },
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsDataFactory = analyticsDataFactory,
            publishableKeySupplier = publishableKeySupplier
        )

        cardNumberEditText.setText(VISA_BIN)
        idleLooper()
        assertThat(repositoryCalls)
            .isEqualTo(0)
    }

    @Test
    fun `getAccountRange() should only be called when necessary`() {
        Dispatchers.setMain(testDispatcher)

        var repositoryCalls = 0
        val cardNumberEditText = CardNumberEditText(
            context,
            workContext = testDispatcher,
            cardAccountRangeRepository = object : CardAccountRangeRepository {
                override suspend fun getAccountRange(
                    cardNumber: CardNumber.Unvalidated
                ): AccountRange? {
                    repositoryCalls++
                    return cardAccountRangeRepository.getAccountRange(cardNumber)
                }

                override val loading: Flow<Boolean> = flowOf(false)
            },
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsDataFactory = analyticsDataFactory,
            publishableKeySupplier = publishableKeySupplier
        )

        // 620000 - valid BIN, call repo
        cardNumberEditText.setText(UNIONPAY_BIN)
        idleLooper()
        assertThat(repositoryCalls)
            .isEqualTo(1)

        // 6200000 - valid BIN but matches existing accountRange
        cardNumberEditText.append("0")
        idleLooper()
        assertThat(repositoryCalls)
            .isEqualTo(1)

        // 620000 - valid BIN but matches existing accountRange
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        idleLooper()
        assertThat(repositoryCalls)
            .isEqualTo(1)

        // 62000 - not a BIN
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        idleLooper()
        assertThat(repositoryCalls)
            .isEqualTo(1)

        // 6200000 - transitioned to valid BIN, call repo
        cardNumberEditText.append("0")
        idleLooper()
        assertThat(repositoryCalls)
            .isEqualTo(2)

        // clear digits
        cardNumberEditText.setText("")
        idleLooper()

        // 621368 - new valid UnionPay BIN, call repo
        cardNumberEditText.setText("621368")
        idleLooper()
        assertThat(repositoryCalls)
            .isEqualTo(3)
    }

    @Test
    fun `inputting a full PAN before card service returns result should fire card_metadata_loaded_too_slow analytics event`() {
        val analyticsRequests = mutableListOf<AnalyticsRequest>()
        val cardNumberEditText = CardNumberEditText(
            context,
            workContext = testDispatcher,
            cardAccountRangeRepository = object : CardAccountRangeRepository {
                override suspend fun getAccountRange(
                    cardNumber: CardNumber.Unvalidated
                ): AccountRange? = null

                override val loading: Flow<Boolean> = flowOf(false)
            },
            analyticsRequestExecutor = {
                analyticsRequests.add(it)
            },
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsDataFactory = analyticsDataFactory,
            publishableKeySupplier = publishableKeySupplier
        )
        cardNumberEditText.setText(UNIONPAY_NO_SPACES)
        idleLooper()
        assertThat(analyticsRequests)
            .hasSize(1)
        assertThat(analyticsRequests.first().params["event"])
            .isEqualTo("stripe_android.card_metadata_loaded_too_slow")
    }

    @Test
    fun `onCardMetadataLoadedTooSlow() when publishable key is unavailable uses undefined publishable key`() {
        val analyticsRequests = mutableListOf<AnalyticsRequest>()

        CardNumberEditText(
            context,
            workContext = testDispatcher,
            cardAccountRangeRepository = cardAccountRangeRepository,
            analyticsRequestExecutor = {
                analyticsRequests.add(it)
            },
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsDataFactory = analyticsDataFactory,
            publishableKeySupplier = {
                throw RuntimeException()
            }
        ).onCardMetadataLoadedTooSlow()

        assertThat(analyticsRequests.first().options.apiKey)
            .isEqualTo(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY)
    }

    private fun verifyCardBrandBin(
        cardBrand: CardBrand,
        bin: String
    ) {
        // Reset inside the loop so we don't count each prefix
        lastBrandChangeCallbackInvocation = null
        updateCardNumberAndIdle(bin)
        assertEquals(cardBrand, lastBrandChangeCallbackInvocation)
        updateCardNumberAndIdle("")
    }

    private fun updateCardNumberAndIdle(cardNumber: String) {
        cardNumberEditText.setText(cardNumber)
        idleLooper()
    }

    private class DelayedCardAccountRangeRepository : CardAccountRangeRepository {
        override suspend fun getAccountRange(
            cardNumber: CardNumber.Unvalidated
        ): AccountRange? {
            delay(TimeUnit.SECONDS.toMillis(10))
            return null
        }

        override val loading: Flow<Boolean> = flowOf(false)
    }

    private companion object {
        private fun withoutLastCharacter(s: String) = s.take(s.length - 1)
    }
}
