package com.ski.autosolver.engine

import com.ski.autosolver.model.GameType

object GameConfiguration {
    @Volatile var enabled = false
    @Volatile var gameType = GameType.WATER_SORT
    @Volatile var frameRateHz = 8
    @Volatile var actionDelayMs = 140L
    @Volatile var waterTubeCount = 8

    // Percentages of the physical screen used as the board crop.
    @Volatile var cropLeft = 0f
    @Volatile var cropTop = 0f
    @Volatile var cropRight = 1f
    @Volatile var cropBottom = 0.92f
}
