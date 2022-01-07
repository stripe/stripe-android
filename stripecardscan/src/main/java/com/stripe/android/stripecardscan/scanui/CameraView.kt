package com.stripe.android.stripecardscan.scanui

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
import com.stripe.android.stripecardscan.R
import com.stripe.android.stripecardscan.camera.CameraAdapter
import com.stripe.android.stripecardscan.scanui.CameraView.ViewFinderType
import com.stripe.android.stripecardscan.scanui.util.addConstraints
import com.stripe.android.stripecardscan.scanui.util.asRect
import com.stripe.android.stripecardscan.scanui.util.constrainToParent
import com.stripe.android.stripecardscan.scanui.util.getDrawableByRes
import com.stripe.android.stripecardscan.scanui.util.getFloatResource
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A view used for camera, lays out the required subviews and exposes components for [CameraAdapter]
 * and [ScanFlow].
 *
 * Has the following styleable parameters to configure in xml
 *  [R.styleable.CameraView_viewFinderType] - a enum to decide the type of [ViewFinderType], which dictates the aspect ratio of [viewFinderWindowView].
 *  [R.styleable.CameraView_borderDrawable] - an optional reference for [viewFinderBorderView]'s drawable resource.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
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

    private var isCreatedFromXML: Boolean = false

    private var viewFinderType: ViewFinderType = ViewFinderType.CreditCard

    /**
     * The type of viewfinder, decides if [viewFinderBackgroundView] should be drawn and the aspect ratio of [viewFinderWindowView]
     */
    private enum class ViewFinderType(
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

    init {
        context.withStyledAttributes(
            attrs,
            R.styleable.CameraView
        ) {
            // if it's null then the view is created programmatically by Bouncer, default to credit
            // card specifics
            // TODO(awush): migrate Bouncer to use xml with type [ViewFinderType.CreditCard]
            isCreatedFromXML = (attrs != null)
            if (isCreatedFromXML) {
                viewFinderType =
                    ViewFinderType.values()[getInt(R.styleable.CameraView_viewFinderType, 0)]
                borderDrawable =
                    getResourceId(
                        R.styleable.CameraView_borderDrawable,
                        NO_BORDER
                    )
            } else {
                viewFinderType = ViewFinderType.CreditCard
            }
        }
        addUiComponents()
        // only setup constraints if it is created from XML, otherwise let the user set up the
        // constraints programmatically
        // TODO(awush): remove this after Bouncer no longer creates it programmatically
        if (isCreatedFromXML) {
            setupBorder()
            post {
                setupUiConstraints()
            }
        }
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
        // TODO(ccen) Finalize the correct aspect ratio for ID and PASSPORT
        const val ID_ASPECT_RATIO = "1:1"
        const val PASSPORT_ASPECT_RATIO = "1:2"
        const val NO_BORDER = -1
    }
}
