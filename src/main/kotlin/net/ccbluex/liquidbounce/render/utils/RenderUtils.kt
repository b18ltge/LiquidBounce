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

package net.ccbluex.liquidbounce.render.utils

import net.ccbluex.liquidbounce.render.engine.tasks.Color4b
import net.ccbluex.liquidbounce.render.engine.tasks.makeBuffer
import net.minecraft.client.render.VertexFormat
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

fun drawBoxOutlineNew(box: Box, color: Color4b) = makeBuffer(VertexFormat.DrawMode.LINES) {
    it.vertex(box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()

    it.vertex(box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
}

fun drawBoxNew(box: Box, color: Color4b) = makeBuffer(VertexFormat.DrawMode.TRIANGLES) {
    it.vertex(box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()

    it.vertex(box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
    it.vertex(box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
}

fun drawBoxSide(box: Box, side: Direction, color: Color4b) = makeBuffer(VertexFormat.DrawMode.TRIANGLES) {
    when (side) {
        Direction.SOUTH -> {
            it.vertex(box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
        }
        Direction.WEST -> {
            it.vertex(box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
        }
        Direction.NORTH -> {
            it.vertex(box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
        }
        Direction.EAST -> {
            it.vertex(box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
        }
        Direction.UP -> {
            it.vertex(box.minX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.minX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.maxY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.maxY, box.minZ).color(color.r, color.g, color.b, color.a).next()
        }
        Direction.DOWN -> {
            it.vertex(box.minX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.minX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.minY, box.minZ).color(color.r, color.g, color.b, color.a).next()
            it.vertex(box.maxX, box.minY, box.maxZ).color(color.r, color.g, color.b, color.a).next()
        }
    }
}
