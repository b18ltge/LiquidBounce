/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.TextValue
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.world.WorldSettings
import net.minecraft.init.Items
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class KeyPearl : Module("KeyPearl", ModuleCategory.PLAYER) {

	private val midClickValue by BoolValue("MidClick", true)
	private val keyValue by TextValue("KeyName", "X") { !midClickValue }
	
	private var wasMouseDown = false
	private var wasKeyDown = false
	
	private fun throwEnderPearl() {
		val pearlInHotbar = InventoryUtils.findItem(36, 45, Items.ender_pearl)
		
		if (pearlInHotbar == null || pearlInHotbar == -1) {
			displayChatMessage("§c§lError: §aThere are no ender pearls in the hotbar.")
			return
		}
		
        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(pearlInHotbar!! - 36))
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
	}
	
	
	@EventTarget
    fun onRender(event: Render2DEvent) {
        if (mc.currentScreen != null)
            return
			
		if (mc.currentScreen != null || mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR 
			|| mc.playerController.currentGameType == WorldSettings.GameType.CREATIVE) return

        if (midClickValue && !wasMouseDown && Mouse.isButtonDown(2)) {
            throwEnderPearl()
        } else if (!midClickValue && !wasKeyDown && Keyboard.isKeyDown(Keyboard.getKeyIndex(keyValue.toUpperCase()))) {
			throwEnderPearl()
		}
        wasMouseDown = Mouse.isButtonDown(2)
		wasKeyDown = Keyboard.isKeyDown(Keyboard.getKeyIndex(keyValue.toUpperCase()))
    }
}
