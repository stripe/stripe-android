package com.stripe.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

class CompileOnlyCodeLeakRule(config: Config = Config.empty) : Rule(config) {

    private val KtImportDirective.import: String?
        get() = importPath?.pathStr

    override val issue = Issue(
        id = "CompileOnlyCodeLeak",
        description = "Accessing compileOnly code" +
            "Ensure it's safely accessed at runtime and suppress this warning",
        severity = Severity.Defect,
        debt = Debt.FIVE_MINS
    )

    override fun visitImportDirective(importDirective: KtImportDirective) {
        if (shouldReport(importDirective)) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(importDirective),
                    message = "Importing '${importDirective.import}' from a compileOnly dependency." +
                        "Ensure it's safely accessed at runtime and suppress this warning"
                )
            )
        }
    }

    private fun shouldReport(importDirective: KtImportDirective): Boolean {
        return importDirective.import.containsCompileOnlyPackagePath()
            && importDirective.containingKtFile.isNotCompileOnlyFile()
    }

    private fun KtFile.isNotCompileOnlyFile(): Boolean {
        return packageDirective?.text.containsCompileOnlyPackagePath().not()
    }

    private fun String?.containsCompileOnlyPackagePath() =
        COMPILE_ONLY_PACKAGE_PATHS.any { this?.contains(it) == true }

    companion object {
        private val COMPILE_ONLY_PACKAGE_PATHS = listOf(
            "com.stripe.android.compileonly"
        )
    }
}

