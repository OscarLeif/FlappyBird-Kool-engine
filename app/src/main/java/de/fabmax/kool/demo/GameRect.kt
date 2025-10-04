package de.fabmax.kool.demo

data class GameRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun intersects(other: GameRect): Boolean {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y
    }
}