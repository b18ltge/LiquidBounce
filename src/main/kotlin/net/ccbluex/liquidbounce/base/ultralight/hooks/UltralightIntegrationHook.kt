/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.base.ultralight.hooks

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.base.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.ScreenRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawableHelper
import org.lwjgl.opengl.GL31

/**
 * A integration bridge between Minecraft and Ultralight
 */
object UltralightIntegrationHook : Listenable {


    val screenRenderHandler = handler<ScreenRenderEvent> {
        // Update window
        UltralightEngine.update()

        // Render the view

        val textureId = UltralightEngine.window.textureId
        println("Texture ID: $textureId")

        // Render texture
        RenderSystem.enableTexture()
        UltralightEngine.window.bindTexture(0)
        GL31.glActiveTexture(GL31.GL_TEXTURE0)
        RenderSystem.setShaderColor(1.0f, 0.5f, 1.0f, 1.0f)
        RenderSystem.enableBlend()
        DrawableHelper.drawTexture(it.matrices, 0, 0, 0f, 0f, mc.window.scaledWidth, mc.window.scaledHeight, UltralightEngine.window.width, UltralightEngine.window.height)
        RenderSystem.disableTexture()
        RenderSystem.disableBlend()
    }

}
