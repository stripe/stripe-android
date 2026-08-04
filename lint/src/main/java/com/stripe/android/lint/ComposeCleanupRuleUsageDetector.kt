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
import org.jetbrains.uast.UFile
import java.util.EnumSet

internal class ComposeCleanupRuleUsageDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UFile::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        ComposeCleanupRuleUsageVisitor(context, ISSUE)

    companion object {
        private val IMPLEMENTATION = Implementation(
            ComposeCleanupRuleUsageDetector::class.java,
            EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
        )

        @JvmField
        val ISSUE = Issue.create(
            id = "ComposeCleanupRuleUsageIssue",
            priority = 8,
            briefDescription = """
                [androidx.compose.ui.test.junit4.createComposeRule] may cause some tests to stall until failure due to
                Compose not being idle from Robolectric environment reusage.
            """,
            explanation = """
                Use [com.stripe.android.testing.createComposeCleanupRule] alongside 
                [androidx.compose.ui.test.junit4.createComposeRule].
            """,
            category = Category.CUSTOM_LINT_CHECKS,
            severity = Severity.ERROR,
            androidSpecific = null,
            implementation = IMPLEMENTATION
        )
    }
}
