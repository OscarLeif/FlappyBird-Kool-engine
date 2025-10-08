package de.fabmax.kool.demo

import android.app.Activity
import android.os.Bundle
import de.fabmax.kool.Assets
import de.fabmax.kool.createDefaultKoolContext
import de.fabmax.kool.loadTexture2d
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.gltf.GltfLoadConfig
import de.fabmax.kool.modules.gltf.GltfMaterialConfig
import de.fabmax.kool.modules.gltf.loadGltfModel
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.FitContent
import de.fabmax.kool.modules.ui2.Text
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.addPanelSurface
import de.fabmax.kool.modules.ui2.align
import de.fabmax.kool.modules.ui2.background
import de.fabmax.kool.modules.ui2.margin
import de.fabmax.kool.modules.ui2.remember
import de.fabmax.kool.modules.ui2.setupUiScene
import de.fabmax.kool.modules.ui2.size
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.ibl.EnvironmentMap
import de.fabmax.kool.platform.KoolContextAndroid
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.Skybox
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.DebugOverlay
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.Time
import kotlinx.coroutines.launch
import kotlin.math.sin
class MainActivity : Activity() {
    lateinit var koolCtx: KoolContextAndroid

    private lateinit var mainMenuScene: Scene
    private lateinit var mainMenuUI: Scene
    private lateinit var gameScene: Scene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        koolCtx = createKoolContext()
        setContentView(koolCtx.surfaceView)
    }

    override fun onResume() {
        super.onResume()
        koolCtx.onResume()
    }

    override fun onPause() {
        super.onPause()
        koolCtx.onPause()
    }

    override fun onDestroy() {
        koolCtx.onDestroy()
        super.onDestroy()
    }

    private fun createKoolContext(): KoolContextAndroid {
        koolCtx = createDefaultKoolContext()
        SceneManager.koolCtx = koolCtx

        SceneManager.mainMenuScene = mainMenuScene()
        SceneManager.mainMenuUI= mainMenuSceneUI()
//
        koolCtx.scenes += SceneManager.mainMenuScene!!
        koolCtx.scenes += SceneManager.mainMenuUI!!


//        val dbgOv = DebugOverlay()
//        dbgOv.isExpanded.set(true)
//        koolCtx.scenes += dbgOv.ui

//        koolCtx.scenes += testScene()
        koolCtx.scenes += fpsCounterScene()

        koolCtx.run()
        return koolCtx
    }

    private fun testScene() = scene {
        coroutineScope.launch {
            defaultOrbitCamera()

            val ibl = loadHdri("shanghai_bund_1k.rgbe.png")
            teapot(ibl)
            addNode(Skybox.cube(ibl.reflectionMap, 2f))
        }
    }

    fun fpsCounterScene():Scene=UiScene {
        coroutineScope.launch {
            setupUiScene()
            addPanelSurface {
                modifier
                    .size(FitContent, FitContent)
                    .align(AlignmentX.End, AlignmentY.Top)
                    .background(null)
                var fps by remember("0")
                Text(fps) {
                    modifier.margin(sizes.smallGap)
                }
                onUpdate {
                    fps = "${Time.fps.toInt()}"
                }
            }
        }
    }

    private suspend fun Scene.teapot(ibl: EnvironmentMap) {
        val materialCfg = GltfMaterialConfig(
            environmentMap = ibl
        )
        // gzip compression and file ending are stripped from asset files (use "teapot.gltf" asset
        // path although project file is named "teapot.gltf.gz"!)
        val modelCfg = GltfLoadConfig(materialConfig = materialCfg)
        val model = Assets.loadGltfModel("teapot.gltf",modelCfg).getOrThrow()

        val mesh = model.meshes.values.first()
        val bounds = mesh.geometry.bounds
        mesh.transform.translate(bounds.center * -1f)

        mesh.shader = KslPbrShader {
            color { uniformColor(MdColor.PINK.toLinear()) }
            roughness(0.25f)
            enableImageBasedLighting(ibl)
        }

        model.onUpdate {
            transform.rotate(30f.deg * Time.deltaT, Vec3f.X_AXIS)
            transform.rotate(63f.deg * Time.deltaT * sin(Time.gameTime * 0.2).toFloat(), Vec3f.Y_AXIS)
        }
        addNode(model)
    }

    private suspend fun loadHdri(assetPath: String): EnvironmentMap {
        val rgbe = Assets.loadTexture2d(
            assetPath = assetPath,
            format = TexFormat.RGBA,
            mipMapping = MipMapping.Off,
            samplerSettings = SamplerSettings().nearest()
        ).getOrThrow()

        return EnvironmentMap.fromHdriTexture(rgbe)
    }
}
