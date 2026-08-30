package com.ski.autosolver.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.ski.autosolver.model.WaterState
import kotlin.math.abs

/**
 * First-pass visual reader. It intentionally avoids OCR/AI and uses color clusters.
 * It expects a standard vertical tube layout and can be replaced independently later.
 */
object WaterSortVision {
    data class TubeSample(val x: Int, val yTop: Int, val yBottom: Int)

    fun detect(bitmap: Bitmap, tubeCount: Int): Pair<WaterState, List<TubeSample>>? {
        if (tubeCount !in 2..16) return null
        val width = bitmap.width
        val height = bitmap.height
        if (width < 100 || height < 100) return null

        val spacing = width.toFloat() / (tubeCount + 1)
        val tubes = ArrayList<List<Int>>(tubeCount)
        val samples = ArrayList<TubeSample>(tubeCount)

        for (i in 0 until tubeCount) {
            val x = (spacing * (i + 1)).toInt().coerceIn(2, width - 3)
            val yTop = (height * 0.18f).toInt()
            val yBottom = (height * 0.78f).toInt()
            samples.add(TubeSample(x, yTop, yBottom))

            val colors = ArrayList<Int>(WaterState.CAPACITY)
            for (slot in 0 until WaterState.CAPACITY) {
                // Bottom -> top. Sample away from borders.
                val yf = yBottom - (slot + 0.5f) * (yBottom - yTop) / WaterState.CAPACITY
                val c = bitmap.getPixel(x, yf.toInt().coerceIn(0, height - 1))
                colors.add(classify(c))
            }
            val normalized = colors.map { if (it < 0) 0 else it }.toMutableList()
            while (normalized.isNotEmpty() && normalized.last() == 0) normalized.removeAt(normalized.lastIndex)
            tubes.add(normalized)
        }
        return WaterState(tubes) to samples
    }

    private fun classify(pixel: Int): Int {
        val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
        val max = maxOf(r, g, b); val min = minOf(r, g, b)
        if (max < 45 || max - min < 12) return 0
        // Quantized hue buckets. The exact appearance varies per app; the solver only needs consistency.
        val h = rgbToHue(r, g, b)
        return (h / 20).toInt() + 1
    }

    private fun rgbToHue(r: Int, g: Int, b: Int): Float {
        val rf=r/255f; val gf=g/255f; val bf=b/255f
        val max=maxOf(rf,gf,bf); val min=minOf(rf,gf,bf); val d=max-min
        if (d == 0f) return 0f
        var h = when (max) {
            rf -> 60f * (((gf-bf)/d) % 6f)
            gf -> 60f * ((bf-rf)/d + 2f)
            else -> 60f * ((rf-gf)/d + 4f)
        }
        if (h < 0) h += 360f
        return h
    }
}
