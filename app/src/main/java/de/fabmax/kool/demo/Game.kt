//file Game.kt
package de.fabmax.kool.demo

import de.fabmax.kool.Assets
import de.fabmax.kool.input.Input
import de.fabmax.kool.loadAudioClip
import de.fabmax.kool.loadTexture2d
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.audio.AudioClip
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.Column
import de.fabmax.kool.modules.ui2.Image
import de.fabmax.kool.modules.ui2.Row
import de.fabmax.kool.modules.ui2.Text
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.addPanelSurface
import de.fabmax.kool.modules.ui2.align
import de.fabmax.kool.modules.ui2.alignX
import de.fabmax.kool.modules.ui2.margin
import de.fabmax.kool.modules.ui2.onClick
import de.fabmax.kool.modules.ui2.size
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.addTextureMesh
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Time
import kotlinx.coroutines.launch
import de.fabmax.kool.modules.ui2.alignY
import de.fabmax.kool.modules.ui2.background
import de.fabmax.kool.modules.ui2.font
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.modules.ui2.textColor
import de.fabmax.kool.util.MsdfFont

var gameGroundMeshes = mutableListOf<Mesh>()
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
var gameStarted: Boolean = false
var idleTimer = 0f

val groundLevel = -25.6f + groundHeight / 2f
var gameOver = false
var playerHit = false

val pipeWidth = 5.2f
val pipeHeight = 32f
var birdRef: Node? = null

val pipeMeshes = mutableListOf<Mesh>()
val pipeGap = 12f          // vertical gap between top and bottom pipe
val pipeSpacing = 35f      // horizontal distance between pipe pairs

var pipeTex: Texture2d? = null

data class PipePair(val bottom: Node, val top: Node, var scored: Boolean = false)

val pipePairs = mutableListOf<PipePair>()
var scoreText = mutableStateOf("")
var score = 0

lateinit var flapAudioClip: AudioClip
lateinit var pointAudioClip: AudioClip
lateinit var hitAudioClip: AudioClip

//object GameUI {
//    var scoreLabel: TextScope? = null
//}

//the UI
fun gameHudScene(): Scene = UiScene("GameHud") {
    coroutineScope.launch {

        //suppose to be the counter label
        addPanelSurface {
            modifier
                .size(150.dp, 60.dp)
                .align(AlignmentX.Center, AlignmentY.Top)
                .margin(top = 12.dp)
                .background(null) //transparent

            //wait is this called every frame ?
            //score++;
            scoreText.set(score.toString())
            Text(scoreText.use()) {
                modifier
                    .font(MsdfFont(sizePts = 55f))
                    .textColor(colors.primary)
                    .alignX(AlignmentX.Center)
                    .alignY(AlignmentY.Center)
            }
        }
    }
}

fun gameOverPanel(): Scene = UiScene("GameOverUI") {
    coroutineScope.launch {
        val replayTex = loadTexture("sprites/play_btn.png")
        val homeTex = loadTexture("sprites/play_btn.png")
        flapAudioClip = Assets.loadAudioClip("audio/wing.ogg").getOrThrow()
        val result = Assets.loadAudioClip("audio/hit.ogg")
            .onSuccess { clip ->
                hitAudioClip = clip
            }
            .onFailure { e ->
                println("Failed to load sound: ${e.message}")
            }
        pointAudioClip = Assets.loadAudioClip("audio/point.ogg").getOrThrow()

        addPanelSurface {
            modifier
                .size(250.dp, 100.dp)
                .align(AlignmentX.Center, AlignmentY.Center)


            Column {
                Text("Game Over!") {
                    //modifier.fontSize(48.dp)
                }
                Row {
                    Image(replayTex) {
                        modifier.size(120.dp, 60.dp).onClick {
                            println("Replay clicked")
                            SceneManager.reloadGameScene()
                        }
                    }
                    Image(homeTex) {
                        modifier.size(120.dp, 60.dp).onClick {
                            println("Home clicked")
                            SceneManager.loadMainMenu()
                        }
                    }
                }
            }
        }
    }
}

