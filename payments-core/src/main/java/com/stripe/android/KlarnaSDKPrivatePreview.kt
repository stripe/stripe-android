//
//  KlarnaSDKPrivatePreview.kt
//  Stripe
//
//  Created by George Birch on 8/17/26.
//  Copyright © 2026 Stripe, Inc. All rights reserved.
//

package com.stripe.android

/**
 * Marks Klarna SDK interoperability APIs as being in private preview. These APIs may change
 * without notice and are not yet covered by semantic-versioning guarantees.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Klarna SDK interoperability is in private preview and may change without notice."
)
@Retention(AnnotationRetention.BINARY)
annotation class KlarnaSDKPrivatePreview
