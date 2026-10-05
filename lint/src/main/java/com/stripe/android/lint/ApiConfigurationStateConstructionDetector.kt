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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastCallKind

internal class ApiConfigurationStateConstructionDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            private val reportedCalls = mutableSetOf<Any>()

            override fun visitCallExpression(node: UCallExpression) {
                if (node.kind != UastCallKind.CONSTRUCTOR_CALL) return
                if (context.isInTestSourceSet()) return

                val constructor = node.resolve() ?: return
                if (constructor.containingClass?.qualifiedName != API_CONFIGURATION_STATE) return
                if (node.isApiConfigurationBuilderImplementation()) return
                if (reportedCalls.add(node.sourcePsi ?: node).not()) return

                context.report(
                    ISSUE,
                    node,
                    context.getNameLocation(node),
                    "Do not construct `ApiConfiguration.State` directly. Use " +
                        "`ApiConfiguration(...).stripeAccountId(...).build()` instead."
                )
            }
        }

    private fun JavaContext.isInTestSourceSet(): Boolean {
        val path = file.path.replace('\\', '/')
        return "/src/test/" in path || "/src/androidTest/" in path
    }

    private fun UCallExpression.isApiConfigurationBuilderImplementation(): Boolean {
        var parent = uastParent
        while (parent != null) {
            if (parent is UMethod) {
                return parent.name == "build" &&
                    parent.javaPsi.containingClass?.qualifiedName == API_CONFIGURATION
            }
            parent = parent.uastParent
        }
        return false
    }

    companion object {
        private const val API_CONFIGURATION = "com.stripe.android.core.ApiConfiguration"
        private const val API_CONFIGURATION_STATE = "$API_CONFIGURATION.State"

        private val IMPLEMENTATION = Implementation(
            ApiConfigurationStateConstructionDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        @JvmField
        val ISSUE = Issue.create(
            id = "ApiConfigurationStateConstruction",
            priority = 8,
            briefDescription = "Do not construct ApiConfiguration.State directly",
            explanation = "Use the ApiConfiguration builder to create ApiConfiguration.State.",
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            androidSpecific = false,
            implementation = IMPLEMENTATION
        )
    }
}
