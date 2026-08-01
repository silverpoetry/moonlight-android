package com.limelight.computers.pairing;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

final class TestCertificates {
    private TestCertificates() {
    }

    static X509Certificate certificate() {
        return new StubCertificate();
    }

    @SuppressWarnings("deprecation")
    private static final class StubCertificate extends X509Certificate {
        @Override public void checkValidity() {}
        @Override public void checkValidity(Date date) {}
        @Override public int getVersion() { return 3; }
        @Override public BigInteger getSerialNumber() { return BigInteger.ONE; }
        @Override public Principal getIssuerDN() { return () -> "issuer"; }
        @Override public Principal getSubjectDN() { return () -> "subject"; }
        @Override public Date getNotBefore() { return new Date(0); }
        @Override public Date getNotAfter() { return new Date(Long.MAX_VALUE); }
        @Override public byte[] getTBSCertificate() { return new byte[0]; }
        @Override public byte[] getSignature() { return new byte[0]; }
        @Override public String getSigAlgName() { return "none"; }
        @Override public String getSigAlgOID() { return "0"; }
        @Override public byte[] getSigAlgParams() { return null; }
        @Override public boolean[] getIssuerUniqueID() { return null; }
        @Override public boolean[] getSubjectUniqueID() { return null; }
        @Override public boolean[] getKeyUsage() { return null; }
        @Override public int getBasicConstraints() { return -1; }
        @Override public byte[] getEncoded() { return "certificate".getBytes(StandardCharsets.UTF_8); }
        @Override public void verify(PublicKey key) {}
        @Override public void verify(PublicKey key, String provider) {}
        @Override public String toString() { return "test-certificate"; }
        @Override public PublicKey getPublicKey() { return null; }
        @Override public boolean hasUnsupportedCriticalExtension() { return false; }
        @Override public Set<String> getCriticalExtensionOIDs() { return null; }
        @Override public Set<String> getNonCriticalExtensionOIDs() { return null; }
        @Override public byte[] getExtensionValue(String oid) { return null; }
    }
}
