/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 * Author: CaT@
 * Created: 07.05.2023
 */
package net.ccbluex.liquidbounce.features.module.modules.misc;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.value.TextValue;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;


public class AutoMessageColor extends Module {
	
	public AutoMessageColor() {
        super("AutoMessageColor", ModuleCategory.MISC);
    }

    private final TextValue textColorValue = new TextValue("ColorCode", "c", null);


    @EventTarget
    public void onPacket(PacketEvent event) {
        final Packet packet = event.getPacket();

        if (!(packet instanceof C01PacketChatMessage) || mc.thePlayer == null) {
          return;
        }
		
		final String message = ((C01PacketChatMessage) packet).getMessage();
		
		if (message.startsWith("/") || message.startsWith(".") || message.startsWith("&")) {
			return;
		}
			
		event.cancelEvent();
		mc.thePlayer.sendChatMessage("&" + textColorValue.get() + message);
    }
}