fun Node.toGameRect(width: Float, height: Float): GameRect {
    val pos = transform.getTranslationF()
    return GameRect(
        x = pos.x - width / 2f,
        y = pos.y - height / 2f,
        width = width,
        height = height
    )
}

fun spawnPipePair(x: Float, yCenter: Float, pipeTex: Texture2d?, scene: Scene) {
    val pipeWidth = 5.2f
    val pipeHeight = 32f

    // bottom pipe
    val bottomPipe = scene.addTextureMesh {
        generate {
            rect { size.set(pipeWidth, pipeHeight) }
        }
        shader = KslUnlitShader {
            color { textureColor(pipeTex) }
        }
    }
    bottomPipe.transform.setPosition(x, yCenter - (pipeGap / 2f + pipeHeight / 2f), 0f)

    // top pipe (rotated upside down)
    val topPipe = scene.addTextureMesh {
        generate {
            rect { size.set(pipeWidth, pipeHeight) }
        }
        shader = KslUnlitShader {
            color { textureColor(pipeTex) }
        }
    }
    topPipe.transform.setPosition(x, yCenter + (pipeGap / 2f + pipeHeight / 2f), 0f)
    topPipe.transform.rotate(0f.deg, 0f.deg, 180f.deg)

    // track them
    pipeMeshes += bottomPipe
    pipeMeshes += topPipe

    //to handle the score
    val pair = PipePair(bottomPipe, topPipe)
    pipePairs += pair
}

