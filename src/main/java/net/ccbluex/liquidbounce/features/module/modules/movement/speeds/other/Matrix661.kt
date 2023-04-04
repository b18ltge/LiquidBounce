/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.math.sqrt

class Matrix661 : SpeedMode("Matrix661") {

	private var recX = 0.0
    private var recZ = 0.0
    
    override fun onUpdate() {
	    val speed = LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed? ?: return
		if (speed.usePreMotion.get()) return
        mc.thePlayer.jumpMovementFactor = 0.0266f
        if (!mc.thePlayer.onGround) {
            mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
            if (MovementUtils.speed < 0.217) {
                MovementUtils.strafe(0.217f)
                mc.thePlayer.jumpMovementFactor = 0.0269f
            }
        }
        if (mc.thePlayer.motionY < 0) {
            timer(1.09f)
            if (mc.thePlayer.fallDistance > 1.4)
                timer(1.0f)
        } else {
            timer(0.95f)
        }
        if (mc.thePlayer.onGround && MovementUtils.isMoving) {
            mc.gameSettings.keyBindJump.pressed = false
            timer(1.03f)
            mc.thePlayer.jump()
            if (mc.thePlayer.movementInput.moveStrafe <= 0.01 && mc.thePlayer.movementInput.moveStrafe >= -0.01) {
                MovementUtils.strafe((MovementUtils.speed * 1.0071).toFloat())
            }
        } else if (!MovementUtils.isMoving) {
            timer(1.0f)
        }
        if (MovementUtils.speed < 0.22)
            MovementUtils.strafe()
    }
    
    override fun onMotion() {}
	@EventTarget
	fun onPreMotion() {
		val speed = LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed? ?: return
        if (!speed.usePreMotion.get()) return
        mc.thePlayer.jumpMovementFactor = 0.0266f
        if (!mc.thePlayer.onGround) {
            mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
            if (MovementUtils.speed < 0.217) {
                MovementUtils.strafe(0.217f)
                mc.thePlayer.jumpMovementFactor = 0.0269f
            }
        }
        if (mc.thePlayer.motionY < 0) {
            timer(1.09f)
            if (mc.thePlayer.fallDistance > 1.4)
                timer(1.0f)
        } else {
            timer(0.95f)
        }
        if (mc.thePlayer.onGround && MovementUtils.isMoving) {
            mc.gameSettings.keyBindJump.pressed = false
            timer(1.03f)
            mc.thePlayer.jump()
            if (mc.thePlayer.movementInput.moveStrafe <= 0.01 && mc.thePlayer.movementInput.moveStrafe >= -0.01) {
                MovementUtils.strafe((MovementUtils.speed * 1.0071).toFloat())
            }
        } else if (!MovementUtils.isMoving) {
            timer(1.0f)
        }
        if (MovementUtils.speed < 0.22)
            MovementUtils.strafe()
    }
    override fun onMove(event: MoveEvent) {}
	@EventTarget
	fun onPacket(event: PacketEvent) {
		val packet = event.packet

		val speed = LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed? ?: return
        if (packet is S12PacketEntityVelocity && speed.veloBoostValue.get()) {
            if (mc.thePlayer == null || (mc.theWorld?.getEntityByID(packet.entityID) ?: return) != mc.thePlayer) {
                return
            }
            event.cancelEvent()

            recX = packet.motionX / 8000.0
            recZ = packet.motionZ / 8000.0
            if (sqrt(recX * recX + recZ * recZ) > MovementUtils.speed) {
                MovementUtils.strafe(sqrt(recX * recX + recZ * recZ).toFloat())
                mc.thePlayer.motionY = packet.motionY / 8000.0
            }

            MovementUtils.strafe((MovementUtils.speed * 1.1).toFloat())
        }
	}
    override fun onDisable() {
        mc.thePlayer.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
    }
	
	private fun timer(value: Float) {
		val speed = LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed? ?: return
        if(speed.timerBoostValue.get()) {
            mc.timer.timerSpeed = value
        }
    }
}
