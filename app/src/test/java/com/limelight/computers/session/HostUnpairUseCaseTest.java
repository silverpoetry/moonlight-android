package com.limelight.computers.session;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public final class HostUnpairUseCaseTest {
    private final HostUnpairUseCase useCase =
            new HostUnpairUseCase();

    @Test
    public void pairedHostMustConfirmUnpairedState()
            throws Exception {
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger unpairs = new AtomicInteger();

        HostUnpairUseCase.Outcome outcome = useCase.execute(
                new HostUnpairUseCase.Backend() {
                    @Override
                    public HostUnpairUseCase.PairingState getState() {
                        return reads.getAndIncrement() == 0
                                ? HostUnpairUseCase.PairingState.PAIRED
                                : HostUnpairUseCase.PairingState.NOT_PAIRED;
                    }

                    @Override
                    public void unpair() {
                        unpairs.incrementAndGet();
                    }
                });

        assertEquals(
                HostUnpairUseCase.Outcome.UNPAIRED,
                outcome);
        assertEquals(1, unpairs.get());
    }

    @Test
    public void alreadyUnpairedHostSkipsMutation() throws Exception {
        AtomicInteger unpairs = new AtomicInteger();
        HostUnpairUseCase.Outcome outcome = useCase.execute(
                new HostUnpairUseCase.Backend() {
                    @Override
                    public HostUnpairUseCase.PairingState getState() {
                        return HostUnpairUseCase.PairingState.NOT_PAIRED;
                    }

                    @Override
                    public void unpair() {
                        unpairs.incrementAndGet();
                    }
                });

        assertEquals(
                HostUnpairUseCase.Outcome.ALREADY_UNPAIRED,
                outcome);
        assertEquals(0, unpairs.get());
    }

    @Test
    public void unchangedPairingStateIsRejected() throws Exception {
        assertEquals(
                HostUnpairUseCase.Outcome.REJECTED,
                useCase.execute(new HostUnpairUseCase.Backend() {
                    @Override
                    public HostUnpairUseCase.PairingState getState() {
                        return HostUnpairUseCase.PairingState.PAIRED;
                    }

                    @Override
                    public void unpair() {
                    }
                }));
    }
}
