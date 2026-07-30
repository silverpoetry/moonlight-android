package com.limelight.binding.crypto;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESLightEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class BouncyCastleCompatibilityTest {
    @Test
    public void generatesAndParsesPairingCertificate() throws Exception {
        Provider provider = new BouncyCastleProvider();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        X500Name name = new X500Name("CN=NVIDIA GameStream Client");
        Date notBefore = new Date(1_700_000_000_000L);
        Date notAfter = new Date(2_300_000_000_000L);
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                name,
                BigInteger.valueOf(42),
                notBefore,
                notAfter,
                name,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(provider)
                .build(keyPair.getPrivate());
        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider(provider)
                .getCertificate(builder.build(signer));

        certificate.verify(keyPair.getPublic());
        certificate.checkValidity(new Date(2_000_000_000_000L));
        assertEquals("RSA", certificate.getPublicKey().getAlgorithm());

        StringWriter output = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(output)) {
            writer.writeObject(certificate);
        }
        X509Certificate parsed = (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(
                        output.toString().getBytes(StandardCharsets.US_ASCII)));
        assertArrayEquals(certificate.getEncoded(), parsed.getEncoded());
    }

    @Test
    public void pairingAesPrimitiveMatchesKnownVector() {
        byte[] key = decodeHex("000102030405060708090a0b0c0d0e0f");
        byte[] plaintext = decodeHex("00112233445566778899aabbccddeeff");
        byte[] expected = decodeHex("69c4e0d86a7b0430d8cdb78070b4c55a");

        BlockCipher encryptor = new AESLightEngine();
        encryptor.init(true, new KeyParameter(key));
        byte[] encrypted = new byte[encryptor.getBlockSize()];
        assertEquals(encrypted.length,
                encryptor.processBlock(plaintext, 0, encrypted, 0));
        assertArrayEquals(expected, encrypted);

        BlockCipher decryptor = new AESLightEngine();
        decryptor.init(false, new KeyParameter(key));
        byte[] decrypted = new byte[decryptor.getBlockSize()];
        assertEquals(decrypted.length,
                decryptor.processBlock(encrypted, 0, decrypted, 0));
        assertArrayEquals(plaintext, decrypted);
        assertTrue(decryptor.getAlgorithmName().startsWith("AES"));
    }

    private static byte[] decodeHex(String value) {
        byte[] decoded = new byte[value.length() / 2];
        for (int i = 0; i < decoded.length; i++) {
            int offset = i * 2;
            decoded[i] = (byte) Integer.parseInt(value.substring(offset, offset + 2), 16);
        }
        return decoded;
    }
}