fun gameScene(): Scene = scene("Game")
{
//    defaultOrbitCamera()

    gameOver = false
    gameStarted = false
    pipeMeshes.clear()
    pipePairs.clear()
    gameGroundMeshes.clear()

    camera = OrthographicCamera().apply {
        setCentered(height = 51.2f, near = -100f, far = 100f)
    }
    camera.transform.setPosition(0f, 0f, -10f)

    coroutineScope.launch {

        pipeTex = Assets.loadTexture2d(
            assetPath = "sprites/pipe-green.png",
            format = TexFormat.RGBA,
            mipMapping = MipMapping.Off,
            samplerSettings = SamplerSettings().nearest()
        ).getOrThrow()

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
            ground.transform.translate(i * groundWidth - groundWidth / 2, -25.6f, 1f)
            gameGroundMeshes += ground
        }


        val frame1Tex = loadTexture("sprites/yellowbird-upflap.png")
        val frame2Tex = loadTexture("sprites/yellowbird-midflap.png")
        val frame3Tex = loadTexture("sprites/yellowbird-downflap.png")

        birdFrames = listOf(frame1Tex, frame2Tex, frame3Tex)

        //now the bird
        birdRef = addTextureMesh {
            generate {
                rect { size.set(birdWidth, birdHeight) }
            }
            shader = KslUnlitShader {
                color { textureColor(frame1Tex) }
            }
            onUpdate {
                frameTimer += Time.deltaT
                if (frameTimer >= frameDuration && !gameOver) {
                    frameTimer = 0f
                    frameIndex = (frameIndex + 1) % birdFrames.size
                    (shader as KslUnlitShader).colorMap = birdFrames[frameIndex]
                }

                if (!gameStarted) {
                    // idle bobbing animation
                    idleTimer += Time.deltaT
                    birdY = (groundHeight * 0.75f) + kotlin.math.sin(idleTimer * 5f) * 1.5f
                    transform.setPosition(groundWidth * -0.25f, birdY, 1f)
                }
            }
        }
        birdRef?.transform?.setPosition(groundWidth * -0.25f, groundHeight * 0.75f, 1f)
    }

    //Scene Loop
    onUpdate {
        val dt = Time.deltaT
        val p0 = Input.pointer.primaryPointer
        if (!gameOver && p0.isAnyButtonClicked) {

            flapAudioClip.play()

            if (!gameStarted) {
                gameStarted = true
                velocity = flapStrength
            } else {
                velocity = flapStrength
            }
        }

        //scroll the land
        if (!gameOver) {
            for (ground in gameGroundMeshes) {
                ground.transform.translate(-scrollSpeed * dt, 0f, 0f)
            }
        }

        //review if the position is not beyond the limit
        for (ground in gameGroundMeshes) {
            //I assume the pivot is 0.5,0.5
            if (ground.transform.getTranslationF().x < -groundWidth * 2) {
                //search the fartest
                var fartest = gameGroundMeshes.first()
                for (m in gameGroundMeshes) {
                    if (m.transform.getTranslationF().x > fartest.transform.getTranslationF().x)
                        fartest = m
                }
                val farPos = fartest.transform.getTranslationF()
                farPos.x += groundWidth
                ground.transform.setPosition(farPos)
            }
        }

        //handle bird
        // apply gravity
        velocity += gravity * Time.deltaT
        birdY += velocity * Time.deltaT


        if (birdY - birdHeight / 2f <= groundLevel) {
            birdY = groundLevel + birdHeight / 2f   // snap bird onto ground
            velocity = 0f                           // stop falling
            if(!gameOver) {
                SceneManager.gameSceneUIGameOver?.isVisible = true;
                hitAudioClip.play()
            }
            gameOver = true                         // or trigger restart
        }
        birdRef?.transform?.setPosition(groundWidth * -0.25f, birdY, 1f)
        val iterator = pipeMeshes.iterator()
        if (!gameOver) {
            while (iterator.hasNext()) {
                val pipe = iterator.next()
                pipe.transform.translate(-scrollSpeed * dt, 0f, 0f)

                // if off-screen left, remove
                if (pipe.transform.getTranslationF().x < -40f) {
                    iterator.remove()
                    removeNode(pipe)
                }
            }
        }

        // spawn new pipes
        if (gameStarted) {
            if (pipeMeshes.isEmpty() || (pipeMeshes.last().transform.getTranslationF().x < 0f)) {
                val yCenter = (-5..5).random().toFloat() // random vertical shift
                spawnPipePair(20f, yCenter, pipeTex, this)
            }
        }

        if (!gameOver) {
            val birdRect = GameRect(
                x = groundWidth * -0.25f - birdWidth / 2f,
                y = birdY - birdHeight / 2f,
                width = birdWidth,
                height = birdHeight
            )

            for (pipe in pipeMeshes) {
                val pipeRect = pipe.toGameRect(5.2f, 32f) // same width/height used in spawnPipePair
                if (birdRect.intersects(pipeRect)) {
                    println("Collision with pipe!")
                    gameOver = true
                    hitAudioClip.play()
                    SceneManager.gameSceneUIGameOver?.isVisible = true;
                    break
                }
            }


            val birdX = birdRef?.transform?.getTranslationF()?.x ?: (groundWidth * -0.25f)
            val birdLeft = birdX - birdWidth * 0.5f
            for (pair in pipePairs) {
                val pipeX = pair.bottom.transform.getTranslationF().x
                val pipeRight = pipeX + pipeWidth * 0.5f

                if (!pair.scored && pipeRight < birdLeft) {
                    pair.scored = true
                    score = score + 1
                    scoreText.set(score.toString())
                    pointAudioClip.play()
                    println("Score: $score")
                }
            }
        }
    }

    suspend fun playFlapSound() {
        val sound: Result<AudioClip> = Assets.loadAudioClip("assets/wing.ogg")
        sound
            .onSuccess { clip ->
                clip.play()
            }
            .onFailure { e ->
                println("Failed to load sound: ${e.message}")
            }
//        sound.getOrThrow().play()//the short version

    }
}
