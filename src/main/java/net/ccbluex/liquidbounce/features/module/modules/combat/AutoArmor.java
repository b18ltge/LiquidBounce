/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.TickEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.item.ArmorComparator;
import net.ccbluex.liquidbounce.utils.item.ArmorPiece;
import net.ccbluex.liquidbounce.utils.item.ItemUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket;
import static net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets;
import static net.ccbluex.liquidbounce.utils.item.ItemUtilsKt.isEmpty;
import static net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT;
import static net.ccbluex.liquidbounce.utils.item.ItemUtilsKt.getEnchantmentLevel;

public class AutoArmor extends Module {

    public AutoArmor() {
        super("AutoArmor", ModuleCategory.COMBAT);
    }
	
	private static final Logger logger = LogManager.getLogger("LiquidBounce");
	
	private	static final List<Integer> armorSlots = Arrays.asList(1, 2, 3, 0);

    public static final ArmorComparator ARMOR_COMPARATOR = new ArmorComparator();
    private final IntegerValue maxTicksValue = new IntegerValue("MaxTicks", 4, 0, 10) {
        @Override
        protected Integer onChange(final Integer oldValue, final Integer newValue) {
            final int minDelay = minTicksValue.get();

            return newValue > minDelay ? newValue : minDelay;
        }
    };
    private final IntegerValue minTicksValue = new IntegerValue("MinTicks", 2, 0, 10) {

        @Override
        protected Integer onChange(final Integer oldValue, final Integer newValue) {
            final int maxDelay = maxTicksValue.get();

            return newValue < maxDelay ? newValue : maxDelay;
        }

        @Override
        public boolean isSupported() {
            return !maxTicksValue.isMinimal();
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
    private final IntegerValue itemTicksValue = new IntegerValue("ItemTicks", 0, 0, 20);
    private final BoolValue hotbarValue = new BoolValue("Hotbar", true);
    public final BoolValue saveArmorValue = new BoolValue("SaveArmor", false);
    public final IntegerValue saveArmorThresholdValue = new IntegerValue("SaveArmorThreshold", 9, 0, 50) {
        @Override
        public boolean isSupported() {
            return saveArmorValue.get();
        }
    };
    public final IntegerValue saveArmorProtectionValue = new IntegerValue("SaveArmorProtection", 0, 0, 4) {
        @Override
        public boolean isSupported() {
            return saveArmorValue.get();
        }
    };
    private final BoolValue smartValue = new BoolValue("SmartMode", false);
    private final BoolValue partsValue = new BoolValue("TakeOffTwoParts", false) {
        @Override
        public boolean isSupported() {
            return smartValue.get();
        }
    };
    private final IntegerValue swordDamageValue = new IntegerValue("SwordDamage", 7, 4, 7) {
        @Override
        public boolean isSupported() {
            return smartValue.get();
        }
    };
    private final IntegerValue sharpnessValue = new IntegerValue("Sharpness", 5, 0, 10) {
        @Override
        public boolean isSupported() {
            return smartValue.get();
        }
    };
    private final IntegerValue minHealthValue = new IntegerValue("MinHealth", 16, 0, 20) {
        @Override
        public boolean isSupported() {
            return smartValue.get();
        }
    };
    private final BoolValue ignoreHelmetsValue = new BoolValue("IgnoreHelmets", false);
    private final BoolValue debugValue = new BoolValue("Debug", false);

    private long delay;

    private boolean locked = false;
    
    private int disabledArmorSlot = -1; //0..3

    private int secondDisabledArmorSlot = -1;

    private boolean canTakeOffArmor = true;

    private final MSTimer timer = new MSTimer();
    
    private int getTheLeastDurabilitySlot() {
        final int[] armorDurability = new int[4];

        for (int i = 0; i < 36; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemStack != null && itemStack.getItem() instanceof ItemArmor) {
                final ItemArmor item = (ItemArmor) itemStack.getItem();
                final float unbreakingMutliplier = 1f / (0.6f + 0.4f / (getEnchantmentLevel(itemStack, Enchantment.unbreaking) + 1));
                armorDurability[item.armorType] += (item.getMaxDamage(itemStack) - item.getDamage(itemStack)) * unbreakingMutliplier;
            }
        }

        for(int i = 0; i < 4; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.armorItemInSlot(i);

            if (itemStack != null && itemStack.getItem() instanceof ItemArmor) {
                final ItemArmor item = (ItemArmor) itemStack.getItem();
                final float unbreakingMutliplier = 1f / (0.6f + 0.4f / (getEnchantmentLevel(itemStack, Enchantment.unbreaking) + 1));
                armorDurability[item.armorType] += (item.getMaxDamage(itemStack) - item.getDamage(itemStack)) * unbreakingMutliplier;
            }
        }

        if (ignoreHelmetsValue.get()) {
            armorDurability[0] = 0;
        }
        final Map<Integer, Integer> armorDurabilityMap = new HashMap<Integer, Integer>();

        for(int i = 0; i < armorDurability.length; ++i) {
            armorDurabilityMap.put(i, armorDurability[i]);
        }

        final Object[] sortedEntries = armorDurabilityMap.entrySet().stream().sorted((entry1, entry2) -> {
            if (entry1.getValue() == 0 && entry2.getValue() > 0) 
                return 1;
            else if (entry1.getValue() > 0 && entry2.getValue() == 0)
                return -1;
            return Integer.compare(entry1.getValue(), entry2.getValue());
        }).toArray();

        disabledArmorSlot = ((Map.Entry<Integer, Integer>) sortedEntries[0]).getKey();
        if (partsValue.get()) {
            secondDisabledArmorSlot = ((Map.Entry<Integer, Integer>) sortedEntries[1]).getKey();
            if ((disabledArmorSlot == 1 && secondDisabledArmorSlot == 2) || (disabledArmorSlot == 2 && secondDisabledArmorSlot == 1)) {
                secondDisabledArmorSlot = ((Map.Entry<Integer, Integer>) sortedEntries[2]).getKey();
            }
        }

        return disabledArmorSlot;
    }

    private boolean needEquip() {
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
        int EPFpoints = 0;  // protection enchantments

        for(int i = 0; i < 4; ++i) {
            if (i == disabledArmorSlot || (partsValue.get() && i == secondDisabledArmorSlot)) {
                continue;
            }

            final ItemStack itemStack = mc.thePlayer.inventory.armorItemInSlot(3 - i);

            if (itemStack == null)
                continue;

            if (itemStack.getItem() instanceof ItemArmor) {
                final ItemArmor item = (ItemArmor) itemStack.getItem();
                
                damageReductionAmount += 4 * item.getArmorMaterial().getDamageReductionAmount(item.armorType);
            }

            final int protectionLevel = getEnchantmentLevel(itemStack, Enchantment.protection);
            EPFpoints += (protectionLevel * protectionLevel + 6) / 4;
        }
    
        EPFpoints *= 2;


        float resistanceMultiplier = 1f;
        if (mc.thePlayer.isPotionActive(Potion.resistance)) {
            resistanceMultiplier -= Math.min(1.0f, 0.2f * (mc.thePlayer.getActivePotionEffect(Potion.resistance).getAmplifier() + 1));
        }

        final float expectedDamage = (1 + swordDamageValue.get()) * 1.5f + (sharpnessValue.get() * 1.25f);
        
        final float expectedPostDamage = expectedDamage * (100f - damageReductionAmount) / 100f * resistanceMultiplier * (100f - EPFpoints) / 100f;
        return mc.thePlayer.getHealth() <= expectedPostDamage;
    }

    private boolean handleArmor(final ArmorPiece[] bestArmor, final int i) {
        int armorSlot = 3 - i;

        // Take off armor
        if (smartValue.get()) {
            if (i == disabledArmorSlot || (partsValue.get() && i == secondDisabledArmorSlot)) {
                final ItemStack itemStack = mc.thePlayer.inventory.armorItemInSlot(armorSlot);
                if (!isEmpty(itemStack) && itemStack.getItem() instanceof ItemArmor) {
                    if (move(8 - armorSlot, true)) {
                        locked = true;
                        return true;
                    }
                }
                return false;
            }
        }

        final ArmorPiece armorPiece = bestArmor[i];

        if (armorPiece == null) return false;

        final ArmorPiece oldArmor = new ArmorPiece(mc.thePlayer.inventory.armorItemInSlot(armorSlot), -1);

        if (isEmpty(oldArmor.getItemStack()) || !(oldArmor.getItemStack().getItem() instanceof ItemArmor) || ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0) {
            if (!isEmpty(oldArmor.getItemStack()) && move(8 - armorSlot, true)) {
                locked = true;
                return true;
            }

            if (isEmpty(mc.thePlayer.inventory.armorItemInSlot(armorSlot)) && move(armorPiece.getSlot(), false)) {
                locked = true;
                return true;
            }
        }

        return false;
    }

    @EventTarget
    public void onTick(final TickEvent event) {
        if (!timer.hasTimePassed(delay * 50L) || mc.thePlayer == null || (mc.thePlayer.openContainer != null && mc.thePlayer.openContainer.windowId != 0))
            return;
        
        // Find armor to take off
        if (smartValue.get()) {
            disabledArmorSlot = getTheLeastDurabilitySlot();
            final boolean needEquip = needEquip();
            
            if (debugValue.get()) {
                logger.info("needEquip: " + needEquip);
                logger.info("disabledArmorSlot: " + disabledArmorSlot);
                logger.info("secondDisabledArmorSlot: " + secondDisabledArmorSlot);
                logger.info("health: " + mc.thePlayer.getHealth());
                logger.info("currentArmor: " + mc.thePlayer.inventory.armorItemInSlot(3) 
                                                + " " + mc.thePlayer.inventory.armorItemInSlot(2)
                                                + " " + mc.thePlayer.inventory.armorItemInSlot(1)
                                                + " " + mc.thePlayer.inventory.armorItemInSlot(0) + " = " + (mc.thePlayer.getTotalArmorValue() * 4));
                logger.info("hurttime: " + mc.thePlayer.hurtTime);
            }
            
            if (disabledArmorSlot != -1 && needEquip) {
                canTakeOffArmor = false;
                disabledArmorSlot = -1;
                secondDisabledArmorSlot = -1;
            }
        }

        // Find best armor
        final Map<Integer, List<ArmorPiece>> armorPieces = IntStream.range(0, 36).filter(i -> {
            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            return itemStack != null && itemStack.getItem() instanceof ItemArmor && (i < 9 || System.currentTimeMillis() - ((IMixinItemStack) (Object) itemStack).getItemDelay() >= itemTicksValue.get() * 50L);
        }).mapToObj(i -> new ArmorPiece(mc.thePlayer.inventory.getStackInSlot(i), i)).collect(Collectors.groupingBy(ArmorPiece::getArmorType));

        final ArmorPiece[] bestArmor = new ArmorPiece[4];

        for (final Map.Entry<Integer, List<ArmorPiece>> armorEntry : armorPieces.entrySet()) {
            bestArmor[armorEntry.getKey()] = armorEntry.getValue().stream().max(ARMOR_COMPARATOR).orElse(null);
        }

        // Swap armor
        for(final int i : armorSlots) {
            if (i == 0 && ignoreHelmetsValue.get()) continue;
            if (smartValue.get() && (i == disabledArmorSlot || (partsValue.get() && i == secondDisabledArmorSlot))) {
                continue;
            }
            if (handleArmor(bestArmor, i)) {
                return;
            }
        }
        
        if (smartValue.get() && disabledArmorSlot != -1 && handleArmor(bestArmor, disabledArmorSlot)) {
            return;
        }
        if (smartValue.get() && partsValue.get() && secondDisabledArmorSlot != -1 && handleArmor(bestArmor, secondDisabledArmorSlot)) {
            return;
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
            sendPackets(
                new C09PacketHeldItemChange(item),
                new C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(item).getStack()),
                new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem)
            );

            timer.reset();
            delay = TimeUtils.INSTANCE.randomDelay(minTicksValue.get(), maxTicksValue.get());

            return true;
        } else if (!(noMoveValue.get() && MovementUtils.INSTANCE.isMoving()) && (!invOpenValue.get() || mc.currentScreen instanceof GuiInventory) && item != -1) {
            final boolean openInventory = simulateInventory.get() && !(mc.currentScreen instanceof GuiInventory);

            if (openInventory) sendPacket(new C16PacketClientStatus(OPEN_INVENTORY_ACHIEVEMENT));

            boolean full = isArmorSlot;

            if (full) {
                for (ItemStack iItemStack : mc.thePlayer.inventory.mainInventory) {
                    if (isEmpty(iItemStack)) {
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

            timer.reset();
            delay = TimeUtils.INSTANCE.randomDelay(minTicksValue.get(), maxTicksValue.get());

            if (openInventory)
                sendPacket(new C0DPacketCloseWindow());

            return true;
        }

        return false;
    }

}
