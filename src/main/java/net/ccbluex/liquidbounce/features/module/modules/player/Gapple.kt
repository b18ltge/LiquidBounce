/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Items
import net.minecraft.item.ItemAppleGold
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.server.S09PacketHeldItemChange
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.potion.Potion
import net.minecraft.util.MathHelper
import java.util.*
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumRarity
import net.minecraft.util.BlockPos

object Gapple : Module("Gapple", ModuleCategory.PLAYER) {

    private val percent by FloatValue("HealthPercent", 75.0f, 1.0f..100.0f)
    private val min by IntegerValue("MinDelay", 75, 1..1000)
    private val max by IntegerValue("MaxDelay", 125, 1..1000)
    private val regenSec by FloatValue("MinRegenSec", 4.6f, 0.0f..10.0f)
	private val eatingDelay by IntegerValue("BeforeEatingDelay", 125, 1..1000)
    private val groundCheck by BoolValue("OnlyOnGround", false)
    private val waitRegen by BoolValue("WaitRegen", true)
    private val invCheck by BoolValue("InvCheck", false)
    private val absorpCheck by BoolValue("NoAbsorption", true)
	private val enchantedOnlyValue by BoolValue("EnchantedOnly", false)
    private val fastEatValue by BoolValue("FastEat", false)
    private val eatDelayValue by IntegerValue("FastEatDelay", 14, 0..35) { fastEatValue }
	private val sneakValue by BoolValue("Sneak", true)
    val timer = MSTimer()
	val eatingTimer = MSTimer()
    private var eating = -1
    var delay = 0
    var isDisable = false
    var tryHeal = false
    var prevSlot = -1
    var switchBack = false
	var isEating = false
    override fun onEnable() {
        eating = -1
		prevSlot = -1
		switchBack = false
		isEating = false
        timer.reset()
        isDisable = false
        tryHeal = false
        delay = MathHelper.getRandomIntegerInRange(Random(), min, max)
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        isDisable = true
        tryHeal = false
    }
	
    override fun onDisable() {
        if (eating != 1) {
			mc.playerController.onStoppedUsingItem(mc.thePlayer as EntityPlayer)
			KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false)
			if (sneakValue) {
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false)
			}
		}
    }
	

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
		
		if (packet is C16PacketClientStatus && eating > -1) {
			event.cancelEvent()
		}

        if (eating != -1 && packet is C03PacketPlayer && isEating) {
            eating++
        } else if (packet is S09PacketHeldItemChange || packet is C09PacketHeldItemChange) {
			eating = -1
			mc.playerController.onStoppedUsingItem(mc.thePlayer as EntityPlayer)
			KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false)
			if (sneakValue) {
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false)
			}
		}
		
    }
	
	fun findGapple(enchantedOnly: Boolean) : Int {
		for (i in 44 downTo 36) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack;

            if (stack != null && stack.item == Items.golden_apple && (!enchantedOnly || stack.item.getRarity(stack) == EnumRarity.EPIC)) {
				return i;
			}
        }
        return -1;
	}

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (tryHeal) {
			if (eating == 0 && eatingTimer.hasTimePassed(eatingDelay)) {
				mc.rightClickMouse()
				//sendPacket(C08PacketPlayerBlockPlacement(
                //    BlockPos(-1, -1, -1), 255, mc.thePlayer.heldItem, 0f, 0f, 0f
                //))
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true)
				isEating = true
			}
		
            if (eating == -1) {
				val gappleInHotbar = findGapple(enchantedOnlyValue)
				if(gappleInHotbar == -1) {
					tryHeal = false
					return
				}
				if (prevSlot == -1)
					prevSlot = mc.thePlayer.inventory.currentItem
					
				if (mc.thePlayer.inventory.currentItem != gappleInHotbar - 36) {
					mc.thePlayer.inventory.currentItem = gappleInHotbar - 36
				}
				eating = 0
				if (sneakValue) {
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true)
				}
				eatingTimer.reset()
            } else if (eating > 35 || (fastEatValue  && eating > eatDelayValue )) {
				repeat(35 - eating) {
                    mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
                }
				isEating = false
				mc.playerController.onStoppedUsingItem(mc.thePlayer as EntityPlayer)
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false)
				if (sneakValue) {
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false)
				}
                timer.reset()
				delay = MathHelper.getRandomIntegerInRange(Random(), min, max)
                tryHeal = false
            }
        }
        if (mc.thePlayer.ticksExisted <= 10 && isDisable) {
            isDisable = false
        }
        val absorp = MathHelper.ceiling_double_int(mc.thePlayer.absorptionAmount.toDouble())


        if (!tryHeal && prevSlot != -1) {
            if (!switchBack) {
                switchBack = true
                return
            }
            mc.thePlayer.inventory.currentItem = prevSlot
			eating = -1
            prevSlot = -1
            switchBack = false
        }

        if ((groundCheck && !mc.thePlayer.onGround) || (invCheck && mc.currentScreen is GuiContainer) || (absorp > 0 && absorpCheck))
            return
        if (waitRegen && mc.thePlayer.isPotionActive(Potion.regeneration) && mc.thePlayer.getActivePotionEffect(Potion.regeneration).duration > regenSec * 20.0f)
            return
        if (!isDisable && (mc.thePlayer.health <= (percent / 100.0f) * mc.thePlayer.maxHealth) && timer.hasTimePassed(delay.toLong())) {
            if (tryHeal)
                return
            tryHeal = true
        }
    }

    override val tag: String
        get() = regenSec.toString()
} 
