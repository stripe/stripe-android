package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public final class Stripe3ds2Fingerprint {
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_THREE_D_SECURE_2_SOURCE = "three_d_secure_2_source";
    private static final String FIELD_DIRECTORY_SERVER_NAME = "directory_server_name";
    private static final String FIELD_SERVER_TRANSACTION_ID = "server_transaction_id";
    private static final String FIELD_DIRECTORY_SERVER_ENCRYPTION = "directory_server_encryption";

    private static final String TYPE = "stripe_3ds2_fingerprint";

    @NonNull public final String source;
    @NonNull public final DirectoryServer directoryServer;
    @NonNull public final String serverTransactionId;
    @Nullable public final DirectoryServerEncryption directoryServerEncryption;

    @NonNull
    public static Stripe3ds2Fingerprint create(@NonNull StripeIntent.SdkData sdkData) {
        if (!sdkData.is3ds2()) {
            throw new IllegalArgumentException(
                    "Expected SdkData with type='stripe_3ds2_fingerprint'.");
        }

        return new Stripe3ds2Fingerprint(
                (String) sdkData.data.get(FIELD_THREE_D_SECURE_2_SOURCE),
                DirectoryServer.lookup((String) sdkData.data.get(FIELD_DIRECTORY_SERVER_NAME)),
                (String) sdkData.data.get(FIELD_SERVER_TRANSACTION_ID),
                DirectoryServerEncryption.create(
                        (Map<String, ?>) sdkData.data.get(FIELD_DIRECTORY_SERVER_ENCRYPTION))
        );
    }

    @NonNull
    static Stripe3ds2Fingerprint create(@NonNull JSONObject json) throws JSONException {
        final String type = json.optString(FIELD_TYPE);
        if (!TYPE.equals(type)) {
            throw new IllegalArgumentException(
                    "Expected JSON with type='stripe_3ds2_fingerprint'. " +
                            "Received type='" + type + "'");
        }

        final String source = json.getString(FIELD_THREE_D_SECURE_2_SOURCE);
        final DirectoryServer directoryServer =
                DirectoryServer.lookup(json.getString(FIELD_DIRECTORY_SERVER_NAME));
        final String serverTransactionId = json.getString(FIELD_SERVER_TRANSACTION_ID);
        final DirectoryServerEncryption directoryServerEncryption =
                DirectoryServerEncryption.create(
                        json.optJSONObject(FIELD_DIRECTORY_SERVER_ENCRYPTION));
        return new Stripe3ds2Fingerprint(source, directoryServer, serverTransactionId,
                directoryServerEncryption);
    }

    private Stripe3ds2Fingerprint(@NonNull String source,
                                  @NonNull DirectoryServer directoryServer,
                                  @NonNull String serverTransactionId,
                                  @Nullable DirectoryServerEncryption directoryServerEncryption) {
        this.source = source;
        this.directoryServer = directoryServer;
        this.serverTransactionId = serverTransactionId;
        this.directoryServerEncryption = directoryServerEncryption;
    }

    public static class DirectoryServerEncryption {
        private static final String FIELD_DIRECTORY_SERVER_ID = "directory_server_id";
        private static final String FIELD_ALGORITHM = "algorithm";
        private static final String FIELD_PUBLIC_KEY = "public_key";
        private static final String FIELD_KEY_ID = "key_id";

        @NonNull public final String directoryServerId;
        @NonNull public final String algorithm;
        @NonNull public final String publicKey;
        @Nullable public final String keyId;

        private DirectoryServerEncryption(@NonNull String directoryServerId,
                                          @NonNull String algorithm,
                                          @NonNull String publicKey,
                                          @Nullable String keyId) {
            this.directoryServerId = directoryServerId;
            this.algorithm = algorithm;
            this.publicKey = publicKey;
            this.keyId = keyId;
        }

        @Nullable
        static DirectoryServerEncryption create(@Nullable Map<String, ?> data) {
            if (data == null) {
                return null;
            }
            return new DirectoryServerEncryption((String) data.get(FIELD_DIRECTORY_SERVER_ID),
                    (String) data.get(FIELD_ALGORITHM), (String) data.get(FIELD_PUBLIC_KEY),
                    (String) data.get(FIELD_KEY_ID));
        }

        @Nullable
        static DirectoryServerEncryption create(@Nullable JSONObject json) throws JSONException {
            if (json == null) {
                return null;
            }
            return new DirectoryServerEncryption(json.getString(FIELD_DIRECTORY_SERVER_ID),
                    json.getString(FIELD_ALGORITHM), json.getString(FIELD_PUBLIC_KEY),
                    json.optString(FIELD_KEY_ID, null));
        }


    }

    public enum DirectoryServer {
        Visa("visa", "A000000003"),
        Mastercard("mastercard", "A000000004"),
        Amex("american_express", "A000000025");

        @NonNull public final String name;
        @NonNull public final String id;

        DirectoryServer(@NonNull String name, @NonNull String id) {
            this.name = name;
            this.id = id;
        }

        @NonNull
        static DirectoryServer lookup(@NonNull String name) {
            for (DirectoryServer directoryServer : values()) {
                if (directoryServer.name.equals(name)) {
                    return directoryServer;
                }
            }

            throw new IllegalArgumentException("Invalid directory server name: '" + name + "'");
        }
    }
}
