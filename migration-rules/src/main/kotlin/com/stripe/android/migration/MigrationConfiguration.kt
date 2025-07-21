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

        SimpleClassMigration(
            fromPackage = "paymentsheet",
            fromClass = "PaymentSheet.Appearance.Embedded.RowStyle",
            toPackage = "elements",
            toClass = "Appearance.Embedded.RowStyle",
            description = "RowStyle moved from paymentsheet to elements package"
        ),

        SimpleClassMigration(
            fromPackage = "paymentsheet",
            fromClass = "PaymentSheet.CardBrandAcceptance",
            toPackage = "elements",
            toClass = "CardBrandAcceptance",
            description = "CardBrandAcceptance moved from paymentsheet to elements package"
        ),
        // === Nested Class Extraction Examples ===
//         Moving nested classes from PaymentSheet to top-level classes in elements package
//        NestedClassExtractionMigration(
//            fromParentClass = "PaymentSheet",
//            fromNestedClass = "CardBrandAcceptance",
//            toPackage = "elements",
//            toClass = "CardBrandAcceptance",
//            description = "CardBrandAcceptance moved from PaymentSheet to elements package"
//        ),
//        NestedClassExtractionMigration(
//            fromParentClass = "PaymentSheet",
//            fromNestedClass = "IntentConfiguration",
//            toPackage = "elements.payment",
//            toClass = "IntentConfiguration",
//            description = "IntentConfiguration moved from PaymentSheet to elements.payment package"
//        )

        // === Simple Class Migration Examples ===
        // Moving entire classes from one package to another
//        SimpleClassMigration(
//            fromPackage = "paymentsheet",
//            fromClass = "PaymentSheet",
//            toPackage = "elements.payment",
//            toClass = "PaymentSheet",
//            description = "PaymentSheet moved from paymentsheet to elements.payment package"
//        ),

        // === Nested Class to Package Migration Examples ===
        // Moving nested classes to different packages as top-level classes
//        NestedClassToPackageMigration(
//            fromParentClass = "PaymentSheet",
//            fromNestedClass = "IntentConfiguration",
//            toPackage = "elements.payment",
//            toClass = "IntentConfiguration",
//            description = "IntentConfiguration moved from PaymentSheet to elements.payment package"
//        ),
        
        // === Nested Class Reorganization Examples ===
        // Reorganizing nested classes under different parents
//        NestedClassReorganizationMigration(
//            fromParentClass = "PaymentSheet",
//            fromNestedClass = "Colors",
//            toPackage = "elements.payment",
//            toParentClass = "Appearance",
//            toNestedClass = "Colors",
//            description = "Colors moved from PaymentSheet to Appearance in elements.payment package"
//        )
        
        // === Future Migration Examples (commented out for now) ===
        
        // More nested class extractions:
        // NestedClassExtractionMigration(
        //     fromParentClass = "PaymentSheet",
        //     fromNestedClass = "BillingDetails",
        //     toPackage = "elements",
        //     toClass = "BillingDetails",
        //     description = "BillingDetails moved from PaymentSheet to elements package"
        // ),
        
        // More nested-to-package migrations:
        // NestedClassToPackageMigration(
        //     fromParentClass = "PaymentSheet",
        //     fromNestedClass = "BillingDetailsCollectionConfiguration",
        //     toPackage = "elements.payment",
        //     toClass = "BillingDetailsCollectionConfiguration",
        //     description = "BillingDetailsCollectionConfiguration moved to elements.payment package"
        // ),
        
        // More simple class migrations:
        // SimpleClassMigration(
        //     fromPackage = "paymentsheet",
        //     fromClass = "FlowController",
        //     toPackage = "elements.payment",
        //     toClass = "FlowController",
        //     description = "FlowController moved from paymentsheet to elements.payment package"
        // ),
        
        // More reorganizations:
        // NestedClassReorganizationMigration(
        //     fromParentClass = "PaymentSheet",
        //     fromNestedClass = "Shapes",
        //     toPackage = "elements.payment",
        //     toParentClass = "Appearance",
        //     toNestedClass = "Shapes",
        //     description = "Shapes moved from PaymentSheet to Appearance"
        // )
    )
} 