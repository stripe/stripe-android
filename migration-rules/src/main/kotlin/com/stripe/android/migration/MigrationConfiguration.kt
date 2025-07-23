package com.stripe.android.migration

/**
 * Configuration containing all the migration rules to be applied.
 * 
 * This file should be updated when adding new migrations in future PRs.
 * Each migration represents one example of the migration patterns supported.
 */
internal object MigrationConfiguration {
    
    /**
     * All migration rules to be applied by the ClassMigration detekt rule.
     * 
     * To add new migrations, simply add entries to this list following the existing patterns.
     */
    val allMigrationRules: List<MigrationRule> = listOf(

        ClassMigrationRule(
            fromPackage = "paymentsheet",
            fromClass = "PaymentSheet.Appearance.Embedded.RowStyle",
            toPackage = "elements",
            toClass = "Appearance.Embedded.RowStyle",
            description = "RowStyle moved from paymentsheet to elements package"
        ),

        ClassMigrationRule(
            fromPackage = "paymentsheet",
            fromClass = "PaymentSheet.CardBrandAcceptance",
            toPackage = "elements",
            toClass = "CardBrandAcceptance",
            description = "CardBrandAcceptance moved from paymentsheet to elements package"
        )
    )
} 