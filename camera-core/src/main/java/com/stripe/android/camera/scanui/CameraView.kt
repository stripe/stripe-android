package com.stripe.android.camera.scanui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import com.stripe.android.camera.R
import com.stripe.android.camera.scanui.CameraView.ViewFinderType
import com.stripe.android.camera.scanui.util.addConstraints
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.camera.scanui.util.constrainToParent
import com.stripe.android.camera.scanui.util.getDrawableByRes
import com.stripe.android.camera.scanui.util.getFloatResource
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A view used for camera, lays out the required subviews and exposes components for [CameraAdapter]
 * and [ScanFlow].
 *
 * Has the following styleable parameters to configure in xml
 *  [R.styleable.StripeCameraView_stripeViewFinderType] - a enum to decide the type of [ViewFinderType], which dictates the aspect ratio of [viewFinderWindowView].
 *  [R.styleable.StripeCameraView_stripeBorderDrawable] - an optional reference for [viewFinderBorderView]'s drawable resource.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CameraView : ConstraintLayout {

    /**
     * a [FrameLayout] to display the preview frame of camera feed, has the same size of the [CameraView].
     */
    val previewFrame: FrameLayout by lazy { FrameLayout(context) }

    /**
     * an optional [ViewFinderBackground] to draw background and reveals a center viewFinder on previewFrame, has the same size of the [CameraView].
     */
    val viewFinderBackgroundView: ViewFinderBackground by lazy {
        ViewFinderBackground(context)
    }

    /**
     * a [View] to highlight a sub area from previewFrame, the [Rect] of this view is used to crop the highlighted area from the preview.
     */
    val viewFinderWindowView: View by lazy { View(context) }

    /**
     * an optional [ImageView] to draw the border of [viewFinderWindowView], exposed for applying animations.
     */
    val viewFinderBorderView: ImageView by lazy { ImageView(context) }

    @DrawableRes
    private var borderDrawable: Int = NO_BORDER

    private var viewFinderType: ViewFinderType = ViewFinderType.CreditCard

    /**
     * Constructor when created programmatically. Leaving [borderDrawable] and
     * [viewFinderType] as default value, add the UI components without adding any constraints.
     * The caller of this constructor is responsible for adding constraint of subviews.
     */
    constructor(context: Context) : super(context) {
        addUiComponents()
    }

    /**
     * Constructor when created programmatically, assigning [borderDrawable] and
     * [viewFinderType] values. The UI components are added and constraints are set up accordingly.
     */
    constructor(
        context: Context,
        argViewFinderType: ViewFinderType,
        @DrawableRes argBorderDrawable: Int = NO_BORDER
    ) : super(context) {
        viewFinderType = argViewFinderType
        borderDrawable = argBorderDrawable
        addUiComponents()
        setupBorder()
        post {
            setupUiConstraints()
        }
    }

    /**
     * Constructor used when inflated from XML, initialize [borderDrawable] and
     * [viewFinderType] from [attrs] and add the constraints accordingly.
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initFromXML(context, attrs)
    }

    /**
     * Constructor used when inflated from XML, initialize [borderDrawable] and
     * [viewFinderType] from [attrs] and add the constraints accordingly.
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initFromXML(context, attrs)
    }

    /**
     * Constructor used when inflated from XML, initialize [borderDrawable] and
     * [viewFinderType] from [attrs] and add the constraints accordingly.
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initFromXML(context, attrs)
    }

    private fun initFromXML(context: Context, attrs: AttributeSet? = null) {
        context.withStyledAttributes(
            attrs,
            R.styleable.StripeCameraView
        ) {
            viewFinderType =
                ViewFinderType.values()[getInt(R.styleable.StripeCameraView_stripeViewFinderType, 0)]
            borderDrawable =
                getResourceId(
                    R.styleable.StripeCameraView_stripeBorderDrawable,
                    NO_BORDER
                )
        }
        addUiComponents()
        setupBorder()
        post {
            setupUiConstraints()
        }
    }

    /**
     * The type of viewfinder, decides if [viewFinderBackgroundView] should be drawn and the aspect ratio of [viewFinderWindowView]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ViewFinderType(
        val aspectRatio: String
    ) {
        /**
         * [Fill] doesn't draw [viewFinderBackgroundView] and fills [viewFinderWindowView] with parent
         */
        Fill(FILL_ASPECT_RATIO),

        /**
         * [CreditCard] draws [viewFinderBackgroundView] and draws [viewFinderWindowView] in center with aspect ratio [CREDIT_CARD_ASPECT_RATIO]
         */
        CreditCard(CREDIT_CARD_ASPECT_RATIO),

        /**
         * [ID] draws [viewFinderBackgroundView] and draws [viewFinderWindowView] in center with aspect ratio [ID]
         */
        ID(ID_ASPECT_RATIO),

        /**
         * [Passport] draws [viewFinderBackgroundView] and draws [viewFinderWindowView] in center with aspect ratio [Passport]
         */
        Passport(PASSPORT_ASPECT_RATIO)
    }

    private fun addUiComponents() {
        listOf(
            previewFrame,
            viewFinderWindowView,
            viewFinderBorderView
        ).forEach {
            it.id = View.generateViewId()
            addView(it)
        }

        // note: viewFinderBackgroundView needs to be added after previewFrame
        if (viewFinderType != ViewFinderType.Fill) {
            viewFinderBackgroundView.id = View.generateViewId()
            addView(viewFinderBackgroundView)
        }
    }

    private fun setupBorder() {
        if (borderDrawable != NO_BORDER) {
            // TODO(ccen) when border is set, need to also set the background of viewFinderWindow
            // to fill the seam between border and darkened area
            viewFinderBorderView.background = context.getDrawableByRes(borderDrawable)
        }
    }

    /**
     * Set up the UI constraints so that -
     * - previewFrame is the same size as the parent
     * - viewFinderBackgroundView is the same size as parent if it's turned on
     * - viewFinderWindowView and viewFinderBorderView is centered with predefined margins and bias
     *
     * TODO(ccen) - consider also parameterizing viewFinderMargin and bias
     */
    private fun setupUiConstraints() {
        previewFrame.layoutParams = LayoutParams(0, 0)
        previewFrame.constrainToParent(this)

        val parentSize = Size(width, height)
        val viewFinderMargin = (
            min(parentSize.width, parentSize.height) *
                context.getFloatResource(R.dimen.stripeViewFinderMargin)
            ).roundToInt()

        if (viewFinderType == ViewFinderType.Fill) {
            listOf(viewFinderWindowView, viewFinderBorderView).forEach { view ->
                view.layoutParams = LayoutParams(0, 0)
                view.constrainToParent(this)
            }
        } else {
            listOf(viewFinderWindowView, viewFinderBorderView).forEach { view ->
                view.layoutParams = LayoutParams(0, 0).apply {
                    topMargin = viewFinderMargin
                    bottomMargin = viewFinderMargin
                    marginStart = viewFinderMargin
                    marginEnd = viewFinderMargin
                }

                view.constrainToParent(this)
                view.addConstraints(this) {
                    setVerticalBias(
                        it.id,
                        context.getFloatResource(R.dimen.stripeViewFinderVerticalBias)
                    )
                    setHorizontalBias(
                        it.id,
                        context.getFloatResource(R.dimen.stripeViewFinderHorizontalBias)
                    )
                    setDimensionRatio(it.id, viewFinderType.aspectRatio)
                }
            }
        }

        post {
            if (viewFinderType != ViewFinderType.Fill) {
                viewFinderBackgroundView.layoutParams = LayoutParams(0, 0)
                viewFinderBackgroundView.constrainToParent(this)
                viewFinderBackgroundView.setViewFinderRect(viewFinderWindowView.asRect())
            }
        }
    }

    private companion object {
        /**
         * For [ViewFinderType.Fill], viewFinderWindowView is the size of parent, no aspect ratio needed
         */
        const val FILL_ASPECT_RATIO = ""
        const val CREDIT_CARD_ASPECT_RATIO = "200:126"
        const val ID_ASPECT_RATIO = "3:2"
        const val PASSPORT_ASPECT_RATIO = "3:2"
        const val NO_BORDER = -1
    }
}
