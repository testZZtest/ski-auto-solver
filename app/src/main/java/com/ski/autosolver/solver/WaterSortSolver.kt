package com.ski.autosolver.solver

import com.ski.autosolver.model.WaterState

/** Exact breadth-first solver for standard 4-slot Water Sort. */
object WaterSortSolver {
    data class Move(val from: Int, val to: Int)

    fun solve(start: WaterState, maxStates: Int = 250_000): List<Move>? {
        if (start.isSolved()) return emptyList()
        val queue = ArrayDeque<WaterState>()
        val parent = HashMap<WaterState, Pair<WaterState?, Move?>>()
        queue.add(start)
        parent[start] = null to null
        var visited = 0

        while (queue.isNotEmpty() && visited < maxStates) {
            val state = queue.removeFirst()
            visited++
            for (from in state.tubes.indices) {
                for (to in state.tubes.indices) {
                    if (from == to) continue
                    val next = pour(state, from, to) ?: continue
                    if (parent.containsKey(next)) continue
                    parent[next] = state to Move(from, to)
                    if (next.isSolved()) return reconstruct(next, parent)
                    queue.add(next)
                }
            }
        }
        return null
    }

    private fun pour(state: WaterState, from: Int, to: Int): WaterState? {
        val a = state.tubes[from]
        val b = state.tubes[to]
        if (a.isEmpty() || b.size >= WaterState.CAPACITY) return null
        val color = a.last()
        if (b.isNotEmpty() && b.last() != color) return null
        var amount = 0
        for (i in a.size - 1 downTo 0) {
            if (a[i] == color) amount++ else break
        }
        amount = minOf(amount, WaterState.CAPACITY - b.size)
        if (amount == 0) return null

        val nt = state.tubes.map { it.toMutableList() }.toMutableList()
        repeat(amount) { nt[from].removeAt(nt[from].lastIndex); nt[to].add(color) }
        return WaterState(nt.map { it.toList() })
    }

    private fun reconstruct(goal: WaterState, parent: Map<WaterState, Pair<WaterState?, Move?>>): List<Move> {
        val result = ArrayList<Move>()
        var cur = goal
        while (true) {
            val (prev, move) = parent[cur] ?: break
            if (prev == null || move == null) break
            result.add(move)
            cur = prev
        }
        result.reverse()
        return result
    }
}
