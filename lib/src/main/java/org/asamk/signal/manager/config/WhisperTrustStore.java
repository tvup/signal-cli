package org.asamk.signal.manager.config;

import org.whispersystems.signalservice.api.push.TrustStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

class WhisperTrustStore implements TrustStore {

    private static final String KEYSTORE_PASSWORD = "whisper";
    private static final String CERT_RESOURCE = "torben.crt";

    private static volatile byte[] cachedKeyStoreBytes;

    @Override
    public InputStream getKeyStoreInputStream() {
        try {
            return new ByteArrayInputStream(getKeyStoreBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to build in-memory trust store from " + CERT_RESOURCE, e);
        }
    }

    @Override
    public String getKeyStorePassword() {
        return KEYSTORE_PASSWORD;
    }

    private static byte[] getKeyStoreBytes() throws Exception {
        byte[] cached = cachedKeyStoreBytes;
        if (cached != null) {
            return cached;
        }
        synchronized (WhisperTrustStore.class) {
            if (cachedKeyStoreBytes != null) {
                return cachedKeyStoreBytes;
            }

            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            final X509Certificate cert;
            try (final InputStream certStream = WhisperTrustStore.class.getResourceAsStream(CERT_RESOURCE)) {
                if (certStream == null) {
                    throw new IOException("Resource not found: " + CERT_RESOURCE);
                }
                cert = (X509Certificate) cf.generateCertificate(certStream);
            }

            final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, KEYSTORE_PASSWORD.toCharArray());
            ks.setCertificateEntry("torben-it", cert);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ks.store(baos, KEYSTORE_PASSWORD.toCharArray());
            cachedKeyStoreBytes = baos.toByteArray();
            return cachedKeyStoreBytes;
        }
    }
}
