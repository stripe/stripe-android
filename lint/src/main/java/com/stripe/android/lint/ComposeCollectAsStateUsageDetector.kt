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
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement

internal class ComposeCollectAsStateUsageDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UImportStatement::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        ComposeCollectAsStateUsageVisitor(context, ISSUE)

    companion object {
        private val IMPLEMENTATION = Implementation(
            ComposeCollectAsStateUsageDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        @JvmField
        val ISSUE = Issue.create(
            id = "ComposeCollectAsStateUsageIssue",
            priority = 8,
            briefDescription = """
                [androidx.compose.runtime.collectAsState] fetches the [StateFlow] value on every recomposition.
            """,
            explanation = """
                Use [com.stripe.android.uicore.utils.collectAsState] instead.
            """,
            category = Category.CUSTOM_LINT_CHECKS,
            severity = Severity.ERROR,
            androidSpecific = null,
            implementation = IMPLEMENTATION
        )
    }
}
