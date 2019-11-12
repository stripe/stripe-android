package com.stripe.android.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.annotation.VisibleForTesting
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.utils.ClassUtils
import java.lang.reflect.Method

/**
 * This class uses Reflection to make the Material Component's floating hint text move above
 * a DrawableLeft, instead of just straight up beside it. If the Material Components library ever
 * officially support this behavior, this class should be removed to avoid Reflection.
 */
class IconTextInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextInputLayout(context, attrs, defStyleAttr) {
    private val collapsingTextHelper: Any?
    private val bounds: Rect?
    private val recalculateMethod: Method?

    init {
        /*
         * Note: this method will break if we upgrade our version of the library
         * and the variable and method names change. We should remove usage of reflection
         * at the first opportunity.
         */
        collapsingTextHelper = ClassUtils.getInternalObject(
            TextInputLayout::class.java, TEXT_FIELD_NAMES, this
        )
        if (collapsingTextHelper == null) {
            bounds = null
            recalculateMethod = null
        } else {
            bounds = ClassUtils.getInternalObject(
                collapsingTextHelper.javaClass, BOUNDS_FIELD_NAMES, collapsingTextHelper
            ) as Rect?
            recalculateMethod = ClassUtils.findMethod(
                collapsingTextHelper.javaClass, RECALCULATE_METHOD_NAMES
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        adjustBounds()
    }

    private fun adjustBounds() {
        val editText = editText
        if (collapsingTextHelper == null || bounds == null || recalculateMethod == null ||
            editText == null) {
            return
        }

        try {
            bounds.left = editText.left + editText.paddingStart
            recalculateMethod.invoke(collapsingTextHelper)
        } catch (ignore: Exception) {
        }
    }

    @VisibleForTesting
    internal fun hasObtainedCollapsingTextHelper(): Boolean {
        return collapsingTextHelper != null && bounds != null && recalculateMethod != null
    }

    private companion object {
        private val BOUNDS_FIELD_NAMES =
            setOf("mCollapsedBounds", "collapsedBounds")
        private val TEXT_FIELD_NAMES =
            setOf("mCollapsingTextHelper", "collapsingTextHelper")
        private val RECALCULATE_METHOD_NAMES = setOf("recalculate")
    }
}
