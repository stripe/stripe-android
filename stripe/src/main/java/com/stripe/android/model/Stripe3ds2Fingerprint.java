package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Objects;

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
    @NonNull public final DirectoryServerEncryption directoryServerEncryption;

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

    private Stripe3ds2Fingerprint(@NonNull String source,
                                  @NonNull DirectoryServer directoryServer,
                                  @NonNull String serverTransactionId,
                                  @NonNull DirectoryServerEncryption directoryServerEncryption) {
        this.source = source;
        this.directoryServer = directoryServer;
        this.serverTransactionId = serverTransactionId;
        this.directoryServerEncryption = directoryServerEncryption;
    }

    public static class DirectoryServerEncryption {
        private static final String FIELD_DIRECTORY_SERVER_ID = "directory_server_id";
        private static final String FIELD_ALGORITHM = "algorithm";
        private static final String FIELD_CERTIFICATE = "certificate";
        private static final String FIELD_KEY_ID = "key_id";

        @NonNull public final String directoryServerId;
        @NonNull public final String algorithm;
        @NonNull public final String certificate;
        @Nullable public final String keyId;

        private DirectoryServerEncryption(@NonNull String directoryServerId,
                                          @NonNull String algorithm,
                                          @NonNull String certificate,
                                          @Nullable String keyId) {
            this.directoryServerId = directoryServerId;
            this.algorithm = algorithm;
            this.certificate = certificate;
            this.keyId = keyId;
        }

        @NonNull
        static DirectoryServerEncryption create(@NonNull Map<String, ?> data) {
            return new DirectoryServerEncryption(
                    Objects.requireNonNull((String) data.get(FIELD_DIRECTORY_SERVER_ID)),
                    Objects.requireNonNull((String) data.get(FIELD_ALGORITHM)),
                    Objects.requireNonNull((String) data.get(FIELD_CERTIFICATE)),
                    (String) data.get(FIELD_KEY_ID));
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
