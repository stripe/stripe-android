package com.stripe.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UField

internal class EagerApiHostUsageDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UField::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitField(node: UField) {
                val initializer = node.uastInitializer ?: return
                val source = initializer.sourcePsi?.text ?: return
                if (!source.contains("API_HOST")) return

                val ktProperty = node.sourcePsi as? KtProperty ?: return
                if (ktProperty.getter != null) return

                context.report(
                    ISSUE,
                    node,
                    context.getNameLocation(node),
                    "URL property using `API_HOST` must use a `get()` accessor to respect " +
                        "`API_HOST_OVERRIDE` at runtime. Change `val X = ...` to " +
                        "`val X: String get() = ...`."
                )
            }
        }
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            EagerApiHostUsageDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        @JvmField
        val ISSUE = Issue.create(
            id = "EagerApiHostUsage",
            priority = 8,
            briefDescription = "URL property eagerly evaluates `API_HOST`, ignoring runtime overrides",
            explanation = """
                Properties that build URLs using `ApiRequest.API_HOST` must use a `get()` \
                accessor (computed property) so the URL is evaluated fresh on each access. \
                A plain `val` (without `get()`) is evaluated once at class-load time and \
                permanently bakes in the default host, ignoring `API_HOST_OVERRIDE` set \
                later by the playground's Custom Stripe API setting.
            """,
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            androidSpecific = false,
            implementation = IMPLEMENTATION
        )
    }
}
