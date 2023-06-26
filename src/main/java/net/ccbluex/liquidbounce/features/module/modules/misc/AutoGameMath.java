/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 * Author: CaT@
 * Created: 28.03.2023
 */
package net.ccbluex.liquidbounce.features.module.modules.misc;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class AutoGameMath extends Module {
	
	public AutoGameMath() {
        super("AutoGameMath", ModuleCategory.MISC);
    }

    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 1000, 0, 5000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int minDelayValueObject = minDelayValue.get();

            if(minDelayValueObject > newValue)
                set(minDelayValueObject);
            delay = TimeUtils.INSTANCE.randomDelay(minDelayValue.get(), maxDelayValue.get());
        }
    };

    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 500, 0, 5000) {

        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int maxDelayValueObject = maxDelayValue.get();

            if(maxDelayValueObject < newValue)
                set(maxDelayValueObject);
            delay = TimeUtils.INSTANCE.randomDelay(minDelayValue.get(), maxDelayValue.get());
        }

        @Override
        public boolean isSupported() {
            return !maxDelayValue.isMinimal();
        };
    };

    private final ListValue modeValue = new ListValue("Mode", new String[]{"General"}, "General");

    private final MSTimer msTimer = new MSTimer();
    private long delay = TimeUtils.INSTANCE.randomDelay(minDelayValue.get(), maxDelayValue.get());
	private boolean isWaiting = false;
	private int numberToSend;

	@EventTarget
	public void onUpdate(UpdateEvent event) {
		if(isWaiting) {
			if (!msTimer.hasTimePassed(delay))
				return;

			mc.thePlayer.sendChatMessage("" + numberToSend);
			isWaiting = false;
			return;
        }

	}

    @EventTarget
    public void onPacket(PacketEvent event) {
        final Packet packet = event.getPacket();

        if (!(packet instanceof S02PacketChat)) {
          return;
        }

        String chatMessage;
        try {
          chatMessage = ((S02PacketChat) packet).getChatComponent().getUnformattedText().toLowerCase();
          if (chatMessage == null) return;
        } catch (Exception ex) {
          return;
        }

        int sum = 0;
        if (modeValue.get().equalsIgnoreCase("General")) {

          final Pattern pattern = Pattern.compile("[0-9]+ [-+] [0-9]+");
          final Matcher matcher = pattern.matcher(chatMessage);

          if (!matcher.find()) 
            return;

          final String[] parts = chatMessage.substring(matcher.start(), matcher.end()).split("[-+ ]+");
		  
		  final boolean add = chatMessage.contains("+");

		  if (!add) sum = Integer.valueOf(parts[0]) * 2;
		  for(final String part : parts) {
			sum += add ? Integer.valueOf(part) : Integer.valueOf(part) * -1;
		  }
        }

        numberToSend = sum;
        isWaiting = true;
        msTimer.reset();
        delay = TimeUtils.INSTANCE.randomDelay(minDelayValue.get(), maxDelayValue.get());
    }
}
