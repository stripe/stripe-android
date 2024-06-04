package com.stripe.android.stripecardscan.scanui

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Typeface
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.camera.scanui.ScanFlow
import com.stripe.android.camera.scanui.ViewFinderBackground
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.camera.scanui.util.setDrawable
import com.stripe.android.stripecardscan.R
import com.stripe.android.stripecardscan.scanui.util.getColorByRes
import com.stripe.android.stripecardscan.scanui.util.getDrawableByRes
import com.stripe.android.stripecardscan.scanui.util.getFloatResource
import com.stripe.android.stripecardscan.scanui.util.setHtmlString
import com.stripe.android.stripecardscan.scanui.util.setTextSizeByRes
import com.stripe.android.stripecardscan.scanui.util.setVisible
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlin.math.min
import kotlin.math.roundToInt
import com.stripe.android.camera.R as CameraR

internal abstract class SimpleScanActivity<ScanFlowParameters> : ScanActivity() {

    /**
     * The main layout used to render the scan view.
     */
    protected open val layout: CameraView by lazy { CameraView(this) }

    /**
     * The frame where the camera preview will be displayed. This is usually the full screen.
     */
    override val previewFrame: ViewGroup by lazy { layout.previewFrame }

    /**
     * The text view that displays the cardholder name once a card has been scanned.
     */
    protected open val cardNameTextView: TextView by lazy { TextView(this) }

    /**
     * The text view that displays the card number once a card has been scanned.
     */
    protected open val cardNumberTextView: TextView by lazy { TextView(this) }

    /**
     * The view that the user can tap to close the scan window.
     */
    protected open val closeButtonView: View by lazy { ImageView(this) }

    /**
     * The view that a user can tap to turn on the flashlight.
     */
    protected open val torchButtonView: View by lazy { ImageView(this) }

    /**
     * The view that a user can tap to swap cameras.
     */
    protected open val swapCameraButtonView: View by lazy { ImageView(this) }

    /**
     * The text view that informs the user what to do.
     */
    protected open val instructionsTextView: TextView by lazy { TextView(this) }

    /**
     * The icon used to display a lock to indicate that the scanned card is secure.
     */
    protected open val securityIconView: ImageView by lazy { ImageView(this) }

    /**
     * The text view used to inform the user that the scanned card is secure.
     */
    protected open val securityTextView: TextView by lazy { TextView(this) }

    /**
     * The text view used to inform user that Stripe is used to verify card data.
     */
    protected open val privacyLinkTextView: TextView by lazy { TextView(this) }

    /**
     * The background that draws the user focus to the view finder.
     */
    protected open val viewFinderBackgroundView: ViewFinderBackground
        by lazy { layout.viewFinderBackgroundView }

    /**
     * The view finder window view.
     */
    protected open val viewFinderWindowView: View by lazy { layout.viewFinderWindowView }

    /**
     * The border around the view finder.
     */
    protected open val viewFinderBorderView: ImageView by lazy { layout.viewFinderBorderView }

    /**
     * The aspect ratio of the view finder.
     */
    protected open val viewFinderAspectRatio = "200:126"

    /**
     * Determine if the flashlight is supported.
     */
    protected var isFlashlightSupported: Boolean? = null

    /**
     * Determine if multiple cameras are available.
     */
    protected var hasMultipleCameras: Boolean? = null

    /**
     * The flow used to scan an item.
     */
    internal abstract val scanFlow: ScanFlow<ScanFlowParameters, CameraPreviewImage<Bitmap>>

    /**
     * The scan flow parameters that will be populated.
     */
    protected lateinit var deferredScanFlowParameters: Deferred<ScanFlowParameters>

    /**
     * Determine if the background is dark. This is used to set light background vs dark background
     * text and images.
     */
    protected open fun isBackgroundDark(): Boolean =
        viewFinderBackgroundView.getBackgroundLuminance() < 128

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addUiComponents()
        setupUiComponents()
        setupUiConstraints()

        closeButtonView.setOnClickListener { userClosedScanner() }
        torchButtonView.setOnClickListener { toggleFlashlight() }
        swapCameraButtonView.setOnClickListener { toggleCamera() }

