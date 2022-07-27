# Stripe Android SDK Identity module
This module provides support for the Stripe Identity product, please refer to the  
[documentation](https://www.stripe.com/docs/identity/verify-identity-documents) for more details.

Note: If you intend to use this SDK with Stripe's Identity service, you must not modify this SDK.
Using a modified version of this SDK with Stripe's Identity service, without Stripe's written
authorization, is a breach of your agreement with Stripe and may result in your Stripe account
being shut down.

# Example
See the `identity-example` directory for an example application that you can try for yourself!

# Integration
* In app/build.gradle, add these dependencies:
    ```gradle
    dependencies {
        implementation 'com.stripe:identity:$stripeVersion'
    }
    ```
