package com.stripe.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.visitor.AbstractUastVisitor

internal class ComposeCleanupRuleUsageVisitor(
    private val context: JavaContext,
    private val issue: Issue,
) : UElementHandler() {
    override fun visitFile(node: UFile) {
        node.accept(ComposeRuleCheckVisitor(context, issue))
    }

    private class ComposeRuleCheckVisitor(
        private val context: JavaContext,
        private val issue: Issue,
    ) : AbstractUastVisitor() {
        private var createComposeCall: UImportStatement? = null
        private var isRobolectricTest: Boolean = false
        private var hasCleanupComposeCall: Boolean = false

        override fun visitImportStatement(node: UImportStatement): Boolean {
            val importReference = node.importReference?.asSourceString()

            when {
                importReference == ROBOLECTRIC_TEST_RUNNER_FULL_NAME ||
                    importReference == PARAMETERIZED_ROBOLECTRIC_TEST_RUNNER_FULL_NAME -> {
                    isRobolectricTest = true
                }
                importReference == COMPOSE_RULE_FULL_NAME -> {
                    createComposeCall = node
                }
                // Match the cleanup rule by its simple function name rather than a fully-qualified
                // name. Modules that can't depend on payments-core-testing keep a local
                // createComposeCleanupRule, so any package satisfies the check.
                importReference?.substringAfterLast('.') == COMPOSE_CLEANUP_RULE_SIMPLE_NAME -> {
                    hasCleanupComposeCall = true
                }
                else -> Unit
            }

            return super.visitImportStatement(node)
        }

        override fun afterVisitClass(node: UClass) {
            createComposeCall?.let { call ->
                if (isRobolectricTest && !hasCleanupComposeCall) {
                    context.report(
                        issue = issue,
                        scope = call,
                        location = context.getNameLocation(call),
                        message = """
                            No cleanup rule found, please use `createComposeCleanupRule` alongside `createComposeRule`!
                        """.trimIndent(),
                        quickfixData = null
                    )
                }
            }

            isRobolectricTest = false
            createComposeCall = null
            hasCleanupComposeCall = false
        }
    }

    private companion object {
        private const val ROBOLECTRIC_TEST_RUNNER_FULL_NAME = "org.robolectric.RobolectricTestRunner"
        private const val PARAMETERIZED_ROBOLECTRIC_TEST_RUNNER_FULL_NAME =
            "org.robolectric.ParameterizedRobolectricTestRunner"

        private const val COMPOSE_RULE_FULL_NAME = "androidx.compose.ui.test.junit4.createComposeRule"
        private const val COMPOSE_CLEANUP_RULE_SIMPLE_NAME = "createComposeCleanupRule"
    }
}
