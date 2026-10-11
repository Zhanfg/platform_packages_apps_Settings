/*
 * Copyright (C) 2026
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controls the Android 17 per-display HDR content policy.
 *
 * Enabled: allow native HDR when content and hardware support it.
 * Disabled: SDR-only; this is not HDR emulation or forced SDR->HDR conversion.
 * The entry stays hidden until Composer exposes multiple real color modes.
 */
public final class Op6HdrContentPreferenceController extends TogglePreferenceController {

    public Op6HdrContentPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    private DisplayManager getManager() {
        return mContext.getSystemService(DisplayManager.class);
    }

    private boolean hasUsableDisplayModes() {
        if (!"OnePlus6".equalsIgnoreCase(Build.DEVICE)
                && !"enchilada".equalsIgnoreCase(Build.DEVICE)) {
            return false;
        }
        final DisplayManager manager = getManager();
        final Display display = manager == null
                ? null : manager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            return false;
        }

        // A broken OP6 HWC advertises HDR formats but maps only NATIVE.
        final int[] modes = display.getSupportedColorModes();
        if (modes == null || modes.length <= 1) {
            return false;
        }

        final Display.HdrCapabilities caps = display.getHdrCapabilities();
        return caps != null && caps.getSupportedHdrTypes().length > 0;
    }

    @Override
    public int getAvailabilityStatus() {
        return hasUsableDisplayModes() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        final DisplayManager manager = getManager();
        if (manager == null) {
            return false;
        }
        try {
            return manager.getUserPreferredHdrMode(Display.DEFAULT_DISPLAY)
                    != DisplayManager.HDR_PREFERENCE_SDR_ONLY;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public boolean setChecked(boolean enabled) {
        if (!hasUsableDisplayModes()) {
            return false;
        }
        final DisplayManager manager = getManager();
        if (manager == null) {
            return false;
        }
        final int preference = enabled
                ? DisplayManager.HDR_PREFERENCE_HDR_ALLOWED
                : DisplayManager.HDR_PREFERENCE_SDR_ONLY;
        try {
            manager.setUserPreferredHdrMode(Display.DEFAULT_DISPLAY, preference);
            return manager.getUserPreferredHdrMode(Display.DEFAULT_DISPLAY) == preference;
        } catch (RuntimeException e) {
            // Do not report success if the OS denied or ignored the request.
            return false;
        }
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(isChecked()
                ? R.string.op6_hdr_content_summary
                : R.string.op6_hdr_content_summary_off);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
