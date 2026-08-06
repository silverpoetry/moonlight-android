package com.moonlight.buildlogic

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption

/**
 * Serializes independent Gradle invocations that share this worktree.
 *
 * Gradle can safely parallelize tasks inside one invocation, but two separate
 * invocations are allowed to overwrite the same variant intermediates. AGP's
 * lint model tasks are particularly sensitive to that race. The service holds
 * an OS-level lock for the lifetime of the build, while maxParallelUsages is
 * left unlimited so tasks in the same build are not serialized.
 */
abstract class WorkspaceBuildLockService
        implements BuildService<WorkspaceBuildLockService.Parameters>, AutoCloseable {

    interface Parameters extends BuildServiceParameters {
        RegularFileProperty getLockFile()
    }

    private FileChannel channel
    private FileLock lock

    synchronized void acquire() {
        if (lock != null && lock.isValid()) {
            return
        }

        File lockFile = parameters.lockFile.get().asFile
        File parent = lockFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IllegalStateException(
                    "Unable to create Gradle workspace lock directory: " + parent)
        }

        channel = FileChannel.open(
                lockFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE)
        try {
            // Blocking here is intentional: a second build waits for the first
            // build to finish instead of corrupting shared AGP intermediates.
            lock = channel.lock()
        } catch (Throwable failure) {
            close()
            throw failure
        }
    }

    @Override
    synchronized void close() {
        if (lock != null) {
            try {
                lock.release()
            } catch (IOException ignored) {
                // The channel close below still releases the OS lock.
            } finally {
                lock = null
            }
        }
        if (channel != null) {
            try {
                channel.close()
            } catch (IOException ignored) {
                // Nothing else can be done during build-service shutdown.
            } finally {
                channel = null
            }
        }
    }
}
