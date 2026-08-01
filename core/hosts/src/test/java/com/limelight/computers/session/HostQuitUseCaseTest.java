package com.limelight.computers.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;

public final class HostQuitUseCaseTest {
    private final HostQuitUseCase useCase = new HostQuitUseCase();

    @Test
    public void successfulQuitIsReported() throws Exception {
        assertEquals(
                HostQuitUseCase.Outcome.QUIT,
                useCase.execute(() -> true));
    }

    @Test
    public void rejectedQuitIsReported() throws Exception {
        assertEquals(
                HostQuitUseCase.Outcome.REJECTED,
                useCase.execute(() -> false));
    }

    @Test
    public void ownershipFailureBecomesDomainOutcome()
            throws Exception {
        assertEquals(
                HostQuitUseCase.Outcome.NOT_SESSION_OWNER,
                useCase.execute(() -> {
                    throw new HostQuitUseCase
                            .NotSessionOwnerException(null);
                }));
    }

    @Test
    public void unexpectedFailureIsPreserved() throws Exception {
        IOException failure = new IOException("network");
        try {
            useCase.execute(() -> {
                throw failure;
            });
            fail("Expected backend failure");
        }
        catch (IOException error) {
            assertSame(failure, error);
        }
    }
}
