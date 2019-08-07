package com.stripe.android;

import com.stripe.android.model.Source;

/**
 * An interface representing a callback to be notified about the results of
 * creating or finding a {@link Source}.
 *
 * @deprecated use {@link ApiResultCallback<Source>}
 */
@Deprecated
public interface SourceCallback extends ApiResultCallback<Source> {
}
