package com.stripe.android.migration

/**
 * Base interface for all migration rules
 */
internal sealed interface MigrationRule {
    val description: String
    fun matchesImport(importPath: String): Boolean
    fun getReplacementImport(importPath: String): String?
    fun matchesExpression(text: String): Boolean
    fun replaceInExpression(text: String): Pair<String, String>? // Returns (newText, importToAdd)
}

/**
 * For migrating classes from one package to another
 * Handles both simple classes and nested classes
 * Examples: 
 * - paymentsheet.PaymentSheet -> elements.payment.PaymentSheet
 * - PaymentSheet.CardBrandAcceptance -> CardBrandAcceptance (in elements package)
 */
internal data class ClassMigrationRule(
    val fromPackage: String,           // e.g., "paymentsheet"
    val fromClass: String,             // e.g., "PaymentSheet"
    val toPackage: String,             // e.g., "elements.payment"
    val toClass: String,               // e.g., "PaymentSheet"
    override val description: String
) : MigrationRule {
    
    private val fromFullPath = "com.stripe.android.$fromPackage.$fromClass"
    private val toFullPath = "com.stripe.android.$toPackage.$toClass"
    
    override fun matchesImport(importPath: String): Boolean {
        return importPath == fromFullPath
    }
    
    override fun getReplacementImport(importPath: String): String? {
        return if (matchesImport(importPath)) toFullPath else null
    }
    
    override fun matchesExpression(text: String): Boolean {
        return text.contains(fromClass)
    }

    override fun replaceInExpression(text: String): Pair<String, String>? {
        return if (matchesExpression(text)) {
            Pair(text.replace(fromClass, toClass), toFullPath)
        } else null
    }
}

 