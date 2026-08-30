package com.ski.autosolver.vision

import android.graphics.Bitmap

object FrameBus {
    @Volatile private var latest: Bitmap? = null

    fun publish(bitmap: Bitmap) {
        val old = latest
        latest = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) old.recycle()
    }

    fun latestCopy(): Bitmap? = latest?.let { if (!it.isRecycled) it.copy(Bitmap.Config.ARGB_8888, false) else null }

    fun clear() {
        latest?.let { if (!it.isRecycled) it.recycle() }
        latest = null
    }
}
