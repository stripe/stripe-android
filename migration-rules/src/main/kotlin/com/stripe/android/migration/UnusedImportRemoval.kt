package com.stripe.android.migration

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

/**
 * Detekt rule that automatically removes unused imports.
 * 
 * This rule identifies imports that are not referenced anywhere in the file
 * and removes them when autoCorrect is enabled.
 */
internal class UnusedImportRemoval(config: Config = Config.empty) : Rule(config) {
    
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Unused imports should be removed",
        Debt.FIVE_MINS
    )

    override val autoCorrect: Boolean = true
    
    private val importsToRemove = mutableSetOf<KtImportDirective>()

    override fun visitKtFile(file: KtFile) {
        importsToRemove.clear()
        super.visitKtFile(file)
        
        withAutoCorrect {
            importsToRemove.forEach { import ->
                import.delete()
            }
        }
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        
        val importPath = importDirective.importPath?.pathStr ?: return
        val importedName = importDirective.importPath?.importedName?.asString() ?: return
        
        // Only process Stripe Android imports
        if (!importPath.startsWith("com.stripe.android")) {
            return
        }
        
        // Skip star imports and imports with aliases for now
        if (importDirective.isAllUnder || importDirective.alias != null) {
            return
        }
        
        val file = importDirective.containingKtFile
        if (!isImportUsed(file, importedName, importPath)) {
            if (autoCorrect) {
                importsToRemove.add(importDirective)
            } else {
                report(CodeSmell(
                    issue,
                    Entity.from(importDirective),
                    "Unused Stripe import: $importPath"
                ))
            }
        }
    }

    private fun isImportUsed(file: KtFile, importedName: String, importPath: String): Boolean {
        // Check if the imported name is used anywhere in the file (excluding import statements)
        val nameReferences = file.collectDescendantsOfType<KtNameReferenceExpression>()
            .filter { !isInsideImportDirective(it) }
        val typeReferences = file.collectDescendantsOfType<KtUserType>()
            .filter { !isInsideImportDirective(it) }
        
        // Check simple name references
        val hasNameReference = nameReferences.any { reference ->
            reference.getReferencedName() == importedName
        }
        
        // Check type references
        val hasTypeReference = typeReferences.any { type ->
            type.referencedName == importedName
        }
        
        // Check qualified expressions (e.g., ClassName.method()) but exclude import statements
        val qualifiedExpressions = file.collectDescendantsOfType<KtDotQualifiedExpression>()
            .filter { !isInsideImportDirective(it) }
        val hasQualifiedReference = qualifiedExpressions.any { expr ->
            expr.text.contains(importedName)
        }
        
        // Check function calls
        val callExpressions = file.collectDescendantsOfType<KtCallExpression>()
            .filter { !isInsideImportDirective(it) }
        val hasCallReference = callExpressions.any { call ->
            call.calleeExpression?.text == importedName
        }
        
        return hasNameReference || hasTypeReference || hasQualifiedReference || hasCallReference
    }
    
    private fun isInsideImportDirective(element: org.jetbrains.kotlin.psi.KtElement): Boolean {
        var parent = element.parent
        while (parent != null) {
            if (parent is KtImportDirective) {
                return true
            }
            parent = parent.parent
        }
        return false
    }
}