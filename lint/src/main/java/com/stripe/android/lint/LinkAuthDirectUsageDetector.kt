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

/**
 * Detects direct usage of LinkAuth class which should not be used directly.
 * All authentication operations should go through LinkAccountManager instead.
 */
internal class LinkAuthDirectUsageDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UFile::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        LinkAuthDirectUsageVisitor(context, ISSUE)

    companion object {
        private val IMPLEMENTATION = Implementation(
            LinkAuthDirectUsageDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        @JvmField
        val ISSUE = Issue.create(
            id = "LinkAuthDirectUsage",
            priority = 10,
            briefDescription = "LinkAuth should not be used directly",
            explanation = """
                Do not inject or use LinkAuth directly.
                
                Use LinkAccountManager.lookup() or LinkAccountManager.signup() instead.
                
                Direct usage of LinkAuth bypasses the intended architecture where LinkAccountManager 
                is the single source of truth for all Link authentication operations.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            androidSpecific = false,
            implementation = IMPLEMENTATION
        )
    }
}
