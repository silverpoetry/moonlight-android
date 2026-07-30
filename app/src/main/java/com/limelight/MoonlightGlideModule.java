package com.limelight;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

/**
 * Application-wide image decoding policy.
 *
 * <p>Glide 3 preferred RGB_565 for opaque images. Retaining that policy avoids
 * doubling the memory occupied by full-screen backgrounds and image grids on
 * memory-constrained Android TV devices.</p>
 */
@GlideModule
public final class MoonlightGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        builder.setDefaultRequestOptions(
                new RequestOptions().format(DecodeFormat.PREFER_RGB_565));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
