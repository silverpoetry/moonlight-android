package com.limelight.computers.apps;

import com.limelight.computers.model.HostId;

/** Host-scoped persistence port for hidden application IDs. */
public interface HiddenAppRepository {
    HiddenAppSelection load(HostId hostId);

    void save(HostId hostId, HiddenAppSelection selection);

    void delete(HostId hostId);
}
