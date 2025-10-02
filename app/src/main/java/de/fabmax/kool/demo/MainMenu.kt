package de.fabmax.kool.demo

import de.fabmax.kool.Assets
import de.fabmax.kool.loadTexture2d
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.Button
import de.fabmax.kool.modules.ui2.Colors
import de.fabmax.kool.modules.ui2.Image
import de.fabmax.kool.modules.ui2.RectBackground
import de.fabmax.kool.modules.ui2.RoundRectBackground
import de.fabmax.kool.modules.ui2.Row
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.addPanelSurface
import de.fabmax.kool.modules.ui2.align
import de.fabmax.kool.modules.ui2.alignX
import de.fabmax.kool.modules.ui2.background
import de.fabmax.kool.modules.ui2.font
import de.fabmax.kool.modules.ui2.margin
import de.fabmax.kool.modules.ui2.onClick
import de.fabmax.kool.modules.ui2.padding
import de.fabmax.kool.modules.ui2.setupUiScene
import de.fabmax.kool.modules.ui2.size
import de.fabmax.kool.pipeline.ClearColor
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.platform.imageAtlasTextureData
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.addTextureMesh
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.scene.set
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.Time
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlin.math.max

val groundWidth = 33.6f   // scale of base.png (336px / 10)
val groundHeight = 11.2f  // scale of base.png (112px / 10)
val scrollSpeed = 10f     // units per second (tweak until it feels right)

val groundMeshes = mutableListOf<Mesh>()

fun mainMenuSceneUI(): Scene= UiScene("MainMenu"){
        // setup UI layer
        //setupUiScene(Scene.DEFAULT_CLEAR_COLOR as ClearColor)
    coroutineScope.launch {
        val playBtnTex = loadUiTexture("sprites/play_btn.png")
        val leaderboardTex = loadUiTexture("sprites/leaderboard_btn.png")
        val titleTex =loadUiTexture("sprites/title.png")

        addPanelSurface {
            // Title at the top
            Image(titleTex) {
                modifier
                    .size(150.dp, 100.dp)
                    .alignX(AlignmentX.Center)
                    .margin(top = 16.dp, bottom = 24.dp)
            }
            modifier
                .size(300.dp, 400.dp)
                .align(AlignmentX.Center, AlignmentY.Center)
//                .background(RoundRectBackground(colors.background, 16.dp))
                .background(null) //transparent


            Row {
                modifier
                    .align(AlignmentX.Center, AlignmentY.Bottom)
                    //.spacing(16.dp)   // add gap between buttons

                Image(playBtnTex) {
                    modifier
                        .size(120.dp, 60.dp)
                        .onClick { println("Start clicked") }
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

fun mainMenuScene(): Scene = scene {
    // this runs immediately when scene is created
    //defaultOrbitCamera()
    camera = OrthographicCamera().apply {
        setCentered(height = 51.2f, near = -100f,far = 100f)
    }
    camera.transform.setPosition(0f,0f,-10f)


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

        // ground meshes
        repeat(3) { i ->
            val ground = addTextureMesh {
                generate {
                    rect { size.set(groundWidth, groundHeight) }
                }
                shader = KslUnlitShader {
                    color { textureColor(scrollLandTex) }
                }
            }
            ground.transform.translate(i * groundWidth - groundWidth / 2, -25.6f, 0f)
            groundMeshes += ground
        }


    }

    // this is attached to the scene itself, so it runs every frame
    onUpdate {
        val dt = Time.deltaT
        for (ground in groundMeshes) {
            ground.transform.translate(-scrollSpeed * dt, 0f, 0f)
        }

        //review if the position is not beyonf the limit
        for (ground in groundMeshes) {
            //I assume the pivot is 0.5,0.5
            if (ground.transform.getTranslationF().x < -28.8f) {
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

suspend fun loadUiTexture(path: String): Texture2d {
    return Assets.loadTexture2d(
        assetPath = path,
        format = TexFormat.RGBA,
        mipMapping = MipMapping.Off,
        samplerSettings = SamplerSettings().nearest()
    ).getOrThrow()
}
