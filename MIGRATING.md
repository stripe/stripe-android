## Migration Guides

### Migrating from versions < 7.0.0
- Remove Bitcoin source support because Stripe no longer processes Bitcoin payments: https://stripe.com/blog/ending-bitcoin-support
    - Sources can no longer have a "BITCOIN" source type. These sources will now be interpreted as "UNKNOWN".
    - You can no longer `createBitcoinParams`. Please use a different payment method.

### Migrating from versions < 5.0.0
- StripeApiHandler methods can no longer be called directly.
- PaymentConfiguration now stores your public key and is depended upon for CustomerSession.
- Many *Utils* classes have been migrated to package-private access.

### Migrating from versions < 4.0.0

- Instantiation of a Stripe object can no longer throw an `AuthenticationException`.
    - Any time you were instantiating a Stripe object in a try/catch block will be simplified.
    ```java
    Stripe stripe;
    try {
       stripe = new Stripe(mContext, MY_PUBLISHABLE_KEY);
    } catch (AuthenticationException authEx) {
       // This never happens because you check your key.
    }
    ```
    now becomes
    ```java
    Stripe stripe = new Stripe(mContext, MY_PUBLISHABLE_KEY);
    ```
- `Stripe#setDefaultPublishableKey(String key)` has similarly been changed, and no longer needs to be wrapped.
- Both methods can still throw an `IllegalArgumentException` if an invalid key is used, but as a
runtime exception, that does not need to be wrapped.
- `AuthenticationException` will now only be thrown if you attempt to create a `Token` or `Source`
with an invalid key.
