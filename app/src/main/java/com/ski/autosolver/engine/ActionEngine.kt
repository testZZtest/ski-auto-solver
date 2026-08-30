package com.ski.autosolver.engine

import com.ski.autosolver.accessibility.TouchAccessibilityService
import com.ski.autosolver.model.TapAction

object ActionEngine {
    fun execute(actions: List<TapAction>): Boolean {
        if (actions.isEmpty()) return true
        val service = TouchAccessibilityService.instance ?: return false
        actions.forEach { action ->
            if (!service.tap(action.x, action.y)) return false
            Thread.sleep(action.delayMs.coerceAtLeast(50L))
        }
        return true
    }
}
