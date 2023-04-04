/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.settings.GameSettings

@ModuleInfo(name = "AutoJump", description = "Allows you to jump automatically", category = ModuleCategory.MOVEMENT)
class AutoJump : Module() {

	private val delayValue = IntegerValue("Delay", 625, 0, 1000)
	
	private val msTimer = MSTimer()
	
	@EventTarget
    fun onUpdate(event: UpdateEvent) {
		if(!msTimer.hasTimePassed(delayValue.get())) {
			return
		}
		
		if (mc.thePlayer.onGround && !mc.thePlayer.isInWater && !mc.thePlayer.isInLava && !mc.thePlayer.isInWeb && !GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
            mc.thePlayer.jump()
			msTimer.reset()
        }
	}
}
