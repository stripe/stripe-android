package com.stripe.android.checkout

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.checkout.Checkout.Companion.configure
import com.stripe.android.checkout.Checkout.Companion.createWithState
import com.stripe.android.checkout.injection.CheckoutComponent
import com.stripe.android.checkout.injection.DaggerCheckoutComponent
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorToggle
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.parcelize.Parcelize
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private val SERVER_UPDATE_TIMEOUT_MS = 20.seconds.inWholeMilliseconds

/**
 * Manages a Checkout Session, providing methods to observe and mutate its state.
 *
 * Create a new instance with [configure], or restore a previously saved instance with [createWithState].
 * Observe session updates via [checkoutSession] and loading state via [isLoading].
 *
 * Mutation methods are queued and run in sequence. They return [Result.failure] if a
 * payment flow is currently presented.
 */
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions")
class Checkout private constructor(
    internalState: InternalState,
    private val component: CheckoutComponent,
) {
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Creates and initializes a new [Checkout] by fetching the session from the server.
         *
         * @param checkoutSessionClientSecret The client secret for the checkout session.
         * @param configuration Optional configuration options.
         */
        suspend fun configure(
            context: Context,
            checkoutSessionClientSecret: String,
            configuration: Configuration = Configuration(),
        ): Result<Checkout> {
            val application = context.applicationContext as Application
            val component = DaggerCheckoutComponent.factory().create(application)
            val configurationState = configuration.build()
            return component.checkoutSessionRepository.init(
                sessionId = checkoutSessionClientSecret.substringBefore("_secret_"),
                adaptivePricingAllowed = configurationState.adaptivePricingAllowed,
            ).map { response ->
                val flagImages = prefetchFlagImages(context, response, component)
                Checkout(
                    internalState = InternalState(
                        key = UUID.randomUUID().toString(),
                        configuration = configurationState,
                        checkoutSessionResponse = response,
                        flagImages = flagImages,
                    ),
                    component = component,
                )
            }
        }

        private suspend fun prefetchFlagImages(
            context: Context,
            response: CheckoutSessionResponse,
            component: CheckoutComponent,
        ): Map<String, Bitmap>? {
            val adaptivePricingInfo = response.adaptivePricingInfo ?: return null
            val localOption = adaptivePricingInfo.localCurrencyOptions.firstOrNull() ?: return null
            val flagImageRepository = FlagImageRepository(
                imageLoader = DefaultStripeImageLoader(context),
                displayDensity = context.resources.displayMetrics.density,
            )
            val result = flagImageRepository.fetch(
                integrationCurrencyCode = adaptivePricingInfo.integrationCurrency,
                localCurrencyCode = localOption.currency,
            )
            for (failure in result.failures) {
                val event = PaymentSheetEvent.AdaptivePricingFlagImageLoadFailed(
                    countryCode = failure.countryCode,
                    url = failure.url,
                )
                component.analyticsRequestExecutor.executeAsync(
                    component.paymentAnalyticsRequestFactory.createRequest(
                        event = event,
                        additionalParams = event.params,
                    )
                )
            }
            return result.images
        }

        /**
         * Recreates a [Checkout] from a previously saved [State], such as after process death.
         */
        fun createWithState(
            context: Context,
            state: State,
        ): Checkout {
            val application = context.applicationContext as Application
            val component = DaggerCheckoutComponent.factory().create(application)
            return Checkout(
                internalState = state.internalState,
                component = component,
            )
        }
    }

    /**
     * A serializable snapshot of a [Checkout] instance's state.
     * Save via the [state] property and restore with [createWithState].
     */
    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class State internal constructor(
        internal val internalState: InternalState,
    ) : Parcelable

    /**
     * Builder for configuring a [Checkout] instance.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var adaptivePricingAllowed: Boolean = false

        /**
         * Whether to allow adaptive pricing, which displays amounts in the customer's local currency.
         */
        fun adaptivePricingAllowed(adaptivePricingAllowed: Boolean) = apply {
            this.adaptivePricingAllowed = adaptivePricingAllowed
        }

        @Parcelize
        internal data class State(
            val adaptivePricingAllowed: Boolean,
        ) : Parcelable

        internal fun build() = State(
            adaptivePricingAllowed = adaptivePricingAllowed,
        )
    }

    /**
     * Appearance configuration for [CurrencySelectorContent].
     *
     * Controls dimensions, colors, typography, and content display of the currency selector toggle.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class CurrencySelectorContentAppearance {

        /**
         * Controls what content is displayed in each currency option's label.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class LabelContent {
            /**
             * Automatically determines the best display based on the purchase type.
             */
            AUTOMATIC,

            /**
             * Displays only the currency code (e.g., "USD").
             */
            CURRENCY_CODE,

            /**
             * Displays the formatted amount (e.g., "$12.00").
             */
            AMOUNT,
        }

        private var contentVerticalPaddingDp: Float = DEFAULT_VERTICAL_PADDING_DP
        private var cornerRadiusDp: Float? = null
        private var borderWidthDp: Float? = null
        private var borderColor: Color? = null
        private var background: Color? = null
        private var selectedBackground: Color? = null
        private var textColor: Color? = null
        private var selectedTextColor: Color? = null
        private var textSecondaryColor: Color? = null
        private var dangerColor: Color? = null

        @FontRes
        private var fontResId: Int? = null
        private var sizeScaleFactor: Float = DEFAULT_SIZE_SCALE_FACTOR
        private var labelContent: LabelContent = LabelContent.AUTOMATIC

        /**
         * Vertical padding inside each currency option in dp. Default is 4.
         */
        fun contentVerticalPaddingDp(contentVerticalPaddingDp: Float): CurrencySelectorContentAppearance = apply {
            require(contentVerticalPaddingDp.isFinite() && contentVerticalPaddingDp >= 0f) {
                "contentVerticalPaddingDp must be finite and non-negative"
            }
            this.contentVerticalPaddingDp = contentVerticalPaddingDp
        }

        /**
         * Corner radius applied to the track and the selected currency pill in dp. When not set,
         * uses a capsule shape.
         */
        fun cornerRadiusDp(cornerRadiusDp: Float): CurrencySelectorContentAppearance = apply {
            require(cornerRadiusDp.isFinite() && cornerRadiusDp >= 0f) {
                "cornerRadiusDp must be finite and non-negative"
            }
            this.cornerRadiusDp = cornerRadiusDp
        }

        /**
         * Border width for the track outline in dp. When not set, uses the theme's border width.
         */
        fun borderWidthDp(borderWidthDp: Float): CurrencySelectorContentAppearance = apply {
            require(borderWidthDp.isFinite() && borderWidthDp >= 0f) {
                "borderWidthDp must be finite and non-negative"
            }
            this.borderWidthDp = borderWidthDp
        }

        /**
         * Border color for the track. When not set, uses the theme's component border color.
         */
        fun borderColor(borderColor: Color): CurrencySelectorContentAppearance = apply {
            this.borderColor = borderColor
        }

        /**
         * Background color of the unselected area. When not set, uses the theme's component color.
         */
        fun background(background: Color): CurrencySelectorContentAppearance = apply {
            this.background = background
        }

        /**
         * Background color of the selected currency pill. When not set, uses the theme's primary color.
         */
        fun selectedBackground(selectedBackground: Color): CurrencySelectorContentAppearance = apply {
            this.selectedBackground = selectedBackground
        }

        /**
         * Text color for unselected currency options. When not set, uses the theme's onComponent color.
         */
        fun textColor(textColor: Color): CurrencySelectorContentAppearance = apply {
            this.textColor = textColor
        }

        /**
         * Text color for the currently selected currency option. When not set, uses the theme's
         * onPrimary color.
         */
        fun selectedTextColor(selectedTextColor: Color): CurrencySelectorContentAppearance = apply {
            this.selectedTextColor = selectedTextColor
        }

        /**
         * Text color for the exchange rate caption. Alpha values below 0.5 are clamped to 0.5 to
         * maintain regulatory text legibility. When not set, uses the theme's subtitle color.
         */
        fun textSecondaryColor(textSecondaryColor: Color): CurrencySelectorContentAppearance = apply {
            this.textSecondaryColor = if (textSecondaryColor.alpha < MIN_SECONDARY_ALPHA) {
                textSecondaryColor.copy(alpha = MIN_SECONDARY_ALPHA)
            } else {
                textSecondaryColor
            }
        }

        /**
         * Color for error messages shown below the selector. When not set, uses the theme's
         * error color.
         */
        fun dangerColor(dangerColor: Color): CurrencySelectorContentAppearance = apply {
            this.dangerColor = dangerColor
        }

        /**
         * The font resource used for text in the selector. When not set, uses the theme's default
         * font.
         */
        fun fontResId(@FontRes fontResId: Int?): CurrencySelectorContentAppearance = apply {
            this.fontResId = fontResId
        }

        /**
         * Multiplier applied to all font sizes. For example, 1.2 makes text 20% larger.
         * Must be greater than 0. Default is 1.0.
         */
        fun sizeScaleFactor(sizeScaleFactor: Float): CurrencySelectorContentAppearance = apply {
            require(sizeScaleFactor.isFinite() && sizeScaleFactor > 0f) {
                "sizeScaleFactor must be finite and greater than zero"
            }
            this.sizeScaleFactor = sizeScaleFactor
        }

        /**
         * Controls what is displayed in each currency option's label. Default is [LabelContent.AUTOMATIC].
         */
        fun labelContent(labelContent: LabelContent): CurrencySelectorContentAppearance = apply {
            this.labelContent = labelContent
        }

        @Parcelize
        internal data class State(
            val contentVerticalPaddingDp: Float,
            val cornerRadiusDp: Float?,
            val borderWidthDp: Float?,
            @ColorInt val borderColor: Int?,
            @ColorInt val background: Int?,
            @ColorInt val selectedBackground: Int?,
            @ColorInt val textColor: Int?,
            @ColorInt val selectedTextColor: Int?,
            @ColorInt val textSecondaryColor: Int?,
            @ColorInt val dangerColor: Int?,
            @FontRes val fontResId: Int?,
            val sizeScaleFactor: Float,
            val labelContent: LabelContent,
        ) : Parcelable

        internal fun build(): State = State(
            contentVerticalPaddingDp = contentVerticalPaddingDp,
            cornerRadiusDp = cornerRadiusDp,
            borderWidthDp = borderWidthDp,
            borderColor = borderColor?.toArgb(),
            background = background?.toArgb(),
            selectedBackground = selectedBackground?.toArgb(),
            textColor = textColor?.toArgb(),
            selectedTextColor = selectedTextColor?.toArgb(),
            textSecondaryColor = textSecondaryColor?.toArgb(),
            dangerColor = dangerColor?.toArgb(),
            fontResId = fontResId,
            sizeScaleFactor = sizeScaleFactor,
            labelContent = labelContent,
        )

        internal companion object {
            const val DEFAULT_VERTICAL_PADDING_DP = 4f
            const val DEFAULT_SIZE_SCALE_FACTOR = 1.0f
            const val MIN_SECONDARY_ALPHA = 0.5f
        }
    }

    @Volatile
    internal var internalState: InternalState = internalState
        private set

    /**
     * A serializable snapshot of this instance's current state. Can be saved and later passed to
     * [createWithState] to restore.
     *
     * @throws IllegalStateException if a mutation is in flight while trying to set the state.
     */
    var state: State
        get() = State(internalState)
        set(value) {
            ensureNoMutationInFlight()
            internalState = value.internalState
            _checkoutSession.value = internalState.asCheckoutSession()
        }

    private val mutex = Mutex()

    private val _checkoutSession = MutableStateFlow(
        internalState.asCheckoutSession()
    )

    /**
     * The current [CheckoutSession], updated after each successful mutation.
     */
    val checkoutSession: StateFlow<CheckoutSession> = _checkoutSession.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /**
     * Whether a mutation is currently in progress.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        CheckoutInstances.add(internalState.key, this)
    }

    /**
     * Applies a promotion code to the checkout session.
     *
     * @param promotionCode The promotion code to apply. Leading/trailing whitespace is trimmed.
     */
    suspend fun applyPromotionCode(
        promotionCode: String,
    ): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.applyPromotionCode(sessionId, promotionCode.trim())
    }

    /**
     * Updates the quantity of a line item.
     *
     * @param lineItemId The ID of the line item to update.
     * @param quantity The new quantity.
     */
    suspend fun updateLineItemQuantity(
        lineItemId: String,
        quantity: Int,
    ): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.updateLineItemQuantity(sessionId, lineItemId, quantity)
    }

    /**
     * Removes the currently applied promotion code from the checkout session.
     */
    suspend fun removePromotionCode(): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.applyPromotionCode(sessionId, "")
    }

    /**
     * Runs an async function that calls your server to update the Checkout Session,
     * then automatically refreshes [checkoutSession] with the latest session data.
     *
     * A 20-second timeout is enforced. If [serverUpdate] doesn't complete within 20 seconds,
     * this method returns a [Result.failure] with a timeout exception.
     *
     * @param serverUpdate A suspend function that makes a request to your server to update
     * the Checkout Session.
     */
    suspend fun runServerUpdate(
        serverUpdate: suspend () -> Result<Unit>,
    ): Result<Unit> = withInternalState { sessionId ->
        withTimeout(SERVER_UPDATE_TIMEOUT_MS) { serverUpdate() }.fold(
            onSuccess = {
                component.checkoutSessionRepository.init(
                    sessionId = sessionId,
                    adaptivePricingAllowed = configuration.adaptivePricingAllowed,
                )
            },
            onFailure = { Result.failure(it) },
        )
    }

    /**
     * Selects a shipping option.
     *
     * @param id The ID of the shipping option to select.
     */
    suspend fun selectShippingOption(
        id: String,
    ): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.selectShippingRate(sessionId, id)
    }

    /**
     * Updates the shipping address and recalculates taxes.
     *
     * @param name The recipient's name.
     * @param phoneNumber The recipient's phone number.
     * @param address The shipping address.
     */
    suspend fun updateShippingAddress(
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<Unit> = updateAddress("shipping", address) {
        copy(shippingName = name, shippingPhoneNumber = phoneNumber, shippingAddress = it)
    }

    /**
     * Updates the customer's tax ID.
     *
     * @param type The type of tax ID (e.g. "eu_vat"). Leading/trailing whitespace is trimmed.
     * @param value The tax ID value. Leading/trailing whitespace is trimmed.
     */
    suspend fun updateTaxId(
        type: String,
        value: String,
    ): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.updateTaxId(sessionId, type.trim(), value.trim())
    }

    /**
     * Updates the billing address and recalculates taxes.
     *
     * @param name The billing name.
     * @param phoneNumber The billing phone number.
     * @param address The billing address.
     */
    suspend fun updateBillingAddress(
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<Unit> = updateAddress("billing", address) {
        copy(billingName = name, billingPhoneNumber = phoneNumber, billingAddress = it)
    }

    private suspend fun updateAddress(
        addressType: String,
        address: Address,
        mutation: InternalState.(Address.State) -> InternalState,
    ): Result<Unit> {
        val built = address.build()
        val response = internalState.checkoutSessionResponse
        val shouldSendTaxRegion =
            response.automaticTaxEnabled && response.automaticTaxAddressSource == addressType
        return withInternalState(
            additionalStateMutations = { mutation(built) },
        ) { sessionId ->
            if (shouldSendTaxRegion) {
                component.checkoutSessionRepository.updateTaxRegion(sessionId, built)
            } else {
                Result.success(checkoutSessionResponse)
            }
        }
    }

    internal suspend fun updateCurrency(currency: String): Result<Unit> {
        val result = withInternalState { sessionId ->
            component.checkoutSessionRepository.updateCurrency(sessionId, currency)
        }
        result.onSuccess {
            fireEvent(PaymentSheetEvent.AdaptivePricingCurrencyToggled())
        }.onFailure {
            fireEvent(PaymentSheetEvent.AdaptivePricingCurrencyToggledFailed(error = it.safeAnalyticsMessage))
        }
        return result
    }

    internal fun markIntegrationLaunched() {
        internalState = internalState.copy(integrationLaunched = true)
    }

    internal fun markIntegrationDismissed() {
        internalState = internalState.copy(integrationLaunched = false)
    }

    internal fun ensureNoMutationInFlight() {
        if (mutex.isLocked) {
            throw IllegalStateException(
                "Cannot launch while a checkout session mutation is in flight."
            )
        }
    }

    internal fun updateWithResponse(response: CheckoutSessionResponse) {
        internalState = internalState.copy(checkoutSessionResponse = response)
        _checkoutSession.value = internalState.asCheckoutSession()
    }

    private suspend fun withInternalState(
        additionalStateMutations: InternalState.() -> InternalState = { this },
        block: suspend InternalState.(sessionId: String) -> Result<CheckoutSessionResponse>,
    ): Result<Unit> {
        if (internalState.integrationLaunched) {
            return Result.failure(
                IllegalStateException(
                    "Cannot mutate checkout session while a payment flow is presented."
                )
            )
        }
        // Run network requests with a mutex to ensure events are processed in order.
        return mutex.withLock {
            _isLoading.value = true
            val result = runCatching {
                internalState.block(internalState.checkoutSessionResponse.id).getOrThrow()
            }.map { response ->
                internalState = internalState.copy(checkoutSessionResponse = response).additionalStateMutations()
                _checkoutSession.value = internalState.asCheckoutSession()
            }
            _isLoading.value = false
            result
        }
    }

    /**
     * A Composable that renders a currency selector toggle for adaptive pricing.
     *
     * @param appearance Appearance configuration for the currency selector toggle.
     */
    @Composable
    fun CurrencySelectorContent(
        appearance: CurrencySelectorContentAppearance = CurrencySelectorContentAppearance(),
    ) {
        val appearanceState = appearance.build()
        val viewModel: CurrencySelectorViewModel = viewModel(
            factory = CurrencySelectorViewModel.Factory(
                checkoutSession = checkoutSession,
                updateCurrency = ::updateCurrency,
                analyticsRequestExecutor = component.analyticsRequestExecutor,
                paymentAnalyticsRequestFactory = component.paymentAnalyticsRequestFactory,
            )
        )
        val isLoading by isLoading.collectAsState()
        val checkoutSession by checkoutSession.collectAsState()
        val currencySelectorOptions = checkoutSession.currencySelectorOptions ?: return
        val showCurrencyCode =
            appearanceState.labelContent == CurrencySelectorContentAppearance.LabelContent.CURRENCY_CODE
        val errorMessage by viewModel.errorMessage.collectAsState()
        CurrencySelectorToggle(
            options = currencySelectorOptions,
            onCurrencySelected = { currencyOption ->
                viewModel.onCurrencySelected(currencyOption.code)
            },
            isEnabled = !isLoading,
            showCurrencyCode = showCurrencyCode,
            errorMessage = errorMessage?.resolve(),
            appearance = appearanceState,
        )
    }

    private fun fireEvent(event: PaymentSheetEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            component.analyticsRequestExecutor.executeAsync(
                component.paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.params,
                )
            )
        }
    }
}
