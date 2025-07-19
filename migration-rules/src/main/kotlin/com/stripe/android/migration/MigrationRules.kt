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
 * For migrating simple classes from one package to another
 * Example: paymentsheet.PaymentSheet -> elements.payment.PaymentSheet
 */
internal data class SimpleClassMigration(
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
        return text == fromClass || text == toClass
    }
    
    override fun replaceInExpression(text: String): Pair<String, String>? {
        return if (matchesExpression(text)) {
            Pair(text.replace(fromClass, toClass), toFullPath)
        } else null
    }
}

/**
 * For extracting nested classes to top-level classes
 * Example: PaymentSheet.CardBrandAcceptance -> CardBrandAcceptance (in elements package)
 */
internal data class NestedClassExtractionMigration(
    val fromParentClass: String,       // e.g., "PaymentSheet"
    val fromNestedClass: String,       // e.g., "CardBrandAcceptance"
    val toPackage: String,             // e.g., "elements"
    val toClass: String,               // e.g., "CardBrandAcceptance"
    override val description: String
) : MigrationRule {
    
    private val fromPattern = "$fromParentClass.$fromNestedClass"
    private val toFullPath = "com.stripe.android.$toPackage.$toClass"
    
    override fun matchesImport(importPath: String): Boolean {
        return importPath.endsWith(fromPattern)
    }
    
    override fun getReplacementImport(importPath: String): String? {
        return if (matchesImport(importPath)) toFullPath else null
    }
    
    override fun matchesExpression(text: String): Boolean {
        return text.contains(fromPattern)
    }
    
    override fun replaceInExpression(text: String): Pair<String, String>? {
        return if (matchesExpression(text)) {
            Pair(text.replace(fromPattern, toClass), toFullPath)
        } else null
    }
}

/**
 * For moving nested classes to a different package as top-level classes
 * Example: PaymentSheet.IntentConfiguration -> IntentConfiguration (in elements.payment package)
 */
internal data class NestedClassToPackageMigration(
    val fromParentClass: String,       // e.g., "PaymentSheet"
    val fromNestedClass: String,       // e.g., "IntentConfiguration" 
    val toPackage: String,             // e.g., "elements.payment"
    val toClass: String,               // e.g., "IntentConfiguration"
    override val description: String
) : MigrationRule {
    
    private val fromPattern = "$fromParentClass.$fromNestedClass"
    private val toFullPath = "com.stripe.android.$toPackage.$toClass"
    
    override fun matchesImport(importPath: String): Boolean {
        return importPath.endsWith(fromPattern)
    }
    
    override fun getReplacementImport(importPath: String): String? {
        return if (matchesImport(importPath)) toFullPath else null
    }
    
    override fun matchesExpression(text: String): Boolean {
        return text.contains(fromPattern)
    }
    
    override fun replaceInExpression(text: String): Pair<String, String>? {
        return if (matchesExpression(text)) {
            Pair(text.replace(fromPattern, toClass), toFullPath)
        } else null
    }
}

/**
 * For reorganizing nested classes under a different parent
 * Example: PaymentSheet.Colors -> Appearance.Colors (in elements.payment package)
 */
internal data class NestedClassReorganizationMigration(
    val fromParentClass: String,       // e.g., "PaymentSheet"
    val fromNestedClass: String,       // e.g., "Colors"
    val toPackage: String,             // e.g., "elements.payment"
    val toParentClass: String,         // e.g., "Appearance"
    val toNestedClass: String,         // e.g., "Colors"
    override val description: String
) : MigrationRule {
    
    private val fromPattern = "$fromParentClass.$fromNestedClass"
    private val toFullPath = "com.stripe.android.$toPackage.$toParentClass"
    private val toPattern = "$toParentClass.$toNestedClass"
    
    override fun matchesImport(importPath: String): Boolean {
        return importPath.endsWith(fromPattern)
    }
    
    override fun getReplacementImport(importPath: String): String? {
        return if (matchesImport(importPath)) "$toFullPath.$toNestedClass" else null
    }
    
    override fun matchesExpression(text: String): Boolean {
        return text.contains(fromPattern)
    }
    
    override fun replaceInExpression(text: String): Pair<String, String>? {
        return if (matchesExpression(text)) {
            Pair(text.replace(fromPattern, toPattern), toFullPath)
        } else null
    }
} 