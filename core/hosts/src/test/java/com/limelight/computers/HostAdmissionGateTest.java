package com.limelight.computers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HostAdmissionGateTest {
    @Test
    public void serializesAdmissionUntilOwnerReleases() throws Exception {
        HostAdmissionGate gate = new HostAdmissionGate();
        HostAdmissionGate.Lease first = gate.acquire();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean acquired = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            started.countDown();
            try (HostAdmissionGate.Lease ignored = gate.acquire()) {
                acquired.set(true);
            }
            catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        });

        waiter.start();
        assertTrue(started.await(1, TimeUnit.SECONDS));
        assertFalse(acquired.get());
        first.close();
        waiter.join(1000L);

        assertFalse(waiter.isAlive());
        assertTrue(acquired.get());
        gate.close();
    }

    @Test
    public void closeInterruptsOwnerAndRejectsWaiters() throws Exception {
        HostAdmissionGate gate = new HostAdmissionGate();
        CountDownLatch ownerAcquired = new CountDownLatch(1);
        AtomicBoolean ownerInterrupted = new AtomicBoolean();
        Thread owner = new Thread(() -> {
            try (HostAdmissionGate.Lease ignored = gate.acquire()) {
                ownerAcquired.countDown();
                try {
                    Thread.sleep(5000L);
                }
                catch (InterruptedException error) {
                    ownerInterrupted.set(true);
                }
            }
            catch (InterruptedException error) {
                ownerInterrupted.set(true);
            }
        });
        CountDownLatch waiterRejected = new CountDownLatch(1);
        Thread waiter = new Thread(() -> {
            try (HostAdmissionGate.Lease ignored = gate.acquire()) {
                fail("Closed gate granted a queued admission");
            }
            catch (IllegalStateException expected) {
                waiterRejected.countDown();
            }
            catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        });

        owner.start();
        assertTrue(ownerAcquired.await(1, TimeUnit.SECONDS));
        waiter.start();
        gate.close();

        owner.join(1000L);
        waiter.join(1000L);
        assertFalse(owner.isAlive());
        assertFalse(waiter.isAlive());
        assertTrue(ownerInterrupted.get());
        assertTrue(waiterRejected.await(1, TimeUnit.SECONDS));

        try {
            gate.acquire();
            fail("Closed gate accepted new work");
        }
        catch (IllegalStateException expected) {
        }
    }

    @Test
    public void leaseCloseIsIdempotent() throws Exception {
        HostAdmissionGate gate = new HostAdmissionGate();
        HostAdmissionGate.Lease lease = gate.acquire();

        lease.close();
        lease.close();
        HostAdmissionGate.Lease replacement = gate.acquire();
        replacement.close();
        gate.close();
    }

    @Test
    public void preInterruptedCallerCannotAcquireAdmission() {
        HostAdmissionGate gate = new HostAdmissionGate();
        Thread.currentThread().interrupt();
        try {
            gate.acquire();
            fail("Interrupted caller acquired admission");
        }
        catch (InterruptedException expected) {
            assertFalse(Thread.currentThread().isInterrupted());
        }
        finally {
            Thread.interrupted();
            gate.close();
        }
    }
}
