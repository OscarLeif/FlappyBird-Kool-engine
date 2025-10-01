package de.fabmax.kool.demo

import de.fabmax.kool.Assets
import de.fabmax.kool.loadTexture2d
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.platform.imageAtlasTextureData
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.addTextureMesh
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Time
import kotlinx.coroutines.launch

    fun mainMenuScene(): Scene= scene {
        coroutineScope.launch {
            defaultOrbitCamera()//kinda lazy we need first setup the background
            val bgTex = Assets.loadTexture2d(
                assetPath = "sprites/background-day.png",
                format = TexFormat.RGBA,
                mipMapping = MipMapping.Off,
                samplerSettings = SamplerSettings().nearest()
            ).getOrThrow()
            // add background image node

            addTextureMesh {
                generate {
                    rect {
                        size.set(28.8f, 51.2f)
                    }
                }
                shader = KslUnlitShader {
                    color{
                        textureColor(bgTex)
                    }
                }
            }
        }
    }
