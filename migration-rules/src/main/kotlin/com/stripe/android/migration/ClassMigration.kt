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
    
    /**
     * Get all migration rules from the configuration.
     */
    private val migrationRules: List<MigrationRule> = MigrationConfiguration.allMigrationRules

    private val pendingReplacePair: MutableList<Pair<KtElement, KtElement>> = mutableListOf()

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        
        val importPath = importDirective.importPath?.pathStr ?: return
        
        // Check each migration rule
        migrationRules.forEach { rule ->
            if (rule.matchesImport(importPath)) {
                val replacement = rule.getReplacementImport(importPath)
                if (replacement != null) {
                    if (autoCorrect) {
                        // Create new import directive
                        val factory = KtPsiFactory(importDirective.project)
                        val newImportPath = ImportPath.fromString(replacement)
                        val newImport = factory.createImportDirective(newImportPath)
                        importDirective.replace(newImport)
                    } else {
                        report(
                            CodeSmell(
                                issue,
                                Entity.from(importDirective),
                                "${rule.description}. Replace import '$importPath' with '$replacement'"
                            )
                        )
                    }
                }
                return@forEach
            }
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val fullText = expression.text
//        println("Visiting DotQualifiedExpression: $fullText")
        
        // Check each migration rule
        migrationRules.forEach { rule ->
            val replacement = rule.replaceInExpression(fullText)
            if (replacement != null) {
                val (newText, importToAdd) = replacement
                println("replacing $fullText with $newText")
                if (autoCorrect) {
                    // Get file context BEFORE replace operation
                    val ktFile = expression.containingKtFile
                    // Add import using the file we got earlier
                    addImportSafely(ktFile, importToAdd)

                    // Replace the expression
                    val factory = KtPsiFactory(expression.project)
                    val newExpression = factory.createExpression(newText)
                    expression.replace(newExpression)
                } else {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(expression),
                            "${rule.description}. Replace '$fullText' with '$newText' and add import '$importToAdd'"
                        )
                    )
                }
                return@forEach
            }
        }
    }

    override fun visitTypeReference(typeReference: KtTypeReference) {
        super.visitTypeReference(typeReference)

        val fullText = typeReference.text
//        println("from: ${typeReference.parent}")
//        for (child in typeReference.parent.children) {
//            println("${child.text}, $child")
//        }
        println("Visiting TypeReference: $fullText")

        migrationRules.forEach { rule ->
            val replacement = rule.replaceInExpression(fullText)
            if (replacement != null) {
                val (newText, importToAdd) = replacement
                println("replacing $fullText with $newText")
                if (autoCorrect) {
                    // Get file context BEFORE replace operation
                    val ktFile = typeReference.containingKtFile
                    // Add import BEFORE replace to avoid DummyHolder issue
                    addImportSafely(ktFile, importToAdd)

                    // Replace the type reference
                    typeReference.let {
                        val factory = KtPsiFactory(typeReference.project)
                        val newTypeReference = factory.createType(newText)

                        pendingReplacePair.add(it to newTypeReference)
                    }
                } else {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(typeReference),
                            "${rule.description}. Replace type '$fullText' with '$newText' and add import '$importToAdd'"
                        )
                    )
                }
            }
        }
    }

//    override fun visitElement(element: PsiElement) {
//        super.visitElement(element)
//        println("$element, ${element.text}")
//    }

    override fun visitKtFile(file: KtFile) {
        pendingReplacePair.clear()
        super.visitKtFile(file)
        withAutoCorrect {
            pendingReplacePair.forEach { (oldElement, newElement) ->
                oldElement.replace(newElement)
            }
        }
    }

    private fun addImportSafely(ktFile: KtFile, importPath: String) {
        val existingImports = ktFile.importDirectives.mapNotNull { it.importPath?.pathStr }

        // Don't add if import already exists
        if (existingImports.contains(importPath)) {
            return
        }

        // Don't add if there are any existing imports that would be replaced by this same import
        // This handles the case where we're processing expressions after import replacement
        val hasConflictingImport = migrationRules.any { rule ->
            existingImports.any { existing ->
                rule.matchesImport(existing) && rule.getReplacementImport(existing) == importPath
            }
        }

        if (hasConflictingImport) {
            return
        }

        val factory = KtPsiFactory(ktFile.project)
        val importDirective = factory.createImportDirective(ImportPath.fromString(importPath))

        val importList = ktFile.importList
        importList?.apply {
            add(factory.createNewLine(1))
            add(importDirective)
        }
    }
}