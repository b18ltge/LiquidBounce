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
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.GlRenderState
import net.ccbluex.liquidbounce.render.engine.RenderEngine
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.tasks.Color4b
import net.ccbluex.liquidbounce.render.engine.tasks.Vec3
import net.ccbluex.liquidbounce.render.engine.tasks.VertexFormatRenderTask
import net.ccbluex.liquidbounce.render.engine.tasks.makeBuffer
import net.ccbluex.liquidbounce.render.engine.utils.vertex
import net.ccbluex.liquidbounce.render.shaders.ColoredPrimitiveShader
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.client.render.VertexFormat

/**
 * Rotations module
 *
 * Allows you to see server-sided rotations.
 */

object ModuleRotations : Module("Rotations", Category.RENDER) {

    val showRotationVector by boolean("ShowRotationVector", false)

    val renderHandler = handler<EngineRenderEvent> {
        if (!showRotationVector) {
            return@handler
        }

        val serverRotation = RotationManager.serverRotation ?: return@handler

        val vertexFormat = PositionColorVertexFormat()

        vertexFormat.initBuffer(2)

        val camera = mc.gameRenderer.camera

        val eyeVector = Vec3(0.0, 0.0, 1.0)
            .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
            .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat()) + Vec3(camera.pos) + Vec3(0.0, 0.0, -1.0)

        RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, VertexFormatRenderTask(makeBuffer(VertexFormat.DrawMode.LINE_STRIP) {
            it.vertex(eyeVector).color(Color4b.WHITE.toRGBA()).next()
            it.vertex(eyeVector + Vec3(serverRotation.rotationVec * 2.0)).color(Color4b.WHITE.toRGBA()).next()
        }, ColoredPrimitiveShader, state = GlRenderState(lineWidth = 2.0f, lineSmooth = true)))
    }

}
