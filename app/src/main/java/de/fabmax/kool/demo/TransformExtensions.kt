package de.fabmax.kool.demo

import de.fabmax.kool.scene.Transform
import de.fabmax.kool.math.*

fun Transform.setPosition(x: Float, y: Float, z: Float) {
    setCompositionOf(
        translation = Vec3f(x, y, z),
        rotation = getRotationF(),
        scale = getScaleF()
    )
}

fun Transform.setPosition(pos: Vec3f) {
    setCompositionOf(
        translation = pos,
        rotation = getRotationF(),
        scale = getScaleF()
    )
}

fun Transform.getPosition(): Vec3f = getTranslationF()