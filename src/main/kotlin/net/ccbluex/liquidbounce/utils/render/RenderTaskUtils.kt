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

package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.render.engine.CullingMode
import net.ccbluex.liquidbounce.render.engine.GlRenderState
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.tasks.VertexFormatRenderTask
import net.ccbluex.liquidbounce.render.shaders.ColoredPrimitiveShader
import net.ccbluex.liquidbounce.render.shaders.InstancedColoredPrimitiveShader
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.VertexFormat

fun espBoxInstancedRenderTask(
    instanceBuffer: PositionColorVertexFormat,
    buffer: Pair<VertexFormat.DrawMode, (BufferBuilder) -> Unit>,
) = VertexFormatRenderTask(
    buffer,
    InstancedColoredPrimitiveShader,
    perInstance = instanceBuffer,
    state = GlRenderState(culling = CullingMode.BACKFACE_CULLING)
)

fun espBoxRenderTask(
    buffer: Pair<VertexFormat.DrawMode, (BufferBuilder) -> Unit>
) = VertexFormatRenderTask(
    buffer,
    ColoredPrimitiveShader,
    state = GlRenderState(culling = CullingMode.BACKFACE_CULLING)
)

fun espBoxInstancedOutlineRenderTask(
    instanceBuffer: PositionColorVertexFormat,
    buffer: Pair<VertexFormat.DrawMode, (BufferBuilder) -> Unit>
) = VertexFormatRenderTask(
    buffer,
    InstancedColoredPrimitiveShader,
    perInstance = instanceBuffer,
    state = GlRenderState(lineWidth = 2.0f, lineSmooth = true)
)
