/*
 * Copyright (C) 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.spa.about

import android.content.Context
import android.database.Cursor
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.Callable
import kotlin.math.roundToInt

private const val EMOJI_AUTHORITY = "com.google.android.apps.emojiwallpaper.config"
private const val CONFIG_PREVIEW = "wallpaper_config_preview_uri"
private const val DESTINATION = "destination"
private const val MAX_IMAGE_SIZE = 1024
private const val TAG = "EmojiHeaderWallpaper"

/** Read the complete rendered pattern, not WallpaperInfo's single-emoji service thumbnail. */
internal fun loadEmojiHeaderWallpaper(context: Context): Drawable? {
    try {
        val resolver = context.contentResolver
        val current = Uri.parse("content://$EMOJI_AUTHORITY/currentwallpapers")
        val imageUri =
            resolver.query(current, null, null, null, null)?.use { emojiWallpaperImageUri(it) }
                ?: return null
        // The Emoji provider has no MIME type, so typed image/* requests cannot open it.
        val source =
            ImageDecoder.createSource(
                Callable {
                    resolver.openAssetFileDescriptor(imageUri, "r")
                        ?: throw FileNotFoundException("No image for the current Emoji wallpaper")
                }
            )
        val bitmap =
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > MAX_IMAGE_SIZE) {
                    val scale = MAX_IMAGE_SIZE.toFloat() / largest
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }
        return BitmapDrawable(context.resources, bitmap)
    } catch (exception: IOException) {
        Log.w(TAG, "Unable to read the Emoji wallpaper image", exception)
    } catch (exception: RuntimeException) {
        Log.w(TAG, "Unable to read the Emoji wallpaper image", exception)
    }
    // An unavailable provider must not replace the user's wallpaper with an unrelated image.
    return null
}

internal fun emojiWallpaperImageUri(cursor: Cursor): Uri? {
    val destination = cursor.getColumnIndex(DESTINATION)
    val preview = cursor.getColumnIndex(CONFIG_PREVIEW)
    if (destination < 0 || preview < 0) return null
    while (cursor.moveToNext()) {
        if (cursor.getString(destination) != "home" || cursor.isNull(preview)) continue
        val config = Uri.parse(cursor.getString(preview))
        if (config.scheme != "content" || config.authority != EMOJI_AUTHORITY) return null
        val assetId = config.getQueryParameter("config_preview")
        if (assetId.isNullOrBlank()) return null
        // This provider endpoint returns the saved, full wallpaper composition.
        return Uri.Builder()
            .scheme("content")
            .authority(EMOJI_AUTHORITY)
            .appendQueryParameter("wallpaper_thumbnail", assetId)
            .build()
    }
    return null
}
