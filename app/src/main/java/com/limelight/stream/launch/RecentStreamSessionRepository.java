package com.limelight.stream.launch;

/** Persistence port for a host's last successfully admitted stream. */
public interface RecentStreamSessionRepository {
    RecentStreamSession find(String hostId);

    void save(String hostId, RecentStreamSession session);
}
