package de.fabmax.kool.demo

import de.fabmax.kool.demo.SceneManager.gameSceneUIGameOver
import de.fabmax.kool.platform.KoolContextAndroid
import de.fabmax.kool.scene.Scene

object SceneManager {
    lateinit var koolCtx: KoolContextAndroid

    var mainMenuScene: Scene? = null
    var mainMenuUI: Scene? = null
    var gameScene: Scene? = null
    var gameSceneUI: Scene? = null
    var gameSceneUIGameOver: Scene? = null

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

    fun reloadGameScene()    {

        removeGameScenes()
        // Create and add game scene
        gameScene = gameScene()
        gameSceneUI = gameHudScene();
        gameSceneUIGameOver = gameOverPanel();

        koolCtx.scenes += gameScene!!
        koolCtx.scenes += gameSceneUI!!
        koolCtx.scenes += gameSceneUIGameOver!!

        gameSceneUIGameOver!!.isVisible = false;
    }

    fun loadGameScene() {
        // Remove menu
        removeMainMenu()

        // Create and add game scene
        gameScene = gameScene()
        gameSceneUI = gameHudScene();
        gameSceneUIGameOver = gameOverPanel();


        koolCtx.scenes += gameScene!!
        koolCtx.scenes += gameSceneUI!!
        koolCtx.scenes += gameSceneUIGameOver!!

        gameSceneUIGameOver!!.isVisible = false;
    }

    fun removeGameScenes()
    {
        gameScene?.let { koolCtx.scenes -= it
            it.release() // free GPU resources
        }
        gameSceneUI?.let { koolCtx.scenes -=it
            it.release()
        }
        gameSceneUIGameOver?.let { koolCtx.scenes-=it
            it.release()
        }

        gameSceneUI=null
        gameScene=null
        gameSceneUIGameOver=null
    }

    fun loadMainMenu() {

        removeGameScenes()

        mainMenuScene = mainMenuScene()
        mainMenuUI= mainMenuSceneUI()

        koolCtx.scenes += SceneManager.mainMenuScene!!
        koolCtx.scenes += SceneManager.mainMenuUI!!
    }
}
