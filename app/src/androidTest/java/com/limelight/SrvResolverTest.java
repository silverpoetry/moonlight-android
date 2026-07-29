package com.limelight;

import org.json.JSONArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class SrvResolverTest {
    private static final String RECORD_NAME =
            "_limelightax._tcp.example.com";

    @Test
    public void parsesMatchingSrvRecordAndNormalizesTrailingDots()
            throws Exception {
        JSONArray answers = new JSONArray(
                "[{\"name\":\"_limelightax._tcp.example.com.\"," +
                        "\"type\":33," +
                        "\"data\":\"0 6 47989 host.example.com.\"}]");

        assertEquals("host.example.com:47989",
                SrvResolver.processSRVRecords(answers, RECORD_NAME));
    }

    @Test
    public void rejectsUnrelatedSrvRecord() throws Exception {
        JSONArray answers = new JSONArray(
                "[{\"name\":\"_other._tcp.example.com\"," +
                        "\"type\":33," +
                        "\"data\":\"0 6 47989 host.example.com\"}]");

        assertNull(SrvResolver.processSRVRecords(answers, RECORD_NAME));
    }
}
