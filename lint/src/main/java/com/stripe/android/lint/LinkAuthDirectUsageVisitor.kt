package com.stripe.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.UImportStatement

internal class LinkAuthDirectUsageVisitor(
    private val context: JavaContext,
    private val issue: Issue,
) : UElementHandler() {

    override fun visitImportStatement(node: UImportStatement) {
        if (node.importReference?.asSourceString() == DISALLOWED_IMPORT_NAME) {
            context.report(
                issue = issue,
                scope = node,
                location = context.getNameLocation(node),
                message = "Do not import LinkAuth directly. Use LinkAccountManager.lookup() or .signup() instead."
            )
        }
    }

    companion object {
        private const val DISALLOWED_IMPORT_NAME = "com.stripe.android.link.account.LinkAuth"
        private const val ALLOWED_CLASS_NAME = "com.stripe.android.link.account.DefaultLinkAccountManager"
    }
}
