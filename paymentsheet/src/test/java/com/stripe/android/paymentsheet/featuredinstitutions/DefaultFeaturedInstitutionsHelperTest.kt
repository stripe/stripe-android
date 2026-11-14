package com.stripe.android.paymentsheet.featuredinstitutions

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultFeaturedInstitutionsHelperTest {

    @Test
    fun `bank row metadata contains the instant debits payment method`() {
        assertThat(BANK_ROW_METADATA.supportedPaymentMethodTypes()).contains("link")
        assertThat(NO_BANK_ROW_METADATA.supportedPaymentMethodTypes()).contains("card")
        assertThat(NO_BANK_ROW_METADATA.supportedPaymentMethodTypes()).doesNotContain("link")
    }

    @Test
    fun `fetches with the merchant publishable key when there's a bank row`() = runScenario {
        helper.fetchAsync(elementsSession(), BANK_ROW_METADATA, DEFAULT_API_CONFIG)

        assertThat(repository.retrieveCalls.awaitItem()).isEqualTo(
            ApiRequest.Options(
                apiKey = DEFAULT_API_CONFIG.publishableKey,
                stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
            )
        )
        assertThat(helper.featuredInstitutions).isNotNull()
        assertThat(eventReporter.featuredInstitutionsInsufficientCalls.awaitItem()).isEqualTo(0)
    }

    @Test
    fun `fetchAsync returns without waiting for the request`() {
        val responseGate = CompletableDeferred<Unit>()
        runScenario(
            institutions = institutions(4),
            responseGate = responseGate,
        ) {
            helper.fetchAsync(elementsSession(), BANK_ROW_METADATA, DEFAULT_API_CONFIG)

            // The request is in flight, but fetchAsync has already returned and nothing is shown yet.
            repository.retrieveCalls.awaitItem()
            val featuredInstitutions = requireNotNull(helper.featuredInstitutions)
            assertThat(featuredInstitutions.icons.value).isEmpty()

            responseGate.complete(Unit)

            assertThat(helper.iconIds()).containsExactly("bank_0", "bank_1", "bank_2", "bank_3").inOrder()
        }
    }

    @Test
    fun `does not fetch without a bank row`() = runScenario {
        helper.fetchAsync(elementsSession(), NO_BANK_ROW_METADATA, DEFAULT_API_CONFIG)

        repository.retrieveCalls.expectNoEvents()
        assertThat(helper.featuredInstitutions).isNull()
    }

    @Test
    fun `does not fetch when the killswitch is on`() = runScenario {
        helper.fetchAsync(elementsSession(killswitch = true), BANK_ROW_METADATA, DEFAULT_API_CONFIG)

        repository.retrieveCalls.expectNoEvents()
        assertThat(helper.featuredInstitutions).isNull()
    }

    @Test
    fun `fetches when the killswitch is off`() = runScenario {
        helper.fetchAsync(elementsSession(killswitch = false), BANK_ROW_METADATA, DEFAULT_API_CONFIG)

        repository.retrieveCalls.awaitItem()
        assertThat(helper.featuredInstitutions).isNotNull()
        eventReporter.featuredInstitutionsInsufficientCalls.awaitItem()
    }

    @Test
    fun `drops institutions without a valid icon url`() = runScenario(
        institutions = institutions(5) + listOf(
            FeaturedInstitution(id = "no_icon", name = "No icon", iconUrl = null),
            FeaturedInstitution(id = "empty_icon", name = "Empty icon", iconUrl = ""),
            FeaturedInstitution(id = "invalid_icon", name = "Invalid icon", iconUrl = "not a url"),
        ),
    ) {
        helper.fetchAsync(elementsSession(), BANK_ROW_METADATA, DEFAULT_API_CONFIG)
        repository.retrieveCalls.awaitItem()

        assertThat(helper.iconIds()).containsExactly("bank_0", "bank_1", "bank_2", "bank_3", "bank_4").inOrder()
    }

    @Test
    fun `drops icons that fail to download and keeps the rest in order`() = runScenario(
        institutions = institutions(5),
        failingUrls = setOf(iconUrl(1)),
    ) {
        helper.fetchAsync(elementsSession(), BANK_ROW_METADATA, DEFAULT_API_CONFIG)
        repository.retrieveCalls.awaitItem()

        assertThat(helper.iconIds()).containsExactly("bank_0", "bank_2", "bank_3", "bank_4").inOrder()
        assertThat(helper.featuredInstitutions?.publishedInstitutions?.map { it.id })
            .containsExactly("bank_0", "bank_2", "bank_3", "bank_4").inOrder()
    }

    @Test
    fun `publishes the first 4 icons before slower downloads finish`() {
        val slowDownload = CompletableDeferred<Result<Bitmap?>>()
        runScenario(
            institutions = institutions(5),
            pendingDownloads = mapOf(iconUrl(4) to slowDownload),
        ) {
            helper.fetchAsync(elementsSession(), BANK_ROW_METADATA, DEFAULT_API_CONFIG)
            repository.retrieveCalls.awaitItem()

            requireNotNull(helper.featuredInstitutions).icons.test {
                assertThat(awaitItem().map { it.id }).containsExactly("bank_0", "bank_1", "bank_2", "bank_3").inOrder()

                slowDownload.complete(Result.success(bitmap()))

                assertThat(awaitItem().map { it.id })
                    .containsExactly("bank_0", "bank_1", "bank_2", "bank_3", "bank_4").inOrder()
            }
        }
    }

    @Test
    fun `publishes nothing and reports insufficient icons with fewer than 4 icons`() = runScenario(
        institutions = institutions(3),
    ) {
        helper.fetchAsync(elementsSession(), BANK_ROW_METADATA, DEFAULT_API_CONFIG)
        repository.retrieveCalls.awaitItem()

        assertThat(eventReporter.featuredInstitutionsInsufficientCalls.awaitItem()).isEqualTo(3)
        assertThat(helper.iconIds()).isEmpty()
    }

    @Test
    fun `publishes nothing and reports load failed when the request fails`() = runScenario {
        val error = IllegalStateException("500")
        repository.result = Result.failure(error)

        helper.fetchAsync(elementsSession(), BANK_ROW_METADATA, DEFAULT_API_CONFIG)
        repository.retrieveCalls.awaitItem()

        assertThat(eventReporter.featuredInstitutionsLoadFailedCalls.awaitItem()).isEqualTo(error)
        assertThat(helper.iconIds()).isEmpty()
    }

    @Test
    fun `reports shown only once per load`() = runScenario(institutions = institutions(4)) {
        helper.fetchAsync(elementsSession(), BANK_ROW_METADATA, DEFAULT_API_CONFIG)
        repository.retrieveCalls.awaitItem()

        val featuredInstitutions = requireNotNull(helper.featuredInstitutions)
        featuredInstitutions.reportShown()
        featuredInstitutions.reportShown()

        eventReporter.featuredInstitutionsShownCalls.awaitItem()
    }

    @Test
    fun `preloaded with fewer than 4 icons has no icons`() {
        val featuredInstitutions = FeaturedInstitutions.preloaded(icons(3))

        assertThat(featuredInstitutions.icons.value).isEmpty()
    }

    @Test
    fun `preloaded with 4 icons has 4 icons`() {
        val featuredInstitutions = FeaturedInstitutions.preloaded(icons(4))

        assertThat(featuredInstitutions.icons.value).hasSize(4)
    }

    @Test
    fun `prefetched helper downloads the given institutions without fetching`() = runTest {
        val eventReporter = FakeEventReporter()
        val helper = PrefetchedFeaturedInstitutionsHelper(
            institutions = institutions(4),
            imageLoader = FakeIconLoader(failingUrls = emptySet(), pendingDownloads = emptyMap()),
            eventReporter = eventReporter,
            viewModelScope = backgroundScope,
            workContext = UnconfinedTestDispatcher(testScheduler),
        )

        assertThat(helper.iconIds()).containsExactly("bank_0", "bank_1", "bank_2", "bank_3").inOrder()
        eventReporter.validate()
    }

    @Test
    fun `prefetched helper without institutions has no featured institutions`() = runTest {
        val helper = PrefetchedFeaturedInstitutionsHelper(
            institutions = null,
            imageLoader = FakeIconLoader(failingUrls = emptySet(), pendingDownloads = emptyMap()),
            eventReporter = FakeEventReporter(),
            viewModelScope = backgroundScope,
            workContext = UnconfinedTestDispatcher(testScheduler),
        )

        assertThat(helper.featuredInstitutions).isNull()
    }

    private fun FeaturedInstitutionsHelper.iconIds(): List<String> {
        return featuredInstitutions?.icons?.value.orEmpty().map { it.id }
    }

    private fun runScenario(
        institutions: List<FeaturedInstitution> = emptyList(),
        failingUrls: Set<String> = emptySet(),
        pendingDownloads: Map<String, CompletableDeferred<Result<Bitmap?>>> = emptyMap(),
        responseGate: CompletableDeferred<Unit>? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val repository = FakeFeaturedInstitutionsRepository(
            result = Result.success(FeaturedInstitutionList(institutions)),
            responseGate = responseGate,
        )
        val eventReporter = FakeEventReporter()
        val helper = DefaultFeaturedInstitutionsHelper(
            featuredInstitutionsRepository = repository,
            imageLoader = FakeIconLoader(failingUrls, pendingDownloads),
            eventReporter = eventReporter,
            viewModelScope = backgroundScope,
            workContext = UnconfinedTestDispatcher(testScheduler),
        )

        Scenario(
            helper = helper,
            repository = repository,
            eventReporter = eventReporter,
        ).block()

        repository.ensureAllEventsConsumed()
        eventReporter.validate()
    }

    private class Scenario(
        val helper: DefaultFeaturedInstitutionsHelper,
        val repository: FakeFeaturedInstitutionsRepository,
        val eventReporter: FakeEventReporter,
    )

    /**
     * Succeeds immediately for every URL, except [failingUrls] which fail and [pendingDownloads] which complete
     * when their deferred does.
     */
    private class FakeIconLoader(
        private val failingUrls: Set<String>,
        private val pendingDownloads: Map<String, CompletableDeferred<Result<Bitmap?>>>,
    ) : StripeImageLoader {
        override suspend fun load(url: String, width: Int, height: Int): Result<Bitmap?> = load(url)

        override suspend fun load(url: String): Result<Bitmap?> {
            pendingDownloads[url]?.let { return it.await() }
            return if (url in failingUrls) {
                Result.failure(IllegalStateException("Failed to load $url"))
            } else {
                Result.success(bitmap())
            }
        }

        override suspend fun get(url: String): Result<Bitmap?> = Result.success(null)
    }

    private companion object {
        val BANK_ROW_METADATA: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("card", "link"),
                linkFundingSources = listOf("card", "bank_account"),
            ),
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION_WITH_INSTANT_DEBITS_ONBOARDING,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
            isInstantDebitsOnboardingEnabled = true,
        )

        val NO_BANK_ROW_METADATA: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("card", "us_bank_account"),
            ),
        )

        fun iconUrl(index: Int) = "https://b.stripecdn.com/bank_$index.png"

        fun institutions(count: Int): List<FeaturedInstitution> {
            return (0 until count).map { index ->
                FeaturedInstitution(id = "bank_$index", name = "Bank $index", iconUrl = iconUrl(index))
            }
        }

        fun icons(count: Int): List<FeaturedInstitutionIcon> {
            return (0 until count).map { index ->
                FeaturedInstitutionIcon(id = "bank_$index", bitmap = bitmap().asImageBitmap())
            }
        }

        fun bitmap(): Bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)

        fun elementsSession(killswitch: Boolean? = null): ElementsSession {
            return ElementsSession(
                linkSettings = null,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                merchantCountry = null,
                isGooglePayEnabled = true,
                sessionsError = null,
                externalPaymentMethodData = null,
                customer = null,
                cardBrandChoice = null,
                customPaymentMethods = emptyList(),
                elementsSessionId = "elements_session_123",
                flags = buildMap {
                    killswitch?.let { put(ElementsSession.Flag.ELEMENTS_MOBILE_FEATURED_INSTITUTIONS_KILLSWITCH, it) }
                },
                orderedPaymentMethodTypesAndWallets = emptyList(),
                experimentsData = null,
                passiveCaptcha = null,
                merchantLogoUrl = null,
                elementsSessionConfigId = null,
                accountId = "acct_test",
                merchantId = "acct_test",
            )
        }
    }
}
