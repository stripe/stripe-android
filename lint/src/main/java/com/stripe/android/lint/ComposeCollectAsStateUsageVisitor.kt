package com.stripe.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import org.jetbrains.uast.UImportStatement

internal class ComposeCollectAsStateUsageVisitor(
    private val context: JavaContext,
    private val issue: Issue,
) : UElementHandler() {
    override fun visitImportStatement(node: UImportStatement) {
        if (node.importReference?.asSourceString() == DISALLOWED_IMPORT_NAME) {
            context.report(
                issue = issue,
                scope = node,
                location = context.getNameLocation(node),
                message = """
                    Do not use the [$DISALLOWED_IMPORT_NAME] composable function
                """.trimIndent(),
                quickfixData = LintFix
                    .create()
                    .replace()
                    .text(DISALLOWED_IMPORT_NAME)
                    .with(ACCEPTABLE_IMPORT_NAME)
                    .build()
            )
        }
    }

    companion object {
        private const val DISALLOWED_IMPORT_NAME = "androidx.compose.runtime.collectAsState"
        private const val ACCEPTABLE_IMPORT_NAME = "com.stripe.android.uicore.utils.collectAsState"
    }
}
