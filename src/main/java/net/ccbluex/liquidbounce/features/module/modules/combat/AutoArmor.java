/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.item.ArmorComparator;
import net.ccbluex.liquidbounce.utils.item.ArmorPiece;
import net.ccbluex.liquidbounce.utils.item.ItemUtils;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.ccbluex.liquidbounce.utils.MovementUtils.isMoving;

@ModuleInfo(name = "AutoArmor", description = "Automatically equips the best armor in your inventory.", category = ModuleCategory.COMBAT)
public class AutoArmor extends Module {

    public static final ArmorComparator ARMOR_COMPARATOR = new ArmorComparator();
    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 200, 0, 400) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int minDelay = minDelayValue.get();

            if (minDelay > newValue) set(minDelay);
        }
    };
    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 100, 0, 400) {

        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int maxDelay = maxDelayValue.get();

            if (maxDelay < newValue) set(maxDelay);
        }

        @Override
        public boolean isSupported() {
            return !maxDelayValue.isMinimal();
        }
    };

    private final BoolValue invOpenValue = new BoolValue("InvOpen", false);
    private final BoolValue simulateInventory = new BoolValue("SimulateInventory", true) {
        @Override
        public boolean isSupported() {
            return !invOpenValue.get();
        }
    };
    private final BoolValue noMoveValue = new BoolValue("NoMove", false);
    private final IntegerValue itemDelayValue = new IntegerValue("ItemDelay", 0, 0, 5000);
    private final BoolValue hotbarValue = new BoolValue("Hotbar", true);
    private final BoolValue smartValue = new BoolValue("SmartMode", false); 
	private final FloatValue expectedDamageValue = new FloatValue("ExpectedDamage", 18.25f, 0f, 50f) {
        @Override
        public boolean isSupported() {
            return smartValue.get();
        }
    };
	private final FloatValue minHealthValue = new FloatValue("MinHealth", 16f, 0f, 20f) {
		@Override
        public boolean isSupported() {
            return smartValue.get();
        }
    };
    private final BoolValue ignoreHelmetsValue = new BoolValue("IgnoreHelmets", false) {
        @Override
        public boolean isSupported() {
            return !smartValue.get();
        }
    };

    private long delay;

    private boolean locked = false;
    
    private int disabledArmorSlot = -1;	//0..3
	
	private boolean canTakeOffArmor = true;
    
    private int getTheLeastDurabilitySlot() {
		int[] armorDurability = new int[4];

        for (int i = 0; i < 36; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemStack != null && itemStack.getItem() instanceof ItemArmor) {
                final ItemArmor item = (ItemArmor) itemStack.getItem();
                final float unbreakingMutliplier = 1f / (0.6f + 0.4f / (ItemUtils.getEnchantment(itemStack, Enchantment.protection) + 1));
				armorDurability[item.armorType] += (item.getMaxDamage(itemStack) - item.getDamage(itemStack)) * unbreakingMutliplier;
            }
        }
		
		for(int i = 0; i < 4; ++i) {
			final ItemStack itemStack = mc.thePlayer.inventory.armorItemInSlot(i);

            if (itemStack != null && itemStack.getItem() instanceof ItemArmor) {
                final ItemArmor item = (ItemArmor) itemStack.getItem();
                final float unbreakingMutliplier = 1f / (0.6f + 0.4f / (ItemUtils.getEnchantment(itemStack, Enchantment.protection) + 1));
				armorDurability[item.armorType] += (item.getMaxDamage(itemStack) - item.getDamage(itemStack)) * unbreakingMutliplier;
            }
		}

        int minID = -1;
        int min = Integer.MAX_VALUE;
        for(int i = 0; i < armorDurability.length; ++i) {
            if (armorDurability[i] > 0 && armorDurability[i] < min) {
                min = armorDurability[i];
                minID = i;
            }
        }
        return minID; 
	}

    private boolean needEquip(int disabledArmorSlot) {
		if (mc.thePlayer.isUsingItem()) {
			Item usingItem = mc.thePlayer.getItemInUse().getItem();
			
			if (usingItem instanceof ItemFood || usingItem instanceof ItemBucketMilk || usingItem instanceof ItemPotion) {
				return true;
			}
		}
		
		if (canTakeOffArmor == false && mc.thePlayer.getHealth() < minHealthValue.get()) {
			return true;
		}
		canTakeOffArmor = true;
		
        int damageReductionAmount = 0;

        for(int i = 0; i < 4; ++i) {
            if (i == disabledArmorSlot) {
                continue;
            }

            final ItemStack itemStack = mc.thePlayer.inventory.armorItemInSlot(3 - i);

            if (itemStack == null)
                continue;

            if (itemStack.getItem() instanceof ItemArmor) {
                final ItemArmor item = (ItemArmor) itemStack.getItem();
                damageReductionAmount += 4 * item.getArmorMaterial().getDamageReductionAmount(item.armorType);
            }

            damageReductionAmount += 2 * ItemUtils.getEnchantment(itemStack, Enchantment.protection);
        }
		
		float resistanceMultiplier = 1f;
        if (mc.thePlayer.isPotionActive(Potion.resistance)) {
            resistanceMultiplier -= Math.min(1.0f, 0.2f * (mc.thePlayer.getActivePotionEffect(Potion.resistance).getAmplifier() + 1));
        }
        
        return mc.thePlayer.getHealth() <= expectedDamageValue.get() / 100 * (100 - damageReductionAmount) * resistanceMultiplier;
    }

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        if (!InventoryUtils.CLICK_TIMER.hasTimePassed(delay) || mc.thePlayer == null || (mc.thePlayer.openContainer != null && mc.thePlayer.openContainer.windowId != 0))
            return;
        
        disabledArmorSlot = smartValue.get() ? getTheLeastDurabilitySlot() : -1;
		if (disabledArmorSlot != -1 && needEquip(disabledArmorSlot)) {
			canTakeOffArmor = false;
            disabledArmorSlot = -1;
		}

        // Find best armor
        final Map<Integer, List<ArmorPiece>> armorPieces = IntStream.range(0, 36).filter(i -> {
            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            return itemStack != null && itemStack.getItem() instanceof ItemArmor && (i < 9 || System.currentTimeMillis() - ((IMixinItemStack) (Object) itemStack).getItemDelay() >= itemDelayValue.get());
        }).mapToObj(i -> new ArmorPiece(mc.thePlayer.inventory.getStackInSlot(i), i)).collect(Collectors.groupingBy(ArmorPiece::getArmorType));

        final ArmorPiece[] bestArmor = new ArmorPiece[4];

        for (final Map.Entry<Integer, List<ArmorPiece>> armorEntry : armorPieces.entrySet()) {
            bestArmor[armorEntry.getKey()] = armorEntry.getValue().stream().max(ARMOR_COMPARATOR).orElse(null);
        }

        // Swap armor
        for (int i = ignoreHelmetsValue.get() ? 1 : 0; i < 4; i++) {
            int armorSlot = 3 - i;
			
 
            if (i == disabledArmorSlot) {
				final ItemStack itemStack = mc.thePlayer.inventory.armorItemInSlot(armorSlot);
				if (!ItemUtils.isStackEmpty(itemStack) && itemStack.getItem() instanceof ItemArmor) {
					moveOutArmor(8 - armorSlot);
					locked = true;
					return;
				}
				continue;
            }
			
			final ArmorPiece armorPiece = bestArmor[i];
			
			if (armorPiece == null) continue;

            final ArmorPiece oldArmor = new ArmorPiece(mc.thePlayer.inventory.armorItemInSlot(armorSlot), -1);

            if (ItemUtils.isStackEmpty(oldArmor.getItemStack()) || !(oldArmor.getItemStack().getItem() instanceof ItemArmor) || ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0) {
                if (!ItemUtils.isStackEmpty(oldArmor.getItemStack()) && move(8 - armorSlot, true)) {
                    locked = true;
                    return;
                }

                if (ItemUtils.isStackEmpty(mc.thePlayer.inventory.armorItemInSlot(armorSlot)) && move(armorPiece.getSlot(), false)) {
                    locked = true;
                    return;
                }
            }
        }

        locked = false;
    }

    public boolean isLocked() {
        return getState() && locked;
    }

    /**
     * Shift+Left-clicks the specified item
     *
     * @param item        Slot of the item to click
     * @param isArmorSlot
     * @return True if it is unable to move the item
     */
    private boolean move(int item, boolean isArmorSlot) {
        if (!isArmorSlot && item < 9 && hotbarValue.get() && !(mc.currentScreen instanceof GuiInventory)) {
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(item));
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(item).getStack()));
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            return true;
        } else if (!(noMoveValue.get() && isMoving()) && (!invOpenValue.get() || mc.currentScreen instanceof GuiInventory) && item != -1) {
            final boolean openInventory = simulateInventory.get() && !(mc.currentScreen instanceof GuiInventory);

            if (openInventory) mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));

            boolean full = isArmorSlot;

            if (full) {
                for (ItemStack iItemStack : mc.thePlayer.inventory.mainInventory) {
                    if (ItemUtils.isStackEmpty(iItemStack)) {
                        full = false;
                        break;
                    }
                }
            }

            if (full) {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, item, 1, 4, mc.thePlayer);
            } else {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, (isArmorSlot ? item : (item < 9 ? item + 36 : item)), 0, 1, mc.thePlayer);
            }

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if (openInventory)
                mc.getNetHandler().addToSendQueue(new C0DPacketCloseWindow());

            return true;
        }

        return false;
    }

    private boolean moveOutArmor(int item) {
		if (!(noMoveValue.get() && MovementUtils.isMoving()) && (!invOpenValue.get() || mc.currentScreen instanceof GuiInventory) && item != -1) {
            final boolean openInventory = simulateInventory.get() && !(mc.currentScreen instanceof GuiInventory);

            if (openInventory) mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));

            boolean full = true;

            for (ItemStack iItemStack : mc.thePlayer.inventory.mainInventory) {
                if (ItemUtils.isStackEmpty(iItemStack)) {
                    full = false;
                    break;
                }
            }

            if (full) {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, item, 1, 4, mc.thePlayer);
            } else {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, item, 0, 1, mc.thePlayer);
            }

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if (openInventory)
                mc.getNetHandler().addToSendQueue(new C0DPacketCloseWindow());
			
			return true;
        }
		return false;
	}
}
