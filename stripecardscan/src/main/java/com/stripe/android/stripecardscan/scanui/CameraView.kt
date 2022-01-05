package com.stripe.android.stripecardscan.scanui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import com.stripe.android.stripecardscan.R
import com.stripe.android.stripecardscan.scanui.util.addConstraints
import com.stripe.android.stripecardscan.scanui.util.asRect
import com.stripe.android.stripecardscan.scanui.util.constrainToParent
import com.stripe.android.stripecardscan.scanui.util.getDrawableByRes
import com.stripe.android.stripecardscan.scanui.util.getFloatResource
import kotlin.math.min
import kotlin.math.roundToInt

fun logBglm(msg: String) {
    Log.d("BGLM", msg)
}

/**
 * TODO(ccen): Test it in SimpleScanActivity
 */
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    // has to be framelayout to center
    // exposed for camera adapter
    val previewFrame: FrameLayout by lazy { FrameLayout(context) }

    // viewFinder
//    *) the viewFinder size determines the size of the hole of VieFinderBackgroundView
//    *) the size also determines the rect() to be returned to cut
//    *) the size needs to be smaller than the full size
//    *) can be optionally configured with a border

    // darkens background, punch a hole
    // exposed for animation and state change
    val viewFinderBackgroundView: ViewFinderBackground by lazy {
        ViewFinderBackground(context)
    }

    // exposed for state change
    val viewFinderWindowView: View by lazy { View(context) }

    // exposed for state change and animation
    val viewFinderBorderView: ImageView by lazy { ImageView(context) }

    fun getViewFinderRect(): Rect = viewFinderWindowView.asRect()

    @DrawableRes
    private var borderDrawable: Int = NO_BORDER

    private var isCreatedFromXML: Boolean = false

    private enum class ViewFinderType(
        val aspectRatio: String
    ) {
        Fill(FILL_ASPECT_RATIO),
        CreditCard(CREDIT_CARD_ASPECT_RATIO),
        ID(ID_ASPECT_RATIO),
        Passport(PASSPORT_ASPECT_RATIO)
    }

    private lateinit var viewFinderType: ViewFinderType

    init {
        context.withStyledAttributes(
            attrs,
            R.styleable.CameraView
        ) {
            // if it's null then the view is created programmatically by Bouncer,
            // default to credit card specifics
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
        // only setup constraints if it is created from XML,
        // otherwise let the user set up the constraints programmatically
        if (isCreatedFromXML) {
            setupBorder()
            post{
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
     * Set up the UI constraints so that
     * previewFrame is the same size as the parent
     * viewFinderBackgroundView is the same size as parent
     * viewFinderWindowView and viewFinderBorderView is centered
     */
    private fun setupUiConstraints() {
        // previewFrame
        previewFrame.layoutParams = LayoutParams(0, 0)
        previewFrame.constrainToParent(this)




        // TODO(ccen) change this to parent width and height
//            val screenSize = Resources.getSystem().displayMetrics.let {
//                Size(it.widthPixels, it.heightPixels)
//            }
        // might be 0, 0
        val parentSize = Size(width, height)

        // Note(ccen) this decides how narrow the viewFinder is
        // TODO(ccen) parameterize the margin?
        val viewFinderMargin = (
            min(parentSize.width, parentSize.height) *
                context.getFloatResource(R.dimen.stripeViewFinderMargin)
            ).roundToInt()

        logBglm("before: widow: ${viewFinderWindowView.asRect()}, \n border: ${viewFinderBorderView.asRect()}")

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
                        // TODO(ccen): parameterize the bias?
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
        logBglm("after: widow: ${viewFinderWindowView.asRect()}, \n border: ${viewFinderBorderView.asRect()}")


        post {
            logBglm("final: widow: ${viewFinderWindowView.asRect()}, \n border: ${viewFinderBorderView.asRect()}")

            // TODO(ccen) background turned off for Fill
            if (viewFinderType != ViewFinderType.Fill) {
                viewFinderBackgroundView.layoutParams = LayoutParams(0, 0)
                viewFinderBackgroundView.constrainToParent(this)
                viewFinderBackgroundView.setViewFinderRect(viewFinderWindowView.asRect())
            }
        }
    }


    private companion object {
        // aspect ratio not used for Fill
        const val FILL_ASPECT_RATIO = ""
        const val CREDIT_CARD_ASPECT_RATIO = "200:126"
        const val ID_ASPECT_RATIO = "1:1"
        const val PASSPORT_ASPECT_RATIO = "1:2"
        const val NO_BORDER = -1
    }

}