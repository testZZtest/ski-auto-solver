package com.ski.autosolver.model

/** One tube is represented bottom -> top. Empty tube = empty list. */
data class WaterState(val tubes: List<List<Int>>) {
    fun isSolved(): Boolean = tubes.all { it.isEmpty() || it.size == CAPACITY && it.all { c -> c == it.first() } }

    companion object { const val CAPACITY = 4 }
}
