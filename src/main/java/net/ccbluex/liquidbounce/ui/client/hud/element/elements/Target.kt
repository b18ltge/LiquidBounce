/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawScaledCustomSizeModalRect
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

import net.minecraft.client.renderer.GlStateManager.*
import org.lwjgl.opengl.GL11.glPopMatrix
import org.lwjgl.opengl.GL11.glPushMatrix
import net.ccbluex.liquidbounce.utils.item.ItemUtils;
import net.minecraft.item.ItemAppleGold
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemBow
import net.minecraft.item.EnumRarity
import net.minecraft.client.renderer.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;

/**
 * A target hud
 */
@ElementInfo(name = "Target")
class Target : Element() {

    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))
	private val absorptionValue = BoolValue("Absorption", false)
	private val storageTime = IntegerValue("GappleCountStorageTime", 15, 5, 60)
	
	private val entityMap = HashMap<EntityPlayer, Pair<Int, Long>>()
	private val entitiesToRemove = HashSet<EntityPlayer>()

    override fun drawElement(): Border {
		updateMap()
        val target = KillAura.target

        if (target is EntityPlayer) {
			if (!entityMap.containsKey(target)) {
				entityMap.put(target, Pair(0, System.currentTimeMillis()))
			} else {
				entityMap.put(target, Pair(entityMap.get(target)!!.first, System.currentTimeMillis()))
			}
			
		
            val width = (38 + (target.name?.let(Fonts.font40::getStringWidth) ?: 0))
                    .coerceAtLeast(118)
                    .toFloat()

            // Draw rect box
            drawBorderedRect(0F, 0F, width, 61F, 3F, Color.BLACK.rgb, Color.BLACK.rgb)

            // Red bar
            drawRect(0F, 59F, width, 61F, Color(252, 96, 66).rgb)

            target.name?.let { Fonts.font40.drawString(it, 36, 3, 0xffffff) }
            Fonts.font35.drawString("Distance: ${decimalFormat.format(mc.thePlayer.getDistanceToEntityBox(target))}", 36, 15, 0xffffff)
			val displayHealth = if (absorptionValue.get()) target.health + target.absorptionAmount else target.health
			Fonts.font35.drawString("Health: ", 36, 24, 0xffffff)
			Fonts.font35.drawString("${decimalFormat.format(displayHealth)}", 66, 24, getHealthColor(displayHealth))

            // Draw info
            val playerInfo = mc.netHandler.getPlayerInfo(target.uniqueID)
            if (playerInfo != null) {
                Fonts.font35.drawString("Ping: ${playerInfo.responseTime.coerceAtLeast(0)} ms",
                        36, 33, 0xffffff)

                // Draw head
                val locationSkin = playerInfo.locationSkin
                drawHead(locationSkin, 30, 30)
            }
			
			// Draw Equipment
			RenderHelper.enableGUIStandardItemLighting()
				
			// Held item
			val heldItem = target.getEquipmentInSlot(0)
			if (heldItem != null) {
				mc.renderItem.renderItemAndEffectIntoGUI(heldItem, 20, 42)
				mc.renderItem.renderItemOverlays(mc.fontRendererObj, heldItem, 20, 42)
				drawEnchants(heldItem, 20, 42)
				
				// update gapple count
				if (heldItem.item is ItemAppleGold && heldItem.item.getRarity(heldItem) == EnumRarity.EPIC) {
					entityMap.put(target, Pair(heldItem.stackSize, entityMap.get(target)!!.second))
				}
			}
			
			// Approximate gapple count
			val gappleCount = entityMap.get(target)!!.first
			val gapples = ItemUtils.createItem("golden_apple " + gappleCount + " 1")

			mc.renderItem.renderItemAndEffectIntoGUI(gapples, 36, 42)

			if (gappleCount == 0) {
				GL11.glDisable(GL11.GL_DEPTH_TEST)
				GL11.glDepthMask(false)
				Fonts.font35.drawString("??", 45f, 53f, 0xffffff, true)
				GL11.glDepthMask(true)
				GL11.glEnable(GL11.GL_DEPTH_TEST)
			} else {
				mc.renderItem.renderItemOverlays(mc.fontRendererObj, gapples, 36, 42)
			}


			// Armor
			for (index in 1..4) {
				val stack = target.getEquipmentInSlot(index)
				if (stack == null) {
					continue
				}

				val x = 52 + (4 - index) * 16
				mc.renderItem.renderItemAndEffectIntoGUI(stack, x, 42)
				mc.renderItem.renderItemOverlays(mc.fontRendererObj, stack, x, 42)
				drawEnchants(stack, x, 42)
			}
				

			RenderHelper.disableStandardItemLighting()
			enableAlpha()
			disableBlend()
			disableLighting()
        }

        return Border(0F, 0F, 120F, 61F)
    }

    private fun drawHead(skin: ResourceLocation, width: Int, height: Int) {
        glColor4f(1F, 1F, 1F, 1F)
        mc.textureManager.bindTexture(skin)
        drawScaledCustomSizeModalRect(2, 2, 8F, 8F, 8, 8, width, height,
                64F, 64F)
    }

    fun updateMap() {
		for(entity in entityMap) {
			if (System.currentTimeMillis() - entity.value.second > storageTime.get() * 1000) {
				entitiesToRemove.add(entity.key)
			}
		}
		for(entity in entitiesToRemove) {
			entityMap.remove(entity)
		}
		entitiesToRemove.clear()
	}
	
	fun drawEnchants(stack : ItemStack, x : Int, posY : Int) {
		var y = posY
        RenderHelper.disableStandardItemLighting();
        disableDepth();
        disableBlend();
        resetColor();
        if (stack.item is ItemArmor || stack.item is ItemSkull) {
            val prot = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
            val unb = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            val thorn = EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack);
			val feather = EnchantmentHelper.getEnchantmentLevel(Enchantment.featherFalling.effectId, stack);
            if (prot > 0) {
                Fonts.font24.drawString("P" + prot.toString(), x, y, -1);
                y += 5;
            }
            if (unb > 0) {
                Fonts.font24.drawString("U" + unb.toString(), x, y, -1);
                y += 5;
            }
            if (thorn > 0) {
                Fonts.font24.drawString("T" + thorn.toString(), x, y, -1);
                y += 5;
            }
			if (feather > 0) {
                Fonts.font24.drawString("F" + feather.toString(), x, y, -1);
            }
        }
        else if (stack.item is ItemBow) {
            val power = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
            val punch = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack);
            val flame = EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack);
            val unb = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            if (power > 0) {
                Fonts.font24.drawString("Pow" + power.toString(), x, y, -1);
                y += 5;
            }
            if (punch > 0) {
                Fonts.font24.drawString("Pun" + punch.toString(), x, y, -1);
                y += 5;
            }
            if (flame > 0) {
                Fonts.font24.drawString("F" + flame.toString(), x, y, -1);
                y += 5;
            }
            if (unb > 0) {
                Fonts.font24.drawString("U" + unb.toString(), x, y, -1);
                y += 5;
            }
        }
        else if (stack.item is ItemSword) {
            val sharp = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
            val kb = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack);
            val fire = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
            val unb = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            if (sharp > 0) {
                Fonts.font24.drawString("S" + sharp.toString(), x, y, -1);
                y += 5;
            }
            if (kb > 0) {
                Fonts.font24.drawString("K" + kb.toString(), x, y, -1);
                y += 5;
            }
            if (fire > 0) {
                Fonts.font24.drawString("F" + fire.toString() , x, y, -1);
                y += 5;
            }
            if (unb > 0) {
                Fonts.font24.drawString("U" + unb.toString(), x, y, -1);
            }
        }
        enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
    }
	
	
	fun getHealthColor(health : Float) : Int {
		if (health >= 16) {
			return 0x00ff00
		} else if (health >= 12) {
			return 0xbbff00
		} else if (health >= 8) {
			return 0xffff00
		} else if (health >= 4) {
			return 0xffbb00
		} else return 0xff0000
	}
}