package com.stripe.android.migration

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.ImportPath

internal class ClassMigration(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Warning,
        "CardBrandAcceptance needs to be migrated from PaymentSheet to elements package",
        Debt.FIVE_MINS
    )

    override val autoCorrect: Boolean = true

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        
        val importPath = importDirective.importPath?.pathStr ?: return
        
        // Check for PaymentSheet.CardBrandAcceptance import
        if (importPath.endsWith("PaymentSheet.CardBrandAcceptance")) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(importDirective),
                    "Replace import '$importPath' with 'com.stripe.android.elements.CardBrandAcceptance'"
                )
            )
            
            if (autoCorrect) {
                // Create new import directive
                val factory = KtPsiFactory(importDirective.project)
                val newImportPath = ImportPath.fromString("com.stripe.android.elements.CardBrandAcceptance")
                val newImport = factory.createImportDirective(newImportPath)
                importDirective.replace(newImport)
            }
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val fullText = expression.text
        if (fullText.contains("PaymentSheet.CardBrandAcceptance")) {
            val replacementText = fullText.replace("PaymentSheet.CardBrandAcceptance", "CardBrandAcceptance")
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Replace '$fullText' with '$replacementText' and add import 'com.stripe.android.elements.CardBrandAcceptance'"
                )
            )
            
            if (autoCorrect) {
                // Get file context BEFORE replace operation
                val ktFile = expression.containingKtFile
                
                // Replace the expression
                val factory = KtPsiFactory(expression.project)
                val newExpression = factory.createExpression(replacementText)
                // Add import using the file we got earlier
                addImportSafely(ktFile, "com.stripe.android.elements.CardBrandAcceptance")
                expression.replace(newExpression)
            }
        }
    }
    
    override fun visitTypeReference(typeReference: KtTypeReference) {
        super.visitTypeReference(typeReference)
        
        val fullText = typeReference.text
        if (fullText.contains("PaymentSheet.CardBrandAcceptance")) {
            val replacementText = fullText.replace("PaymentSheet.CardBrandAcceptance", "CardBrandAcceptance")
            report(
                CodeSmell(
                    issue,
                    Entity.from(typeReference),
                    "Replace type '$fullText' with '$replacementText' and add import 'com.stripe.android.elements.CardBrandAcceptance'"
                )
            )
            
            if (autoCorrect) {
                // Get file context BEFORE replace operation
                val ktFile = typeReference.containingKtFile
                
                // Replace the type reference
                val factory = KtPsiFactory(typeReference.project)
                val newTypeReference = factory.createType(replacementText)
                
                // Add import BEFORE replace to avoid DummyHolder issue
                addImportSafely(ktFile, "com.stripe.android.elements.CardBrandAcceptance")
                
                typeReference.replace(newTypeReference)
            }
        }
    }
    
    private fun addImportSafely(ktFile: KtFile, importPath: String) {
        val existingImports = ktFile.importDirectives.mapNotNull { it.importPath?.pathStr }
        
        if (!existingImports.contains(importPath)) {
            val factory = KtPsiFactory(ktFile.project)
            val importDirective = factory.createImportDirective(ImportPath.fromString(importPath))
            
            val importList = ktFile.importList
            importList?.apply {
                add(factory.createNewLine(1))
                add(importDirective)
            }
        }
    }
}