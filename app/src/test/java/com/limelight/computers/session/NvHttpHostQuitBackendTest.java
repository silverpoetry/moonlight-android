package com.limelight.computers.session;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import com.limelight.nvstream.http.HostHttpResponseException;

import org.junit.Test;

public final class NvHttpHostQuitBackendTest {
    @Test
    public void mapsSessionOwnershipResponseToDomainFailure()
            throws Exception {
        HostHttpResponseException response =
                new HostHttpResponseException(599, "owner");
        NvHttpHostQuitBackend backend =
                new NvHttpHostQuitBackend(() -> {
                    throw response;
                });

        try {
            backend.quit();
            fail("Expected ownership failure");
        }
        catch (HostQuitUseCase.NotSessionOwnerException error) {
            assertSame(response, error.getCause());
        }
    }

    @Test
    public void preservesOtherHostResponses() throws Exception {
        HostHttpResponseException response =
                new HostHttpResponseException(503, "busy");
        NvHttpHostQuitBackend backend =
                new NvHttpHostQuitBackend(() -> {
                    throw response;
                });

        try {
            backend.quit();
            fail("Expected host response failure");
        }
        catch (HostHttpResponseException error) {
            assertSame(response, error);
        }
    }
}
