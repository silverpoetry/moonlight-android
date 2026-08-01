package com.limelight.computers;

import com.limelight.computers.model.HostRuntimeSnapshot;

/** Receives detached immutable host-state publications from the service. */
public interface ComputerManagerListener {
    void notifyComputerUpdated(HostRuntimeSnapshot snapshot);
}
