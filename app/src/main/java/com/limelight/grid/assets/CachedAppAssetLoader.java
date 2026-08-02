package com.limelight.grid.assets;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CachedAppAssetLoader {
    private static final int MAX_CONCURRENT_DISK_LOADS = 3;
    private static final int MAX_CONCURRENT_NETWORK_LOADS = 3;
    private static final int MAX_CONCURRENT_CACHE_LOADS = 1;

    private static final int MAX_PENDING_CACHE_LOADS = 100;
    private static final int MAX_PENDING_NETWORK_LOADS = 40;
    private static final int MAX_PENDING_DISK_LOADS = 40;
    private static final long IDLE_WORKER_TIMEOUT_SECONDS = 30;
    private static final AtomicInteger NEXT_WORKER_ID = new AtomicInteger();
    private static final RejectedExecutionHandler DISCARD_OLDEST_AND_CANCEL =
            (task, executor) -> {
                if (executor.isShutdown()) {
                    cancelQueuedTask(task, false);
                    return;
                }

                Runnable discardedTask = executor.getQueue().poll();
                cancelQueuedTask(discardedTask, true);

                // A competing producer may have filled the slot we just freed. In that
                // exceptional case, cancel the new task instead of blocking the UI thread.
                if (!executor.getQueue().offer(task)) {
                    cancelQueuedTask(task, true);
                }
                else if (executor.isShutdown() && executor.remove(task)) {
                    cancelQueuedTask(task, false);
                }
            };

    private final ThreadPoolExecutor cacheExecutor = createExecutor(
            "prefetch",
            MAX_CONCURRENT_CACHE_LOADS,
            MAX_PENDING_CACHE_LOADS);

    private final ThreadPoolExecutor foregroundExecutor = createExecutor(
            "disk",
            MAX_CONCURRENT_DISK_LOADS,
            MAX_PENDING_DISK_LOADS);

    private final ThreadPoolExecutor networkExecutor = createExecutor(
            "network",
            MAX_CONCURRENT_NETWORK_LOADS,
            MAX_PENDING_NETWORK_LOADS);

    private final ComputerDetails computer;
    private final double scalingDivider;
    private final NetworkAssetLoader networkLoader;
    private final MemoryAssetLoader memoryLoader;
    private final DiskAssetLoader diskLoader;
    private final Bitmap placeholderBitmap;
    private final Bitmap noAppImageBitmap;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CachedAppAssetLoader(ComputerDetails computer, double scalingDivider,
                                NetworkAssetLoader networkLoader, MemoryAssetLoader memoryLoader,
                                DiskAssetLoader diskLoader, Bitmap noAppImageBitmap) {
        this.computer = computer;
        this.scalingDivider = scalingDivider;
        this.networkLoader = networkLoader;
        this.memoryLoader = memoryLoader;
        this.diskLoader = diskLoader;
        this.noAppImageBitmap = noAppImageBitmap;
        this.placeholderBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    private static ThreadPoolExecutor createExecutor(
            String role,
            int concurrency,
            int pendingCapacity) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                concurrency,
                concurrency,
                IDLE_WORKER_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(pendingCapacity),
                new AssetThreadFactory(role),
                DISCARD_OLDEST_AND_CANCEL);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static final class AssetThreadFactory implements ThreadFactory {
        private final String role;

        AssetThreadFactory(String role) {
            this.role = role;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(
                    runnable,
                    "moonlight-boxart-" + role + "-" +
                            NEXT_WORKER_ID.incrementAndGet());
        }
    }

    public void cancelBackgroundLoads() {
        Runnable r;
        while ((r = cacheExecutor.getQueue().poll()) != null) {
            cancelQueuedTask(r, false);
        }
    }

    public void cancelForegroundLoads() {
        Runnable r;

        while ((r = foregroundExecutor.getQueue().poll()) != null) {
            cancelQueuedTask(r, false);
        }

        while ((r = networkExecutor.getQueue().poll()) != null) {
            cancelQueuedTask(r, false);
        }
    }

    public void freeCacheMemory() {
        memoryLoader.clearCache();
    }

    private static void cancelQueuedTask(
            Runnable task,
            boolean showFallback) {
        if (task instanceof LoaderFutureTask) {
            ((LoaderFutureTask) task).cancelFromQueue(showFallback);
        }
    }

    private ScaledBitmap doNetworkAssetLoad(LoaderTuple tuple, LoaderTask task) {
        // Try 3 times
        for (int i = 0; i < 3; i++) {
            // Check again whether we've been cancelled or the image view is gone
            if (task != null && (task.isCancelled() || task.imageViewRef.get() == null)) {
                return null;
            }

            InputStream in = networkLoader.getBitmapStream(tuple);
            if (in != null) {
                // Write the stream straight to disk
                diskLoader.populateCacheWithStream(tuple, in);

                // Close the network input stream
                try {
                    in.close();
                } catch (IOException ignored) {}

                // If there's a task associated with this load, we should return the bitmap
                if (task != null) {
                    // If the cached bitmap is valid, return it. Otherwise, we'll try the load again
                    ScaledBitmap bmp = diskLoader.loadBitmapFromCache(tuple, (int) scalingDivider);
                    if (bmp != null) {
                        return bmp;
                    }
                }
                else {
                    // Otherwise it's a background load and we return nothing
                    return null;
                }
            }

            // Wait 1 second with a bit of fuzz
            try {
                Thread.sleep((int) (1000 + (Math.random() * 500)));
            } catch (InterruptedException e) {
                LimeLog.warning(
                        "Interrupted while retrying app asset download",
                        e);

                // InterruptedException clears the thread's interrupt status. Since we can't
                // handle that here, we will re-interrupt the thread to set the interrupt
                // status back to true.
                Thread.currentThread().interrupt();

                return null;
            }
        }

        return null;
    }

    private static final class LoaderTask implements Runnable {
        private final CachedAppAssetLoader assetLoader;
        private final WeakReference<ImageView> imageViewRef;
        private final WeakReference<TextView> textViewRef;
        private final boolean diskOnly;

        private volatile LoaderTuple tuple;
        private volatile ThreadPoolExecutor executor;
        private volatile LoaderFutureTask future;
        private volatile boolean cancelled;

        LoaderTask(
                CachedAppAssetLoader assetLoader,
                ImageView imageView,
                TextView textView,
                boolean diskOnly) {
            this.assetLoader = assetLoader;
            this.imageViewRef = new WeakReference<>(imageView);
            this.textViewRef = new WeakReference<>(textView);
            this.diskOnly = diskOnly;
        }

        void executeOn(ThreadPoolExecutor executor, LoaderTuple loaderTuple) {
            tuple = loaderTuple;
            this.executor = executor;
            LoaderFutureTask submittedFuture = new LoaderFutureTask(this);
            future = submittedFuture;
            if (cancelled) {
                submittedFuture.cancel(true);
                return;
            }
            executor.execute(submittedFuture);
            if (cancelled) {
                submittedFuture.cancel(true);
                executor.remove(submittedFuture);
            }
        }

        boolean isCancelled() {
            return cancelled;
        }

        void cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            LoaderFutureTask activeFuture = future;
            if (activeFuture != null) {
                activeFuture.cancel(mayInterruptIfRunning);
                ThreadPoolExecutor activeExecutor = executor;
                if (activeExecutor != null) {
                    activeExecutor.remove(activeFuture);
                }
            }
        }

        void onQueuedTaskCancelled(boolean showFallback) {
            cancelled = true;
            if (showFallback) {
                assetLoader.mainHandler.post(this::showFallbackIfCurrent);
            }
        }

        @Override
        public void run() {
            ScaledBitmap bitmap = loadInBackground();
            if (!cancelled) {
                assetLoader.mainHandler.post(() -> deliverResult(bitmap));
            }
        }

        private ScaledBitmap loadInBackground() {
            if (isCancelled() ||
                    imageViewRef.get() == null ||
                    textViewRef.get() == null) {
                return null;
            }

            ScaledBitmap bitmap = assetLoader.diskLoader.loadBitmapFromCache(
                    tuple,
                    (int) assetLoader.scalingDivider);
            if (bitmap == null) {
                if (diskOnly) {
                    assetLoader.mainHandler.post(this::startNetworkLoad);
                }
                else {
                    bitmap = assetLoader.doNetworkAssetLoad(tuple, this);
                }
            }

            if (bitmap != null) {
                assetLoader.memoryLoader.populateCache(tuple, bitmap);
            }
            return bitmap;
        }

        private void startNetworkLoad() {
            if (isCancelled()) {
                return;
            }

            ImageView imageView = imageViewRef.get();
            TextView textView = textViewRef.get();
            if (imageView == null ||
                    textView == null ||
                    getLoaderTask(imageView) != this) {
                return;
            }

            LoaderTask task = new LoaderTask(
                    assetLoader,
                    imageView,
                    textView,
                    false);
            AsyncDrawable asyncDrawable = new AsyncDrawable(
                    imageView.getResources(),
                    assetLoader.noAppImageBitmap,
                    task);
            imageView.setImageDrawable(asyncDrawable);
            imageView.startAnimation(AnimationUtils.loadAnimation(
                    imageView.getContext(),
                    R.anim.boxart_fadein));
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            task.executeOn(assetLoader.networkExecutor, tuple);
        }

        private void showFallbackIfCurrent() {
            ImageView imageView = imageViewRef.get();
            TextView textView = textViewRef.get();
            if (imageView == null ||
                    textView == null ||
                    getLoaderTask(imageView) != this) {
                return;
            }

            imageView.setImageBitmap(assetLoader.noAppImageBitmap);
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
        }

        private void deliverResult(final ScaledBitmap bitmap) {
            if (isCancelled()) {
                return;
            }

            ImageView imageView = imageViewRef.get();
            TextView textView = textViewRef.get();
            if (imageView == null ||
                    textView == null ||
                    getLoaderTask(imageView) != this ||
                    bitmap == null) {
                return;
            }

            textView.setVisibility(
                    assetLoader.isBitmapPlaceholder(bitmap)
                            ? View.VISIBLE
                            : View.GONE);

            if (imageView.getVisibility() == View.VISIBLE) {
                Animation fadeOutAnimation = AnimationUtils.loadAnimation(
                        imageView.getContext(),
                        R.anim.boxart_fadeout);
                fadeOutAnimation.setAnimationListener(
                        new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                imageView.setImageBitmap(bitmap.bitmap);
                                imageView.startAnimation(
                                        AnimationUtils.loadAnimation(
                                                imageView.getContext(),
                                                R.anim.boxart_fadein));
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                imageView.startAnimation(fadeOutAnimation);
            }
            else {
                imageView.setImageBitmap(bitmap.bitmap);
                imageView.startAnimation(AnimationUtils.loadAnimation(
                        imageView.getContext(),
                        R.anim.boxart_fadein));
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private static final class LoaderFutureTask extends FutureTask<Void> {
        private final LoaderTask loaderTask;

        LoaderFutureTask(LoaderTask loaderTask) {
            super(loaderTask, null);
            this.loaderTask = loaderTask;
        }

        void cancelFromQueue(boolean showFallback) {
            if (cancel(false)) {
                loaderTask.onQueuedTaskCancelled(showFallback);
            }
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<LoaderTask> loaderTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             LoaderTask loaderTask) {
            super(res, bitmap);
            loaderTaskReference = new WeakReference<>(loaderTask);
        }

        public LoaderTask getLoaderTask() {
            return loaderTaskReference.get();
        }
    }

    private static LoaderTask getLoaderTask(ImageView imageView) {
        if (imageView == null) {
            return null;
        }

        final Drawable drawable = imageView.getDrawable();

        // If our drawable is in play, get the loader task
        if (drawable instanceof AsyncDrawable) {
            final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
            return asyncDrawable.getLoaderTask();
        }

        return null;
    }

    private static boolean cancelPendingLoad(LoaderTuple tuple, ImageView imageView) {
        final LoaderTask loaderTask = getLoaderTask(imageView);

        // Check if any task was pending for this image view
        if (loaderTask != null && !loaderTask.isCancelled()) {
            final LoaderTuple taskTuple = loaderTask.tuple;

            // Cancel the task if it's not already loading the same data
            if (taskTuple == null || !taskTuple.equals(tuple)) {
                loaderTask.cancel(true);
            } else {
                // It's already loading what we want
                return false;
            }
        }

        // Allow the load to proceed
        return true;
    }

    public void queueCacheLoad(NvApp app) {
        final LoaderTuple tuple = new LoaderTuple(computer, app);

        if (memoryLoader.loadBitmapFromCache(tuple) != null) {
            // It's in memory which means it must also be on disk
            return;
        }

        // Queue a fetch in the cache executor
        cacheExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // Check if the image is cached on disk
                if (diskLoader.checkCacheExists(tuple)) {
                    return;
                }

                // Try to load the asset from the network and cache result on disk
                doNetworkAssetLoad(tuple, null);
            }
        });
    }

    private boolean isBitmapPlaceholder(ScaledBitmap bitmap) {
        return (bitmap == null) ||
                (bitmap.originalWidth == 130 && bitmap.originalHeight == 180) || // GFE 2.0
                (bitmap.originalWidth == 628 && bitmap.originalHeight == 888); // GFE 3.0
    }

    public boolean populateImageView(NvApp app, ImageView imgView, TextView textView) {
        LoaderTuple tuple = new LoaderTuple(computer, app);

        // If there's already a task in progress for this view,
        // cancel it. If the task is already loading the same image,
        // we return and let that load finish.
        if (!cancelPendingLoad(tuple, imgView)) {
            return true;
        }

        // Always set the name text so we have it if needed later
        textView.setText(app.getAppName());

        // First, try the memory cache in the current context
        ScaledBitmap bmp = memoryLoader.loadBitmapFromCache(tuple);
        if (bmp != null) {
            // Show the bitmap immediately
            imgView.setVisibility(View.VISIBLE);
            imgView.setImageBitmap(bmp.bitmap);

            // Show the text if it's a placeholder bitmap
            textView.setVisibility(isBitmapPlaceholder(bmp) ? View.VISIBLE : View.GONE);
            return true;
        }

        // If it's not in memory, create an async task to load it. This task will be attached
        // via AsyncDrawable to this view.
        final LoaderTask task =
                new LoaderTask(this, imgView, textView, true);
        final AsyncDrawable asyncDrawable = new AsyncDrawable(imgView.getResources(), placeholderBitmap, task);
        textView.setVisibility(View.INVISIBLE);
        imgView.setVisibility(View.INVISIBLE);
        imgView.setImageDrawable(asyncDrawable);

        // Run the task on our foreground executor
        task.executeOn(foregroundExecutor, tuple);
        return false;
    }

    public static class LoaderTuple {
        public final ComputerDetails computer;
        public final NvApp app;

        public LoaderTuple(ComputerDetails computer, NvApp app) {
            this.computer = computer;
            this.app = app;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LoaderTuple)) {
                return false;
            }

            LoaderTuple other = (LoaderTuple) o;
            return computer.uuid.equals(other.computer.uuid) && app.getAppId() == other.app.getAppId();
        }

        @Override
        public String toString() {
            return "("+computer.uuid+", "+app.getAppId()+")";
        }
    }
}
