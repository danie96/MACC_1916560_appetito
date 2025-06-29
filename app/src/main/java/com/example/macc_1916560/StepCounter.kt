package com.example.macc_1916560

object StepCounter {
    private var steps = 0

    fun increment() {
        steps++
    }

    fun getSteps(): Int = steps

    fun reset() {
        steps = 0
    }
}