package de.fabmax.kool.demo

import de.fabmax.kool.Assets
import de.fabmax.kool.loadTexture2d
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.Image
import de.fabmax.kool.modules.ui2.RoundRectBackground
import de.fabmax.kool.modules.ui2.Row
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.addPanelSurface
import de.fabmax.kool.modules.ui2.align
import de.fabmax.kool.modules.ui2.alignX
import de.fabmax.kool.modules.ui2.alignY
import de.fabmax.kool.modules.ui2.background
import de.fabmax.kool.modules.ui2.margin
import de.fabmax.kool.modules.ui2.onClick
import de.fabmax.kool.modules.ui2.size
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.addTextureMesh
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Time
import kotlinx.coroutines.launch

val groundWidth = 33.6f   // scale of base.png (336px / 10)
val groundHeight = 11.2f  // scale of base.png (112px / 10)
val scrollSpeed = 10f     // units per second (tweak until it feels right)
val groundMeshes = mutableListOf<Mesh>()

fun mainMenuSceneUI(): Scene = UiScene("MainMenuUI") {
    // setup UI layer
    //setupUiScene(Scene.DEFAULT_CLEAR_COLOR as ClearColor)
    coroutineScope.launch {

        val playBtnTex = loadTexture("sprites/play_btn.png")
        val leaderboardTex = loadTexture("sprites/leaderboard_btn.png")
        val titleTex = loadTexture("sprites/title.png")

        addPanelSurface {
            val sizeX: Int = SceneManager.koolCtx.window.sizeOnScreen.x
            val sizeY: Int = SceneManager.koolCtx.window.sizeOnScreen.y
            val isLandscape = (sizeX > sizeY)

            var marginTop = 10.dp
            if (isLandscape) {
                marginTop = 10.dp
            } else {
                marginTop = 160.dp
            }

            modifier
                .size(250.dp, 100.dp)
                .align(AlignmentX.Center, AlignmentY.Top)
//                .background(RoundRectBackground(colors.background, 16.dp))
                .background(null) //transparent
                .margin(top = marginTop)

            // Title at the top
            Image(titleTex) {
                modifier
                    .size(200.dp, 100.dp)
                    .alignX(AlignmentX.Center)
                    .margin(top = 16.dp, bottom = 24.dp)
            }
        }

        addPanelSurface {
            //The play and leaderboard buttons
            val sizeX: Int = SceneManager.koolCtx.window.sizeOnScreen.x
            val sizeY: Int = SceneManager.koolCtx.window.sizeOnScreen.y
            val isLandscape = (sizeX > sizeY)

            var colorBg: RoundRectBackground? = null
            if (isLandscape)
                colorBg = RoundRectBackground(colors.background, 16.dp)

            var marginBottom = 85.dp
            if (isLandscape)
                marginBottom = 50.dp
            else
                marginBottom = 120.dp

            modifier
                .size(width = 250.dp, height = 80.dp)
                .alignX(AlignmentX.Center)
                .alignY(AlignmentY.Bottom)
                .margin(bottom = marginBottom)
//                .background(RoundRectBackground(colors.background, 16.dp))
//                .background(colorBg)
                .background(null)

            Row {
                modifier
                    .align(AlignmentX.Center, AlignmentY.Bottom)
                //.spacing(16.dp)   // add gap between buttons

                Image(playBtnTex) {
                    modifier
                        .size(120.dp, 60.dp)
                        .onClick {
                            println("Start clicked")
                            SceneManager.loadGameScene()
                        }
                }

                Image(leaderboardTex) {
                    modifier
                        .size(120.dp, 60.dp)
                        .onClick { println("Leaderboard clicked") }
                }
            }
        }
    }
}

fun mainMenuScene(): Scene = scene("MainMenu") {
    // this runs immediately when scene is created
//    defaultOrbitCamera()
    camera = OrthographicCamera().apply {
        setCentered(height = 51.2f, near = -100f, far = 100f)
    }
    camera.transform.setPosition(0f, 0f, -10f)
    groundMeshes.clear()

    coroutineScope.launch {
        val bgTex = Assets.loadTexture2d(
            assetPath = "sprites/background-day.png",
            format = TexFormat.RGBA,
            mipMapping = MipMapping.Off,
            samplerSettings = SamplerSettings().nearest()
        ).getOrThrow()

        val scrollLandTex = Assets.loadTexture2d(
            assetPath = "sprites/base.png",
            format = TexFormat.RGBA,
            mipMapping = MipMapping.Off,
            samplerSettings = SamplerSettings().nearest()
        ).getOrThrow()

        // background
        addTextureMesh {
            generate {
                rect { size.set(28.8f, 51.2f) }
            }
            shader = KslUnlitShader {
                color { textureColor(bgTex) }
            }
        }

        // ground meshes 5 units was enough for landscape
        repeat(5) { i ->
            val ground = addTextureMesh {
                generate {
                    rect { size.set(groundWidth, groundHeight) }
                }
                shader = KslUnlitShader {
                    color { textureColor(scrollLandTex) }
                }
            }
            ground.transform.translate(i * groundWidth - groundWidth / 2, -20.6f, 0f)
            groundMeshes += ground
        }

        //setup the bird
        val frame1Tex = loadTexture("sprites/yellowbird-upflap.png")
        val frame2Tex = loadTexture("sprites/yellowbird-midflap.png")
        val frame3Tex = loadTexture("sprites/yellowbird-downflap.png")
        birdFrames = listOf(frame1Tex, frame2Tex, frame3Tex)//

        val birdRef = addTextureMesh {
            generate {
                rect { size.set(birdWidth, birdHeight) }
            }
            shader = KslUnlitShader {
                color { textureColor(frame1Tex) }
            }
            onUpdate {
                frameTimer += Time.deltaT
                if (frameTimer >= frameDuration) {
                    frameTimer = 0f
                    frameIndex = (frameIndex + 1) % birdFrames.size
                    (shader as KslUnlitShader).colorMap = birdFrames[frameIndex]
                }
                // idle bobbing animation
                idleTimer += Time.deltaT
                birdY = (groundHeight * 0.50f) + kotlin.math.sin(idleTimer * 5f) * 1.0f
                transform.setPosition(0f, birdY, 1f)
            }
        }
    }

    // this is attached to the scene itself, so it runs every frame
    onUpdate {
        val dt = Time.deltaT
        for (ground in groundMeshes) {
            ground.transform.translate(-scrollSpeed * dt, 0f, 0f)
        }

        //review if the position is not beyond the limit
        for (ground in groundMeshes) {
            //I assume the pivot is 0.5,0.5
            if (ground.transform.getTranslationF().x < -groundWidth * 2) {
                //search the fartest
                var fartest = groundMeshes.first()
                for (m in groundMeshes) {
                    if (m.transform.getTranslationF().x > fartest.transform.getTranslationF().x)
                        fartest = m
                }
                val farPos = fartest.transform.getTranslationF()
                farPos.x += groundWidth
                ground.transform.setPosition(farPos)
            }
        }
    }
}

suspend fun loadTexture(path: String): Texture2d {
    return Assets.loadTexture2d(
        assetPath = path,
        format = TexFormat.RGBA,
        mipMapping = MipMapping.Off,
        samplerSettings = SamplerSettings().nearest()
    ).getOrThrow()
}
