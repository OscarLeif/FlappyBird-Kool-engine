package de.fabmax.kool.demo

import de.fabmax.kool.platform.KoolContextAndroid
import de.fabmax.kool.scene.Scene

object SceneManager {
    lateinit var koolCtx: KoolContextAndroid

    var mainMenuScene: Scene? = null
    var mainMenuUI: Scene? = null
    var gameScene: Scene? = null

    fun removeMainMenu() {
        mainMenuScene?.let {
            koolCtx.scenes -= it
            it.release() // free GPU resources
        }
        mainMenuUI?.let {
            koolCtx.scenes -= it
            it.release()
        }
        mainMenuScene = null
        mainMenuUI = null
    }

    fun loadGameScene() {
        // Remove menu
        removeMainMenu()

        // Create and add game scene
        gameScene = gameScene()
        koolCtx.scenes += gameScene!!
    }
}
