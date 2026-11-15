package com.l7cor1s.viktoria.inventory;

import com.mojang.datafixers.util.Pair;
import com.l7cor1s.viktoria.entity.VikaBags;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.registry.VikaRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Laura's inventory: her equipment (armor and both hands) and her bag.
 * <p>
 * The menu type has no extra network data; the client learns the bag size from the menu context
 * packet sent just before the menu opens ({@link #setPendingClientRows}).
 *
 * @author vyrriox
 */
public class VikaInventoryMenu extends AbstractContainerMenu {
    public static final EquipmentSlot[] EQUIPMENT = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    /** Left edge of the bag and of the player inventory; the side panel holds the equipment. */
    public static final int MAIN_LEFT = 88;
    public static final int TOP = 18;
    public static final int ARMOR_X = 8;
    public static final int HANDS_X = 62;
    /** Armor, both hands, and the back (backpack). */
    public static final int EQUIPMENT_SLOTS = 7;
    public static final int BACK_INDEX = 6;

    private static volatile int pendingClientRows = 3;
    private static volatile int pendingClientEntity = -1;

    private final Container bag;
    private final Container equipment;
    private final VikaEntity laura;
    private final int rows;
    private final int entityId;

    /** Server side. */
    public VikaInventoryMenu(int id, Inventory playerInventory, VikaEntity laura) {
        this(id, playerInventory, laura.inventory(), new EquipmentContainer(laura), laura, laura.inventory().getContainerSize() / 9, laura.getId());
    }

    /** Client side, built by the menu type. */
    public static VikaInventoryMenu clientSide(int id, Inventory playerInventory) {
        int rows = Math.max(1, Math.min(6, pendingClientRows));
        return new VikaInventoryMenu(id, playerInventory, new SimpleContainer(rows * 9), new SimpleContainer(EQUIPMENT_SLOTS), null, rows, pendingClientEntity);
    }

    public static void setPendingClientRows(int rows, int entityId) {
        pendingClientRows = rows;
        pendingClientEntity = entityId;
    }

    private VikaInventoryMenu(int id, Inventory playerInventory, Container bag, Container equipment, VikaEntity laura, int rows, int entityId) {
        super(VikaRegistries.INVENTORY_MENU.get(), id);
        this.bag = bag;
        this.equipment = equipment;
        this.laura = laura;
        this.rows = rows;
        this.entityId = entityId;
        bag.startOpen(playerInventory.player);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(bag, col + row * 9, MAIN_LEFT + col * 18, TOP + row * 18));
            }
        }
        ResourceLocation[] armorIcons = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
                InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = EQUIPMENT[i];
            ResourceLocation icon = armorIcons[i];
            this.addSlot(new Slot(equipment, i, ARMOR_X, TOP + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return VikaInventoryMenu.this.laura == null || VikaInventoryMenu.this.laura.getEquipmentSlotForItem(stack) == slot;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
                }
            });
        }
        this.addSlot(new Slot(equipment, 4, HANDS_X, TOP + 36));
        this.addSlot(new Slot(equipment, 5, HANDS_X, TOP + 54) {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
        // One row down, so the slot never sits under the back button in the corner.
        this.addSlot(new Slot(equipment, BACK_INDEX, HANDS_X, TOP + 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VikaBags.isWearableOnBack(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        int playerTop = playerTop(rows);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, MAIN_LEFT + col * 18, playerTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, MAIN_LEFT + col * 18, playerTop + 58));
        }
    }

    /** Top of the player inventory for a bag of {@code rows} rows. */
    public static int playerTop(int rows) {
        return TOP + rows * 18 + 14;
    }

    public int rows() {
        return rows;
    }

    public int entityId() {
        return entityId;
    }

    public int bagSlots() {
        return rows * 9;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        int bagEnd = bagSlots();
        int equipEnd = bagEnd + EQUIPMENT_SLOTS;
        int end = this.slots.size();
        if (index < equipEnd) {
            if (!this.moveItemStackTo(stack, equipEnd, end, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            if (VikaBags.isWearableOnBack(stack) && !this.slots.get(bagEnd + BACK_INDEX).hasItem()) {
                moved = this.moveItemStackTo(stack, bagEnd + BACK_INDEX, bagEnd + BACK_INDEX + 1, false);
            }
            if (!moved && laura != null) {
                EquipmentSlot target = laura.getEquipmentSlotForItem(stack);
                for (int i = 0; i < EQUIPMENT.length && !moved; i++) {
                    if (EQUIPMENT[i] == target && target.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !this.slots.get(bagEnd + i).hasItem()) {
                        moved = this.moveItemStackTo(stack, bagEnd + i, bagEnd + i + 1, false);
                    }
                }
            }
            if (!moved && !this.moveItemStackTo(stack, 0, bagEnd, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (laura == null) {
            return true;
        }
        return laura.isAlive() && player.distanceToSqr(laura) < 8 * 8;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.bag.stopOpen(player);
    }

    /** Laura's equipment seen as a 6 slot container. */
    public static final class EquipmentContainer implements Container {
        private final VikaEntity laura;

        public EquipmentContainer(VikaEntity laura) {
            this.laura = laura;
        }

        @Override
        public int getContainerSize() {
            return EQUIPMENT_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            for (EquipmentSlot slot : EQUIPMENT) {
                if (!laura.getItemBySlot(slot).isEmpty()) {
                    return false;
                }
            }
            return laura.getBackItem().isEmpty();
        }

        @Override
        public ItemStack getItem(int index) {
            return index == BACK_INDEX ? laura.getBackItem() : laura.getItemBySlot(EQUIPMENT[index]);
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack current = getItem(index);
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = current.split(count);
            setItem(index, current.isEmpty() ? ItemStack.EMPTY : current);
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack current = getItem(index);
            setItem(index, ItemStack.EMPTY);
            return current;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (index == BACK_INDEX) {
                // A new instance, so the change is synced to the clients.
                laura.setBackItem(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            } else {
                laura.setItemSlot(EQUIPMENT[index], stack);
            }
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return laura.isAlive() && player.distanceToSqr(laura) < 64;
        }

        @Override
        public void clearContent() {
            for (EquipmentSlot slot : EQUIPMENT) {
                laura.setItemSlot(slot, ItemStack.EMPTY);
            }
            laura.setBackItem(ItemStack.EMPTY);
        }
    }
}
