//file Game.kt
package de.fabmax.kool.demo

import android.R
import android.graphics.Mesh
import de.fabmax.kool.Assets
import de.fabmax.kool.input.Input
import de.fabmax.kool.loadTexture2d
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.addTextureMesh
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Time
import kotlinx.coroutines.launch

val gameGroundMeshes = mutableListOf<Mesh>()
val birdWidth = 3.24f
val birdHeight = 2.4f

var frameIndex = 0
var frameTimer = 0f
val frameDuration = 0.1f  // 100 ms per frame

var birdFrames: List<Texture2d> = listOf()

//physics + Game Logic
var velocity = 0f
var gravity = -30f
var flapStrength = 20f
var birdY = 0f
var gameStarted = false
var idleTimer = 0f

val groundLevel = -25.6f + groundHeight / 2f
var gameOver=false

fun gameScene(): Scene = scene("Game")
{
//    defaultOrbitCamera()
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

        // background I probably need more chunks
        addTextureMesh {
            generate {
                rect { size.set(28.8f, 51.2f) }
            }
            shader = KslUnlitShader {
                color { textureColor(bgTex) }
            }
        }

        //Ok the land
        val scrollLandTex = Assets.loadTexture2d(
            assetPath = "sprites/base.png",
            format = TexFormat.RGBA,
            mipMapping = MipMapping.Off,
            samplerSettings = SamplerSettings().nearest()
        ).getOrThrow()

        repeat(5) { i ->
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

        val frame1Tex = loadUiTexture("sprites/yellowbird-midflap.png")
        val frame2Tex = loadUiTexture("sprites/yellowbird-upflap.png")
        val frame3Tex = loadUiTexture("sprites/yellowbird-downflap.png")

        birdFrames = listOf(frame1Tex, frame2Tex, frame3Tex)

        //now the bird
        val bird = addTextureMesh {
            generate {
                rect { size.set(birdWidth,birdHeight) }
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

                if (!gameStarted) {
                    // idle bobbing animation
                    idleTimer += Time.deltaT
                    birdY = (groundHeight * 0.75f) + kotlin.math.sin(idleTimer * 5f) * 1.5f
                } else {
                    // apply gravity
                    velocity += gravity * Time.deltaT
                    birdY += velocity * Time.deltaT

                    if (birdY - birdHeight / 2f <= groundLevel) {
                        birdY = groundLevel + birdHeight / 2f   // snap bird onto ground
                        velocity = 0f                           // stop falling
                        gameOver = true                         // or trigger restart
                    }
                }

                transform.setPosition(groundWidth * -0.25f, birdY, 1f)
            }
        }
        bird.transform.setPosition(groundWidth * -0.25f, groundHeight * 0.75f,1f)
    }

    onUpdate    {
        val p0 = Input.pointer.primaryPointer
        if ( p0.isAnyButtonClicked) {
            if (!gameStarted) {
                gameStarted = true
                velocity = flapStrength
            } else {
                velocity = flapStrength
            }
        }
    }
}