        viewFinderBorderView.setOnTouchListener { _, e ->
            setFocus(PointF(e.x + viewFinderWindowView.left, e.y + viewFinderWindowView.top))
            true
        }

        setContentView(layout)
    }

    override fun onPause() {
        viewFinderBackgroundView.clearOnDrawListener()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewFinderBackgroundView.setOnDrawListener { setupUiComponents() }
    }

    override fun onDestroy() {
        scanFlow.cancelFlow()
        super.onDestroy()
    }

    /**
     * Add the UI components to the root view.
     */
    protected open fun addUiComponents() {
        layout.id = View.generateViewId()

        appendUiComponents(
            securityIconView,
            securityTextView,
            instructionsTextView,
            closeButtonView,
            torchButtonView,
            swapCameraButtonView,
            cardNameTextView,
            cardNumberTextView,
            privacyLinkTextView
        )
    }

    /**
     * Append additional UI elements to the view.
     */
    protected fun appendUiComponents(vararg components: View) {
        components.forEach {
            it.id = View.generateViewId()
            layout.addView(it)
        }
    }

    protected open fun setupUiComponents() {
        setupCloseButtonViewUi()
        setupTorchButtonViewUi()
        setupSwapCameraButtonViewUi()
        setupViewFinderViewUI()
        setupInstructionsViewUi()
        setupSecurityNoticeUi()
        setupCardDetailsUi()
        setupPrivacyLinkTextUi()
    }

    protected open fun setupCloseButtonViewUi() {
        when (val view = closeButtonView) {
            is ImageView -> {
                view.contentDescription = getString(R.string.stripe_close_button_description)
                if (isBackgroundDark()) {
                    view.setDrawable(R.drawable.stripe_close_button_dark)
                } else {
                    view.setDrawable(R.drawable.stripe_close_button_light)
                }
            }
            is TextView -> {
                view.text = getString(R.string.stripe_close_button_description)
                if (isBackgroundDark()) {
                    view.setTextColor(getColorByRes(R.color.stripeCloseButtonDarkColor))
                } else {
                    view.setTextColor(getColorByRes(R.color.stripeCloseButtonLightColor))
                }
            }
        }
    }

    protected open fun setupTorchButtonViewUi() {
        torchButtonView.setVisible(isFlashlightSupported == true)
        when (val view = torchButtonView) {
            is ImageView -> {
                view.contentDescription = getString(R.string.stripe_torch_button_description)
                if (isBackgroundDark()) {
                    if (isFlashlightOn) {
                        view.setDrawable(R.drawable.stripe_flash_on_dark)
                    } else {
                        view.setDrawable(R.drawable.stripe_flash_off_dark)
                    }
                } else {
                    if (isFlashlightOn) {
                        view.setDrawable(R.drawable.stripe_flash_on_light)
                    } else {
                        view.setDrawable(R.drawable.stripe_flash_off_light)
                    }
                }
            }
            is TextView -> {
                view.text = getString(R.string.stripe_torch_button_description)
                if (isBackgroundDark()) {
                    view.setTextColor(getColorByRes(R.color.stripeFlashButtonDarkColor))
                } else {
                    view.setTextColor(getColorByRes(R.color.stripeFlashButtonLightColor))
                }
            }
        }
    }

    protected open fun setupSwapCameraButtonViewUi() {
        swapCameraButtonView.setVisible(hasMultipleCameras == true)
        when (val view = swapCameraButtonView) {
            is ImageView -> {
                view.contentDescription = getString(R.string.stripe_swap_camera_button_description)
                if (isBackgroundDark()) {
                    view.setDrawable(R.drawable.stripe_camera_swap_dark)
                } else {
                    view.setDrawable(R.drawable.stripe_camera_swap_light)
                }
            }
            is TextView -> {
                view.text = getString(R.string.stripe_swap_camera_button_description)
                if (isBackgroundDark()) {
                    view.setTextColor(getColorByRes(R.color.stripeCameraSwapButtonDarkColor))
                } else {
                    view.setTextColor(getColorByRes(R.color.stripeCameraSwapButtonLightColor))
                }
            }
        }
    }

    protected open fun setupViewFinderViewUI() {
        viewFinderBorderView.background = getDrawableByRes(R.drawable.stripe_card_border_not_found)
    }

    protected open fun setupInstructionsViewUi() {
        instructionsTextView.setTextSizeByRes(R.dimen.stripeInstructionsTextSize)
        instructionsTextView.typeface = Typeface.DEFAULT_BOLD
        instructionsTextView.gravity = Gravity.CENTER

        if (isBackgroundDark()) {
            instructionsTextView.setTextColor(getColorByRes(R.color.stripeInstructionsColorDark))
        } else {
            instructionsTextView.setTextColor(getColorByRes(R.color.stripeInstructionsColorLight))
        }
    }

    protected open fun setupSecurityNoticeUi() {
        securityTextView.text = getString(R.string.stripe_card_scan_security)
        securityTextView.setTextSizeByRes(R.dimen.stripeSecurityTextSize)
        securityIconView.contentDescription = getString(R.string.stripe_security_description)

        if (isBackgroundDark()) {
            securityTextView.setTextColor(getColorByRes(R.color.stripeSecurityColorDark))
            securityIconView.setDrawable(R.drawable.stripe_lock_dark)
        } else {
            securityTextView.setTextColor(getColorByRes(R.color.stripeSecurityColorLight))
            securityIconView.setDrawable(R.drawable.stripe_lock_light)
        }
    }

    protected open fun setupCardDetailsUi() {
        cardNumberTextView.setTextColor(getColorByRes(R.color.stripeCardPanColor))
        cardNumberTextView.setTextSizeByRes(R.dimen.stripePanTextSize)
        cardNumberTextView.gravity = Gravity.CENTER
        cardNumberTextView.typeface = Typeface.DEFAULT_BOLD
        cardNumberTextView.setShadowLayer(
            getFloatResource(R.dimen.stripePanStrokeSize),
            0F,
            0F,
            getColorByRes(R.color.stripeCardPanOutlineColor)
        )

        cardNameTextView.setTextColor(getColorByRes(R.color.stripeCardNameColor))
        cardNameTextView.setTextSizeByRes(R.dimen.stripeNameTextSize)
        cardNameTextView.gravity = Gravity.CENTER
        cardNameTextView.typeface = Typeface.DEFAULT_BOLD
        cardNameTextView.setShadowLayer(
            getFloatResource(R.dimen.stripeNameStrokeSize),
            0F,
            0F,
            getColorByRes(R.color.stripeCardNameOutlineColor)
        )
    }

    /**
     * Configures the privacy link blurb that allows the user to view
     * the Stripe privacy policy on the web.
     * <p>
     * NOTE: THIS STRING SHOULD NOT BE MODIFIED
     */
    protected fun setupPrivacyLinkTextUi() {
        privacyLinkTextView.setHtmlString(getString(R.string.stripe_card_scan_privacy_link_text))
        privacyLinkTextView.setTextSizeByRes(R.dimen.stripePrivacyLinkTextSize)
        privacyLinkTextView.gravity = Gravity.CENTER

        var textColor = getColorByRes(R.color.stripePrivacyLinkColorLight)
        if (isBackgroundDark()) {
            textColor = getColorByRes(R.color.stripePrivacyLinkColorDark)
        }

        privacyLinkTextView.setTextColor(textColor)
        privacyLinkTextView.setLinkTextColor(textColor)
    }

    protected open fun setupUiConstraints() {
        setupPreviewFrameConstraints()
        setupCloseButtonViewConstraints()
        setupTorchButtonViewConstraints()
        setupSwapCameraButtonViewConstraints()
        setupViewFinderConstraints()
        setupInstructionsViewConstraints()
        setupSecurityNoticeConstraints()
        setupCardDetailsConstraints()
        setupPrivacyLinkViewConstraints()
    }

    protected open fun setupPreviewFrameConstraints() {
        previewFrame.layoutParams = ConstraintLayout.LayoutParams(0, 0)
        previewFrame.constrainToParent()
    }

    protected open fun setupCloseButtonViewConstraints() {
        closeButtonView.layoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, // width
            ViewGroup.LayoutParams.WRAP_CONTENT // height
        ).apply {
            topMargin = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            marginStart = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
        }

        closeButtonView.addConstraints {
            connect(it.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        }
    }

    protected open fun setupTorchButtonViewConstraints() {
        torchButtonView.layoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, // width
            ViewGroup.LayoutParams.WRAP_CONTENT // height
        ).apply {
            topMargin = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            marginStart = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
        }

        torchButtonView.addConstraints {
            connect(it.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(it.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }
    }

    protected open fun setupSwapCameraButtonViewConstraints() {
        swapCameraButtonView.layoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, // width
            ViewGroup.LayoutParams.WRAP_CONTENT // height
        ).apply {
            topMargin = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            marginStart = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
        }

        swapCameraButtonView.addConstraints {
            connect(it.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        }
    }

    protected open fun setupViewFinderConstraints() {
        viewFinderBackgroundView.layoutParams = ConstraintLayout.LayoutParams(0, 0)

        viewFinderBackgroundView.constrainToParent()

        val screenSize = Resources.getSystem().displayMetrics.let {
            Size(it.widthPixels, it.heightPixels)
        }
        val viewFinderMargin = (
            min(screenSize.width, screenSize.height) *
                getFloatResource(CameraR.dimen.stripeViewFinderMargin)
            ).roundToInt()

        listOf(viewFinderWindowView, viewFinderBorderView).forEach { view ->
            view.layoutParams = ConstraintLayout.LayoutParams(0, 0).apply {
                topMargin = viewFinderMargin
                bottomMargin = viewFinderMargin
                marginStart = viewFinderMargin
                marginEnd = viewFinderMargin
            }

            view.constrainToParent()
            view.addConstraints {
                setVerticalBias(it.id, getFloatResource(CameraR.dimen.stripeViewFinderVerticalBias))
                setHorizontalBias(it.id, getFloatResource(CameraR.dimen.stripeViewFinderHorizontalBias))

                setDimensionRatio(it.id, viewFinderAspectRatio)
            }
        }
    }

    protected open fun setupInstructionsViewConstraints() {
        instructionsTextView.layoutParams = ConstraintLayout.LayoutParams(
            0, // width
            ViewGroup.LayoutParams.WRAP_CONTENT // height
        ).apply {
            topMargin = resources.getDimensionPixelSize(R.dimen.stripeInstructionsMargin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.stripeInstructionsMargin)
            marginStart = resources.getDimensionPixelSize(R.dimen.stripeInstructionsMargin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeInstructionsMargin)
        }

        instructionsTextView.addConstraints {
            connect(it.id, ConstraintSet.BOTTOM, viewFinderWindowView.id, ConstraintSet.TOP)
            connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(it.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }
    }

    protected open fun setupPrivacyLinkViewConstraints() {
        privacyLinkTextView.layoutParams = ConstraintLayout.LayoutParams(
            0, // width
            ViewGroup.LayoutParams.WRAP_CONTENT // height
        ).apply {
            topMargin = resources.getDimensionPixelSize(R.dimen.stripeInstructionsMargin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.stripeInstructionsMargin)
            marginStart = resources.getDimensionPixelSize(R.dimen.stripeInstructionsMargin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeInstructionsMargin)
        }

        privacyLinkTextView.addConstraints {
            connect(it.id, ConstraintSet.TOP, securityTextView.id, ConstraintSet.BOTTOM)
            connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(it.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }
    }

    protected open fun setupSecurityNoticeConstraints() {
        securityIconView.layoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, // width
            0 // height
        ).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeSecurityIconMargin)
        }

        securityTextView.layoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, // width
            ViewGroup.LayoutParams.WRAP_CONTENT // height
        ).apply {
            topMargin = resources.getDimensionPixelSize(R.dimen.stripeSecurityMargin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.stripeSecurityMargin)
        }

        securityIconView.addConstraints {
            connect(it.id, ConstraintSet.TOP, securityTextView.id, ConstraintSet.TOP)
            connect(it.id, ConstraintSet.BOTTOM, securityTextView.id, ConstraintSet.BOTTOM)
            connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(it.id, ConstraintSet.END, securityTextView.id, ConstraintSet.START)

            setHorizontalChainStyle(it.id, ConstraintSet.CHAIN_PACKED)
        }

        securityTextView.addConstraints {
            connect(it.id, ConstraintSet.TOP, viewFinderWindowView.id, ConstraintSet.BOTTOM)
            connect(it.id, ConstraintSet.START, securityIconView.id, ConstraintSet.END)
            connect(it.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }
    }

    protected open fun setupCardDetailsConstraints() {
        cardNumberTextView.layoutParams = ConstraintLayout.LayoutParams(
            0, // width
            ViewGroup.LayoutParams.WRAP_CONTENT // height
        ).apply {
            marginStart = resources.getDimensionPixelSize(R.dimen.stripeCardDetailsMargin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeCardDetailsMargin)
        }

        cardNameTextView.layoutParams = ConstraintLayout.LayoutParams(
            0, // width
            ViewGroup.LayoutParams.WRAP_CONTENT // height
        ).apply {
            marginStart = resources.getDimensionPixelSize(R.dimen.stripeCardDetailsMargin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeCardDetailsMargin)

            topToBottom = cardNumberTextView.id
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }

        cardNumberTextView.addConstraints {
            connect(it.id, ConstraintSet.TOP, viewFinderWindowView.id, ConstraintSet.TOP)
            connect(it.id, ConstraintSet.BOTTOM, cardNameTextView.id, ConstraintSet.TOP)
            connect(it.id, ConstraintSet.START, viewFinderWindowView.id, ConstraintSet.START)
            connect(it.id, ConstraintSet.END, viewFinderWindowView.id, ConstraintSet.END)

            setVerticalChainStyle(it.id, ConstraintSet.CHAIN_PACKED)
        }

        cardNameTextView.addConstraints {
            connect(it.id, ConstraintSet.TOP, cardNumberTextView.id, ConstraintSet.BOTTOM)
            connect(it.id, ConstraintSet.BOTTOM, viewFinderWindowView.id, ConstraintSet.BOTTOM)
            connect(it.id, ConstraintSet.START, viewFinderWindowView.id, ConstraintSet.START)
            connect(it.id, ConstraintSet.END, viewFinderWindowView.id, ConstraintSet.END)
        }
    }

    override fun onFlashlightStateChanged(flashlightOn: Boolean) {
        setupUiComponents()
    }

    override fun onCameraReady() {
        viewFinderBackgroundView.setViewFinderRect(viewFinderWindowView.asRect())
        startCameraAdapter()
    }

    override fun onFlashSupported(supported: Boolean) {
        isFlashlightSupported = supported
        torchButtonView.setVisible(supported)
    }

    override fun onSupportsMultipleCameras(supported: Boolean) {
        hasMultipleCameras = supported
        swapCameraButtonView.setVisible(supported)
    }

    /**
     * Add constraints to a view.
     */
    protected inline fun <T : View> T.addConstraints(block: ConstraintSet.(view: T) -> Unit) {
        ConstraintSet().apply {
            clone(layout)
            block(this, this@addConstraints)
            applyTo(layout)
        }
    }

    /**
     * Constrain a view to the top, bottom, start, and end of its parent.
     */
    protected fun <T : View> T.constrainToParent() {
        addConstraints {
            connect(it.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(it.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(it.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }
    }

    /**
     * Once the camera stream is available, start processing images.
     */
    override suspend fun onCameraStreamAvailable(cameraStream: Flow<CameraPreviewImage<Bitmap>>) {
        scanFlow.startFlow(
            context = this,
            imageStream = cameraStream,
            viewFinder = viewFinderWindowView.asRect(),
            lifecycleOwner = this,
            coroutineScope = this,
            parameters = deferredScanFlowParameters.await()
        )
    }
}
