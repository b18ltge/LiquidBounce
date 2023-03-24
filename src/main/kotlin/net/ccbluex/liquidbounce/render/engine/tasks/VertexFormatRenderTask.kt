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

package net.ccbluex.liquidbounce.render.engine.tasks

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.render.engine.GlRenderState
import net.ccbluex.liquidbounce.render.engine.Texture
import net.ccbluex.liquidbounce.render.engine.memory.VertexFormat
import net.ccbluex.liquidbounce.render.shaders.ShaderHandler
import net.ccbluex.liquidbounce.utils.math.Mat4
import net.minecraft.client.gl.VertexBuffer
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats
import org.lwjgl.opengl.GL11

class VertexFormatRenderTask<T>(private val bufferFunc: Pair<DrawMode, (BufferBuilder) -> Unit>, val shaderHandler: ShaderHandler<T>, private val perInstance: VertexFormat? = null, private val texture: Texture? = null, private val state: GlRenderState = GlRenderState(), private val shaderData: T? = null) : RenderTask() {

    var vao = VertexBuffer()

    override fun getBatchRenderer(): BatchRenderer? = null

    override fun initRendering(mvpMatrix: Mat4) {
        RenderSystem.disableScissor()
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        state.applyFlags()
    }

    override fun draw() {
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }

        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        val (mode, builder) = bufferFunc

        bufferBuilder.begin(mode, VertexFormats.POSITION_COLOR)
        builder(bufferBuilder)
        tessellator.draw()
    }

    override fun upload() {

    }

    override fun cleanupRendering() {
    }

}

fun makeBuffer(mode: DrawMode, builder: (BufferBuilder) -> Unit) = Pair(mode, builder)
