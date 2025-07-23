package com.stripe.android.migration

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * Detekt rule for migrating classes to new package locations.
 * 
 * This rule applies all migrations defined in MigrationConfiguration.
 * It handles imports, dot-qualified expressions, and type references.
 */
internal class ClassMigration(config: Config = Config.empty) : Rule(config) {
    
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Warning,
        "Classes need to be migrated to new package locations",
        Debt.FIVE_MINS
    )

    override val autoCorrect: Boolean = true
    
    private val migrationRules: List<MigrationRule> = MigrationConfiguration.allMigrationRules
    private val pendingReplacements: MutableList<Pair<KtElement, KtElement>> = mutableListOf()

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        
        val importPath = importDirective.importPath?.pathStr ?: return
        
        migrationRules.forEach { rule ->
            if (rule.matchesImport(importPath)) {
                val replacement = rule.getReplacementImport(importPath) ?: return@forEach
                
                if (autoCorrect) {
                    val factory = KtPsiFactory(importDirective.project)
                    val newImport = factory.createImportDirective(ImportPath.fromString(replacement))
                    importDirective.replace(newImport)
                } else {
                    reportImportIssue(importDirective, rule, importPath, replacement)
                }
                return@forEach
            }
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        handleTextReplacement(expression, expression.text, ::createExpression)
    }

    override fun visitTypeReference(typeReference: KtTypeReference) {
        super.visitTypeReference(typeReference)
        handleTextReplacement(typeReference, typeReference.text, ::createTypeReference)
    }

    override fun visitKtFile(file: KtFile) {
        pendingReplacements.clear()
        super.visitKtFile(file)
        withAutoCorrect {
            pendingReplacements.forEach { (oldElement, newElement) ->
                oldElement.replace(newElement)
            }
        }
    }

    private fun handleTextReplacement(
        element: KtElement,
        text: String,
        createElement: (KtPsiFactory, String) -> KtElement
    ) {
        migrationRules.forEach { rule ->
            val replacement = rule.replaceInExpression(text) ?: return@forEach
            val (newText, importToAdd) = replacement
            
            if (autoCorrect) {
                addImportSafely(element.containingKtFile, importToAdd)
                
                val factory = KtPsiFactory(element.project)
                val newElement = createElement(factory, newText)
                
                // Type references need deferred replacement to avoid AST issues
                if (element is KtTypeReference) {
                    pendingReplacements.add(element to newElement)
                } else {
                    element.replace(newElement)
                }
            } else {
                reportTextIssue(element, rule, text, newText, importToAdd)
            }
            return@forEach
        }
    }

    private fun createExpression(factory: KtPsiFactory, text: String) = factory.createExpression(text)
    private fun createTypeReference(factory: KtPsiFactory, text: String) = factory.createType(text)

    private fun reportImportIssue(
        element: KtImportDirective,
        rule: MigrationRule,
        oldImport: String,
        newImport: String
    ) {
        report(CodeSmell(
            issue,
            Entity.from(element),
            "${rule.description}. Replace import '$oldImport' with '$newImport'"
        ))
    }

    private fun reportTextIssue(
        element: KtElement,
        rule: MigrationRule,
        oldText: String,
        newText: String,
        importToAdd: String
    ) {
        val elementType = if (element is KtTypeReference) "type" else "expression"
        report(CodeSmell(
            issue,
            Entity.from(element),
            "${rule.description}. Replace $elementType '$oldText' with '$newText' and add import '$importToAdd'"
        ))
    }

    private fun addImportSafely(ktFile: KtFile, importPath: String) {
        val existingImports = ktFile.importDirectives.mapNotNull { it.importPath?.pathStr }

        if (existingImports.contains(importPath)) return

        val hasConflictingImport = migrationRules.any { rule ->
            existingImports.any { existing ->
                rule.matchesImport(existing) && rule.getReplacementImport(existing) == importPath
            }
        }

        if (hasConflictingImport) return

        val factory = KtPsiFactory(ktFile.project)
        val importDirective = factory.createImportDirective(ImportPath.fromString(importPath))

        ktFile.importList?.apply {
            add(factory.createNewLine(1))
            add(importDirective)
        }
    }
}
