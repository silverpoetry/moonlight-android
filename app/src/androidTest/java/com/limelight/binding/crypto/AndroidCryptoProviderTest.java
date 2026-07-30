package com.limelight.binding.crypto;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class AndroidCryptoProviderTest {
    private File testDirectory;
    private Context isolatedContext;

    @Before
    public void setUp() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        testDirectory = new File(targetContext.getCacheDir(), "crypto-provider-test");
        deleteRecursively(testDirectory);
        assertTrue(testDirectory.mkdirs());
        isolatedContext = new ContextWrapper(targetContext) {
            @Override
            public File getFilesDir() {
                return testDirectory;
            }
        };
    }

    @After
    public void tearDown() {
        deleteRecursively(testDirectory);
    }

    @Test
    public void generatedIdentityCanBeReloadedAndUsedForPairingSignatures() throws Exception {
        AndroidCryptoProvider provider = new AndroidCryptoProvider(isolatedContext);
        X509Certificate certificate = provider.getClientCertificate();
        PrivateKey privateKey = provider.getClientPrivateKey();
        byte[] pemCertificate = provider.getPemEncodedClientCertificate();

        assertNotNull(certificate);
        assertNotNull(privateKey);
        assertNotNull(pemCertificate);
        assertEquals("RSA", certificate.getPublicKey().getAlgorithm());
        assertEquals("RSA", privateKey.getAlgorithm());
        certificate.verify(certificate.getPublicKey());

        String pemText = new String(pemCertificate, StandardCharsets.US_ASCII);
        assertTrue(pemText.startsWith("-----BEGIN CERTIFICATE-----\n"));
        assertFalse(pemText.contains("\r"));

        X509Certificate storedCertificate = (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pemCertificate));
        PrivateKey storedKey = KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(readFile(new File(testDirectory, "client.key"))));
        assertArrayEquals(certificate.getEncoded(), storedCertificate.getEncoded());
        assertArrayEquals(privateKey.getEncoded(), storedKey.getEncoded());

        byte[] challenge = "moonlight-pairing-challenge"
                .getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(storedKey);
        signer.update(challenge);
        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(storedCertificate);
        verifier.update(challenge);
        assertTrue(verifier.verify(signature));

        AndroidCryptoProvider reloadedProvider =
                new AndroidCryptoProvider(isolatedContext);
        assertArrayEquals(certificate.getEncoded(),
                reloadedProvider.getClientCertificate().getEncoded());
        assertArrayEquals(privateKey.getEncoded(),
                reloadedProvider.getClientPrivateKey().getEncoded());
    }

    private static byte[] readFile(File file) throws Exception {
        java.io.FileInputStream input = new java.io.FileInputStream(file);
        try {
            byte[] data = new byte[(int) file.length()];
            int offset = 0;
            while (offset < data.length) {
                int read = input.read(data, offset, data.length - offset);
                if (read < 0) {
                    throw new java.io.IOException("Unexpected end of file");
                }
                offset += read;
            }
            return data;
        }
        finally {
            input.close();
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        assertTrue(file.delete());
    }
}
