/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.GlRenderState
import net.ccbluex.liquidbounce.render.engine.RenderEngine
import net.ccbluex.liquidbounce.render.engine.tasks.Color4b
import net.ccbluex.liquidbounce.render.engine.tasks.RenderTask
import net.ccbluex.liquidbounce.render.engine.tasks.VertexFormatRenderTask
import net.ccbluex.liquidbounce.render.engine.tasks.makeBuffer
import net.ccbluex.liquidbounce.render.shaders.SmoothLineShader
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.client.render.VertexFormat

/**
 * Breadcrumbs module
 *
 * Leaves a trace behind you.
 */

object ModuleBreadcrumbs : Module("Breadcrumbs", Category.RENDER) {

    private val color by color("Color", Color4b(255, 179, 72, 255))
    private val colorRainbow by boolean("Rainbow", false)

    private val positions = mutableListOf<Double>()
    private var lastPosX = 0.0
    private var lastPosY = 0.0
    private var lastPosZ = 0.0

    override fun enable() {
        synchronized(positions) {
            positions.addAll(listOf(player.x, player.eyeY, player.z))
            positions.addAll(listOf(player.x, player.y, player.z))
        }
    }

    override fun disable() {
        synchronized(positions) {
            positions.clear()
        }
    }

    val renderHandler = handler<EngineRenderEvent> {
        val color = if (colorRainbow) rainbow() else color

        synchronized(positions) {
            RenderEngine.enqueueForRendering(
                RenderEngine.CAMERA_VIEW_LAYER_WITHOUT_BOBBING,
                createBreadcrumbsRenderTask(
                    color,
                    positions,
                    it.tickDelta
                )
            )
        }
    }

    @JvmStatic
    internal fun createBreadcrumbsRenderTask(color: Color4b, positions: List<Double>, tickDelta: Float): RenderTask {
        return VertexFormatRenderTask(makeBuffer(VertexFormat.DrawMode.LINE_STRIP) {
            for (i in 0 until positions.size / 3) {
                it.vertex(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]).color(color.r, color.b, color.g, color.a).next()
            }

            val interpolatedVec = player.interpolateCurrentPosition(tickDelta)
            it.vertex(interpolatedVec.x.toDouble(), interpolatedVec.y.toDouble(), interpolatedVec.z.toDouble()).color(color.r, color.b, color.g, color.a).next()

        }, SmoothLineShader, state = GlRenderState(lineWidth = 2.0f, lineSmooth = true), shaderData = SmoothLineShader.SmoothLineShaderUniforms(2.0f))
    }

    val updateHandler = handler<PlayerTickEvent> {
        if (player.x == lastPosX && player.y == lastPosY && player.z == lastPosZ) {
            return@handler
        }

        lastPosX = player.x
        lastPosY = player.y
        lastPosZ = player.z

        synchronized(positions) {
            positions.addAll(listOf(player.x, player.y, player.z))
        }
    }

}
