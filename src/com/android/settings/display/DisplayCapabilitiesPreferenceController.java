/*
 * Copyright (C) 2026
 * Licensed under the Apache License, Version 2.0
 */
package com.android.settings.display;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Read-only report of capabilities actually exposed by the display HAL.
 *
 * A panel DTS HDR flag alone does not prove that Android can present HDR.
 * Do not enable HDR or non-existent refresh modes by writing global settings.
 */
public class DisplayCapabilitiesPreferenceController extends BasePreferenceController {

    public DisplayCapabilitiesPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final DisplayManager manager = mContext.getSystemService(DisplayManager.class);
        final Display display = manager == null ? null : manager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            return mContext.getString(R.string.display_capabilities_unavailable);
        }

        final Display.Mode activeMode = display.getMode();
        final List<String> frequencies = activeMode == null ? new ArrayList<>() :
                Arrays.stream(display.getSupportedModes())
                        .filter(mode -> mode.getPhysicalWidth() == activeMode.getPhysicalWidth()
                                && mode.getPhysicalHeight() == activeMode.getPhysicalHeight())
                        .map(mode -> Math.round(mode.getRefreshRate()))
                        .filter(hz -> hz > 0)
                        .distinct().sorted()
                        .map(hz -> hz + " Hz")
                        .collect(Collectors.toList());

        final Display.HdrCapabilities capabilities = display.getHdrCapabilities();
        final int[] types = capabilities == null ? new int[0] :
                capabilities.getSupportedHdrTypes();
        final List<String> hdr = new ArrayList<>();
        for (int type : types) {
            switch (type) {
                case Display.HdrCapabilities.HDR_TYPE_HDR10:
                    hdr.add("HDR10");
                    break;
                case Display.HdrCapabilities.HDR_TYPE_HLG:
                    hdr.add("HLG");
                    break;
                case Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS:
                    hdr.add("HDR10+");
                    break;
                case Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION:
                    hdr.add("Dolby Vision");
                    break;
                default:
                    // Never advertise an unknown HDR type.
                    break;
            }
        }

        final String hdrDescription = hdr.isEmpty() ?
                mContext.getString(R.string.display_capabilities_hdr_unreported) :
                String.join(", ", hdr);
        final String ratesDescription = frequencies.isEmpty() ? "-" :
                String.join(", ", frequencies);
        return mContext.getString(R.string.display_capabilities_summary,
                hdrDescription, ratesDescription);
    }
}
