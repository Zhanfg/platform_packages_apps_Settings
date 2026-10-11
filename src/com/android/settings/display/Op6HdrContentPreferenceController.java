/*
 * Copyright (C) 2026
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.provider.Settings;
import android.view.Display;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import java.util.Arrays;

/**
 * Opt-in control for Android's user-disabled HDR formats, not HDR emulation.
 *
 * The preference is gated on a functional set of HWC color modes; an HDR
 * capability flag alone is insufficient on legacy OnePlus 6.
 *
 * This control does not force SDR video into HDR, modify the panel, or bypass
 * SurfaceFlinger. It uses the framework-owned HDR format policy.
 */
public final class Op6HdrContentPreferenceController extends TogglePreferenceController {
    private static final String ORIGINAL_TYPES_KEY = "op6_hdr_original_disabled_types";
    private static final String ORIGINAL_ALLOWED_KEY = "op6_hdr_original_disabled_allowed";
    private static final int[] ALL_HDR_TYPES = {
            Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION,
            Display.HdrCapabilities.HDR_TYPE_HDR10,
            Display.HdrCapabilities.HDR_TYPE_HLG,
            Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS,
            Display.HdrCapabilities.HDR_TYPE_HLG_PLUS
    };

    public Op6HdrContentPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    private DisplayManager manager() {
        return mContext.getSystemService(DisplayManager.class);
    }

    private boolean isSupported() {
        if (!"OnePlus6".equalsIgnoreCase(Build.DEVICE)
                && !"enchilada".equalsIgnoreCase(Build.DEVICE)) {
            return false;
        }
        final DisplayManager dm = manager();
        final Display display = dm == null ? null : dm.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) return false;

        // The broken Composer reported only NATIVE. Do not expose a fake toggle
        // until the HWC color-mode mapping has actually recovered.
        final int[] modes = display.getSupportedColorModes();
        if (modes == null || modes.length <= 1) return false;

        if (Settings.Secure.getString(mContext.getContentResolver(),
                ORIGINAL_TYPES_KEY) != null) {
            // Preserve access to the OFF -> ON transition even after the
            // framework correctly hides user-disabled HDR capabilities.
            return true;
        }
        final Display.HdrCapabilities caps = display.getHdrCapabilities();
        return caps != null && caps.getSupportedHdrTypes().length > 0;
    }

    @Override
    public int getAvailabilityStatus() {
        return isSupported() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getString(mContext.getContentResolver(),
                ORIGINAL_TYPES_KEY) == null;
    }

    @Override
    public boolean setChecked(boolean enabled) {
        if (!isSupported()) return false;
        final DisplayManager dm = manager();
        if (dm == null) return false;
        try {
            if (enabled) {
                final String saved = Settings.Secure.getString(mContext.getContentResolver(),
                        ORIGINAL_TYPES_KEY);
                if (saved == null) return true;
                final int[] previousTypes = parseSavedTypes(saved);
                dm.setUserDisabledHdrTypes(previousTypes);
                dm.setAreUserDisabledHdrTypesAllowed(
                        Settings.Secure.getInt(mContext.getContentResolver(),
                                ORIGINAL_ALLOWED_KEY, 0) != 0);
                if (Settings.Secure.putString(mContext.getContentResolver(),
                        ORIGINAL_TYPES_KEY, null)) {
                    Settings.Secure.putString(mContext.getContentResolver(),
                            ORIGINAL_ALLOWED_KEY, null);
                    return true;
                }
                return false;
            }
            if (Settings.Secure.getString(mContext.getContentResolver(),
                    ORIGINAL_TYPES_KEY) != null) return true;

            final int[] previousTypes = dm.getUserDisabledHdrTypes();
            final boolean previousAllowed = dm.areUserDisabledHdrTypesAllowed();
            final String previousTypesString = serializeTypes(previousTypes);
            if (!Settings.Secure.putInt(mContext.getContentResolver(),
                    ORIGINAL_ALLOWED_KEY, previousAllowed ? 1 : 0)) return false;
            if (!Settings.Secure.putString(mContext.getContentResolver(),
                    ORIGINAL_TYPES_KEY, previousTypesString)) return false;
            dm.setAreUserDisabledHdrTypesAllowed(false);
            dm.setUserDisabledHdrTypes(ALL_HDR_TYPES.clone());
            return true;
        } catch (RuntimeException e) {
            // UI must not claim that changing HDR policy succeeded if the
            // display service rejected the request.
            return false;
        }
    }

    private static String serializeTypes(int[] types) {
        if (types == null || types.length == 0) return "-";
        final StringBuilder s = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) s.append(',');
            s.append(types[i]);
        }
        return s.toString();
    }

    private static int[] parseSavedTypes(String saved) {
        if (saved == null || "-".equals(saved) || saved.isEmpty()) return new int[0];
        final String[] parts = saved.split(",");
        final int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
