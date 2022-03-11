package com.stripe.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtImportDirective

class ConnectionsCodeLeakRule(config: Config = Config.empty) : Rule(config) {

    private val KtImportDirective.import: String?
        get() = importPath?.pathStr

    override val issue = Issue(
        id = "ConnectionsCodeLeak",
        description = "Accessing :connections code, added as compileOnly." +
            " Ensure it's safely accessed at runtime and annotate it with @SafeCompileOnlyAccess",
        severity = Severity.Defect,
        debt = Debt.FIVE_MINS
    )

    override fun visitImportDirective(importDirective: KtImportDirective) {
        if (shouldReport(importDirective)) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(importDirective),
                    message = "Importing '${importDirective.import}' from :connections." +
                        "Ensure it's safely accessed at runtime and annotate it with @SafeCompileOnlyAccess"
                )
            )
        }
    }

    private fun shouldReport(importDirective: KtImportDirective): Boolean {
        return importDirective.import.containsConnectionsPackagePath()
    }

    private fun String?.containsConnectionsPackagePath() =
        this?.contains(CONNECTIONS_PACKAGE_PATH) == true

    companion object {
        const val CONNECTIONS_PACKAGE_PATH = "com.stripe.android.connections"
    }
}