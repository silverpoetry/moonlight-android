package com.limelight.computers;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

/** Minimal deterministic certificate value for host-domain unit tests. */
final class TestX509Certificate extends X509Certificate {
    private final String id;

    TestX509Certificate(String id) {
        this.id = id;
    }

    @Override public void checkValidity() { }
    @Override public void checkValidity(Date date) { }
    @Override public int getVersion() { return 3; }
    @Override public BigInteger getSerialNumber() { return BigInteger.ONE; }
    @Override public Principal getIssuerDN() { return () -> id; }
    @Override public Principal getSubjectDN() { return () -> id; }
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
    @Override public byte[] getEncoded() {
        return id.getBytes(StandardCharsets.UTF_8);
    }
    @Override public void verify(PublicKey key) { }
    @Override public void verify(PublicKey key, String sigProvider) { }
    @Override public String toString() { return id; }
    @Override public PublicKey getPublicKey() { return null; }
    @Override public boolean hasUnsupportedCriticalExtension() {
        return false;
    }
    @Override public Set<String> getCriticalExtensionOIDs() { return null; }
    @Override public Set<String> getNonCriticalExtensionOIDs() { return null; }
    @Override public byte[] getExtensionValue(String oid) { return null; }
}
