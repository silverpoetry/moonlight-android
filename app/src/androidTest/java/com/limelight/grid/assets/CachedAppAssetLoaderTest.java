package com.limelight.grid.assets;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.limelight.computers.model.HostConnectionState;
import com.limelight.computers.model.HostEndpoint;
import com.limelight.computers.model.HostId;
import com.limelight.computers.model.HostIdentity;
import com.limelight.computers.model.HostRecord;
import com.limelight.computers.model.HostRuntimeSnapshot;
import com.limelight.computers.model.PersistedHost;
import com.limelight.nvstream.http.NvApp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class CachedAppAssetLoaderTest {
    private static final int APP_ID = 73;
    private static final long LOAD_TIMEOUT_MS = 2_000;

    private CachedAppAssetLoader loader;
    private MemoryAssetLoader memoryLoader;
    private File cachedPoster;
    private ImageView imageView;
    private TextView textView;
    private NvApp app;
    private Bitmap noAppImageBitmap;

    @Before
    public void setUp() throws IOException {
        Context context =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        HostRuntimeSnapshot host = host("asset-loader-test");
        app = new NvApp("Cached test app", APP_ID, false);

        memoryLoader = new MemoryAssetLoader();
        memoryLoader.clearCache();
        DiskAssetLoader diskLoader = new DiskAssetLoader(context);
        cachedPoster = diskLoader.getFile(
                host.getRecord().getIdentity().getId().getValue(),
                APP_ID);
        File parent = cachedPoster.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IOException("Unable to create cached poster directory");
        }

        Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        try (FileOutputStream output = new FileOutputStream(cachedPoster)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("Unable to encode cached poster");
            }
        }
        finally {
            bitmap.recycle();
        }

        noAppImageBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        loader = new CachedAppAssetLoader(
                host,
                1.0,
                new NetworkAssetLoader(context, "test"),
                memoryLoader,
                diskLoader,
                noAppImageBitmap);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageView = new ImageView(context);
            textView = new TextView(context);
        });
    }

    @After
    public void tearDown() {
        loader.cancelForegroundLoads();
        loader.cancelBackgroundLoads();
        memoryLoader.clearCache();
        cachedPoster.delete();
        File parent = cachedPoster.getParentFile();
        if (parent != null) {
            parent.delete();
        }
        noAppImageBitmap.recycle();
    }

    @Test
    public void diskHitIsDeliveredOnMainThread() {
        AtomicBoolean returnedSynchronously = new AtomicBoolean(true);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                returnedSynchronously.set(
                        loader.populateImageView(app, imageView, textView)));
        assertFalse(returnedSynchronously.get());

        long deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT_MS;
        AtomicBoolean delivered = new AtomicBoolean();
        while (SystemClock.uptimeMillis() < deadline && !delivered.get()) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    delivered.set(
                            imageView.getDrawable() != null &&
                                    !(imageView.getDrawable() instanceof
                                            CachedAppAssetLoader.AsyncDrawable)));
            if (!delivered.get()) {
                SystemClock.sleep(20);
            }
        }

        assertTrue("Cached poster was not delivered", delivered.get());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertEquals(app.getAppName(), textView.getText().toString());
            assertEquals(View.GONE, textView.getVisibility());
        });
    }

    @Test
    public void saturatedQueueCancelsEvictedTaskAndShowsFallback()
            throws Exception {
        Context context =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        HostRuntimeSnapshot host = host(
                "asset-loader-saturation-test");
        BlockingDiskAssetLoader blockingDiskLoader =
                new BlockingDiskAssetLoader(context, noAppImageBitmap);
        CachedAppAssetLoader saturatedLoader = new CachedAppAssetLoader(
                host,
                1.0,
                new NetworkAssetLoader(context, "test"),
                memoryLoader,
                blockingDiskLoader,
                noAppImageBitmap);
        List<ImageView> imageViews = new ArrayList<>();
        List<TextView> textViews = new ArrayList<>();

        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (int index = 0; index < 3; index++) {
                    queueLoad(
                            context,
                            saturatedLoader,
                            imageViews,
                            textViews,
                            index);
                }
            });
            assertTrue(
                    "Disk workers did not start",
                    blockingDiskLoader.awaitWorkersStarted());

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                // Three tasks are running and the next forty fill the bounded queue.
                // The final request must evict the oldest queued request (index 3).
                for (int index = 3; index < 44; index++) {
                    queueLoad(
                            context,
                            saturatedLoader,
                            imageViews,
                            textViews,
                            index);
                }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                ImageView evictedView = imageViews.get(3);
                assertFalse(
                        "Evicted view retained a permanently pending drawable",
                        evictedView.getDrawable() instanceof
                                CachedAppAssetLoader.AsyncDrawable);
                assertEquals(View.VISIBLE, evictedView.getVisibility());
                assertEquals(View.VISIBLE, textViews.get(3).getVisibility());
            });
        }
        finally {
            saturatedLoader.cancelForegroundLoads();
            blockingDiskLoader.releaseWorkers();
            assertTrue(
                    "Disk workers did not stop",
                    blockingDiskLoader.awaitWorkersStopped());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }

    private static HostRuntimeSnapshot host(String id) {
        HostId hostId = HostId.of(id);
        HostEndpoint endpoint = new HostEndpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "192.0.2.10",
                47989);
        return new HostRuntimeSnapshot(
                new PersistedHost(
                        new HostRecord(
                                new HostIdentity(
                                        hostId,
                                        "Asset host",
                                        null),
                                Collections.singletonList(endpoint),
                                null),
                        null),
                new HostConnectionState(
                        hostId,
                        HostConnectionState.Reachability.ONLINE,
                        HostConnectionState.PairingStatus.PAIRED,
                        endpoint,
                        0,
                        0),
                null,
                false);
    }

    private static void queueLoad(
            Context context,
            CachedAppAssetLoader loader,
            List<ImageView> imageViews,
            List<TextView> textViews,
            int index) {
        ImageView queuedImage = new ImageView(context);
        TextView queuedText = new TextView(context);
        imageViews.add(queuedImage);
        textViews.add(queuedText);
        loader.populateImageView(
                new NvApp("Queued app " + index, 1_000 + index, false),
                queuedImage,
                queuedText);
    }

    private static final class BlockingDiskAssetLoader
            extends DiskAssetLoader {
        private final CountDownLatch workersStarted = new CountDownLatch(3);
        private final CountDownLatch releaseWorkers = new CountDownLatch(1);
        private final CountDownLatch workersStopped = new CountDownLatch(3);
        private final Bitmap resultBitmap;

        BlockingDiskAssetLoader(
                Context context,
                Bitmap resultBitmap) {
            super(context);
            this.resultBitmap = resultBitmap;
        }

        @Override
        public ScaledBitmap loadBitmapFromCache(
                CachedAppAssetLoader.LoaderTuple tuple,
                int sampleSize) {
            workersStarted.countDown();
            try {
                releaseWorkers.await();
                return new ScaledBitmap(16, 16, resultBitmap);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            finally {
                workersStopped.countDown();
            }
        }

        boolean awaitWorkersStarted() throws InterruptedException {
            return workersStarted.await(2, TimeUnit.SECONDS);
        }

        void releaseWorkers() {
            releaseWorkers.countDown();
        }

        boolean awaitWorkersStopped() throws InterruptedException {
            return workersStopped.await(2, TimeUnit.SECONDS);
        }
    }
}
