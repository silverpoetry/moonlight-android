package com.limelight.computers;

import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.PersistedHost;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Persistent host boundary used by service and application policy.
 *
 * <p>The contract exposes only immutable host values. Protocol DTOs and
 * Android storage types belong to adapters outside this boundary.</p>
 */
public interface HostRepository extends AutoCloseable {
    List<PersistedHost> getAllHosts();

    PersistedHost findHost(HostId hostId);

    PersistedHost findHostByName(String advertisedName);

    boolean updateHostMetadata(HostRecord record);

    boolean importHost(PersistedHost host);

    void updatePinnedCertificate(
            HostId hostId,
            X509Certificate certificate);

    void deleteHost(HostId hostId);

    @Override
    void close();
}
