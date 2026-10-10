/*
 * Copyright (C) 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.spa.about

import android.database.MatrixCursor
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmojiHeaderWallpaperTest {
    @Test
    fun homeWallpaper_usesCompletePatternForCurrentHomeAsset() {
        cursor().use {
            it.addRow(arrayOf(preview("lock-pattern"), "lock"))
            it.addRow(arrayOf(preview("home-pattern"), "home"))

            val uri = emojiWallpaperImageUri(it)

            assertThat(uri)
                .isEqualTo(Uri.parse("content://$AUTHORITY?wallpaper_thumbnail=home-pattern"))
        }
    }

    @Test
    fun lockOnlyWallpaper_doesNotUseUnrelatedLockImage() {
        cursor().use {
            it.addRow(arrayOf(preview("lock-pattern"), "lock"))
            assertThat(emojiWallpaperImageUri(it)).isNull()
        }
    }

    @Test
    fun missingColumns_doesNotGuessAnAsset() {
        MatrixCursor(arrayOf("wallpaper_thumbnail")).use {
            it.addRow(arrayOf("content://$AUTHORITY?wallpaper_thumbnail=old-pattern"))
            assertThat(emojiWallpaperImageUri(it)).isNull()
        }
    }

    @Test
    fun untrustedUri_isNotOpened() {
        cursor().use {
            it.addRow(arrayOf("content://unrelated.provider?config_preview=pattern", "home"))
            assertThat(emojiWallpaperImageUri(it)).isNull()
        }
    }

    @Test
    fun missingAssetId_doesNotUseTheCategoryOrServiceIcon() {
        cursor().use {
            it.addRow(arrayOf("content://$AUTHORITY?category_thumbnail=emoji", "home"))
            assertThat(emojiWallpaperImageUri(it)).isNull()
        }
    }

    @Test
    fun assetId_preservesUriEncoding() {
        cursor().use {
            it.addRow(arrayOf(preview("pattern & colors"), "home"))
            val uri = emojiWallpaperImageUri(it)
            assertThat(uri?.getQueryParameter("wallpaper_thumbnail")).isEqualTo("pattern & colors")
            assertThat(uri?.queryParameterNames).containsExactly("wallpaper_thumbnail")
        }
    }

    private fun cursor() = MatrixCursor(arrayOf("wallpaper_config_preview_uri", "destination"))

    private fun preview(assetId: String) =
        Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .appendQueryParameter("config_preview", assetId)
            .build()
            .toString()

    companion object {
        private const val AUTHORITY = "com.google.android.apps.emojiwallpaper.config"
    }
}
