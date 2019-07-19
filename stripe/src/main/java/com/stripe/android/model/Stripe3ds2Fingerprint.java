package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Stripe3ds2Fingerprint {
    private static final String FIELD_THREE_D_SECURE_2_SOURCE = "three_d_secure_2_source";
    private static final String FIELD_DIRECTORY_SERVER_NAME = "directory_server_name";
    private static final String FIELD_SERVER_TRANSACTION_ID = "server_transaction_id";
    private static final String FIELD_DIRECTORY_SERVER_ENCRYPTION = "directory_server_encryption";

    @NonNull public final String source;
    @NonNull public final DirectoryServer directoryServer;
    @NonNull public final String serverTransactionId;
    @NonNull public final DirectoryServerEncryption directoryServerEncryption;

    @NonNull
    public static Stripe3ds2Fingerprint create(@NonNull StripeIntent.SdkData sdkData)
            throws CertificateException {
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

    public static final class DirectoryServerEncryption {
        private static final String FIELD_DIRECTORY_SERVER_ID = "directory_server_id";
        private static final String FIELD_CERTIFICATE = "certificate";
        private static final String FIELD_KEY_ID = "key_id";
        private static final String FIELD_ROOT_CAS = "root_certificate_authorities";

        @NonNull public final String directoryServerId;
        @NonNull public final PublicKey directoryServerPublicKey;
        @NonNull public final List<X509Certificate> rootCerts;
        @Nullable public final String keyId;

        @VisibleForTesting
        DirectoryServerEncryption(@NonNull String directoryServerId,
                                  @NonNull String dsCertificateData,
                                  @NonNull List<String> rootCertsData,
                                  @Nullable String keyId) throws CertificateException {
            this.directoryServerId = directoryServerId;
            this.directoryServerPublicKey = generateCertificate(dsCertificateData).getPublicKey();
            this.keyId = keyId;
            this.rootCerts = generateCertificates(rootCertsData);
        }

        @NonNull
        static DirectoryServerEncryption create(@NonNull Map<String, ?> data)
                throws CertificateException {
            final List<String> rootCertData;
            if (data.containsKey(FIELD_ROOT_CAS)) {
                rootCertData = (List<String>) Objects.requireNonNull(data.get(FIELD_ROOT_CAS));
            } else {
                rootCertData = Collections.emptyList();
            }
            return new DirectoryServerEncryption(
                    Objects.requireNonNull((String) data.get(FIELD_DIRECTORY_SERVER_ID)),
                    Objects.requireNonNull((String) data.get(FIELD_CERTIFICATE)),
                    rootCertData,
                    (String) data.get(FIELD_KEY_ID));
        }

        @NonNull
        private List<X509Certificate> generateCertificates(@NonNull List<String> certificatesData)
                throws CertificateException {
            final List<X509Certificate> certs = new ArrayList<>(certificatesData.size());
            for (final String certData : certificatesData) {
                certs.add(generateCertificate(certData));
            }
            return certs;
        }

        @NonNull
        private X509Certificate generateCertificate(@NonNull String certificate)
                throws CertificateException {
            final CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate)
                    factory.generateCertificate(new ByteArrayInputStream(certificate.getBytes()));
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
