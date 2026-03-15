package com.itemswap.handler;

import com.itemswap.ItemSwapMod;
import com.itemswap.config.ItemSwapConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tick-based handler that:
 *  1. Monitors a designated hotbar slot for tool wear and swaps in a fresh copy
 *     when the tool reaches 90 % durability loss.
 *  2. Monitors a designated hotbar slot for consumables and restocks it from
 *     the main inventory whenever it empties.
 *
 * Inventory slot layout (PlayerInventory.getItem):
 *   0-8   – hotbar
 *   9-35  – main inventory
 *   36-39 – armour
 *   40    – off-hand
 */
public class ItemSwapHandler {

    /** Fraction of max durability that must be consumed before a tool swap is triggered. */
    private static final double TOOL_DAMAGE_THRESHOLD = 0.9;

    /** Number of main inventory slots. */
    private static final int MAIN_INV_START = 9;
    private static final int MAIN_INV_END   = 36; // exclusive

    /** Only run checks once per second (20 ticks) to avoid performance overhead. */
    private static final AtomicInteger tickCounter = new AtomicInteger(0);

    // ── Tick handler ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter.incrementAndGet();
        if (tickCounter.get() < 20) return;
        tickCounter.set(0);

        PlayerEntity player   = event.player;
        PlayerInventory inv   = player.inventory;

        handleToolTracking(inv);
        handleConsumableTracking(inv);
    }

    // ── Tool logic ────────────────────────────────────────────────────────────

    private static void handleToolTracking(PlayerInventory inv) {
        Item trackedTool = ItemSwapConfig.getTrackedTool();
        int  toolSlot    = ItemSwapConfig.getToolSlot();

        if (trackedTool == null || toolSlot < 0) return;

        ItemStack current = inv.getItem(toolSlot);

        // Slot must contain the correct tool type
        if (current.isEmpty() || current.getItem() != trackedTool) return;
        if (!current.isDamageableItem()) return;

        int maxDamage     = current.getMaxDamage();
        int currentDamage = current.getDamageValue();

        if (currentDamage < (int)(maxDamage * TOOL_DAMAGE_THRESHOLD)) return;

        // Search the whole inventory (hotbar + main) for a fresher copy
        for (int i = 0; i < MAIN_INV_END; i++) {
            if (i == toolSlot) continue;

            ItemStack candidate = inv.getItem(i);
            if (candidate.isEmpty() || candidate.getItem() != trackedTool) continue;
            if (!candidate.isDamageableItem()) continue;

            int candidateDamage = candidate.getDamageValue();
            int candidateMax    = candidate.getMaxDamage();

            if (candidateDamage < (int)(candidateMax * TOOL_DAMAGE_THRESHOLD)) {
                // Swap the worn tool with the fresh one
                ItemStack worn  = current.copy();
                ItemStack fresh = candidate.copy();
                inv.setItem(toolSlot, fresh);
                inv.setItem(i, worn);
                ItemSwapMod.LOGGER.debug(
                    "ItemSwap: Swapped worn tool ({}) from slot {} with fresher copy in slot {}",
                    trackedTool.getRegistryName(), toolSlot, i);
                return;
            }
        }
    }

    // ── Consumable logic ──────────────────────────────────────────────────────

    private static void handleConsumableTracking(PlayerInventory inv) {
        Item trackedConsumable = ItemSwapConfig.getTrackedConsumable();
        int  hotbarSlot        = ItemSwapConfig.getConsumableHotbarSlot();

        if (trackedConsumable == null || hotbarSlot < 0) return;

        ItemStack hotbarStack = inv.getItem(hotbarSlot);

        // If the hotbar slot is empty (or has a different item), pull from main inventory
        if (hotbarStack.isEmpty() || hotbarStack.getItem() != trackedConsumable) {
            for (int i = MAIN_INV_START; i < MAIN_INV_END; i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && stack.getItem() == trackedConsumable) {
                    inv.setItem(hotbarSlot, stack.copy());
                    inv.setItem(i, ItemStack.EMPTY);
                    ItemSwapMod.LOGGER.debug(
                        "ItemSwap: Moved consumable ({}) from inventory slot {} to hotbar slot {}",
                        trackedConsumable.getRegistryName(), i, hotbarSlot);
                    return;
                }
            }
        }
    }

    // ── Distribution helper (called on /itemswap consumable command) ──────────

    /**
     * Distributes at least one copy of {@code item} into every empty main-inventory
     * slot (9-35), taking items from wherever they currently exist in the inventory.
     * The designated hotbar slot is also stocked if it does not already hold the item.
     *
     * @param inv     the player's inventory
     * @param item    the consumable item to distribute
     * @param hotbarSlot the hotbar slot (0-based) that should hold the item
     */
    public static void distributeConsumable(PlayerInventory inv, Item item, int hotbarSlot) {
        // Ensure hotbar slot has the item first
        if (inv.getItem(hotbarSlot).isEmpty()
                || inv.getItem(hotbarSlot).getItem() != item) {
            for (int i = MAIN_INV_START; i < MAIN_INV_END; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && s.getItem() == item) {
                    inv.setItem(hotbarSlot, s.copy());
                    inv.setItem(i, ItemStack.EMPTY);
                    break;
                }
            }
        }

        // Place at least 1 of the item in every empty main-inventory slot
        for (int target = MAIN_INV_START; target < MAIN_INV_END; target++) {
            if (!inv.getItem(target).isEmpty()) continue;

            // Find a source slot that has more than 1 item (so we can split off 1)
            ItemStack sourceStack = findSplittableStack(inv, item, target);
            if (sourceStack == null) break; // Nothing left to distribute

            ItemStack single = sourceStack.copy();
            single.setCount(1);
            inv.setItem(target, single);
            sourceStack.shrink(1);
        }

        ItemSwapMod.LOGGER.debug(
            "ItemSwap: Distributed consumable ({}) across main inventory, hotbar slot {}",
            item.getRegistryName(), hotbarSlot);
    }

    /**
     * Returns a mutable reference to a stack of {@code item} in the entire
     * inventory that has a count > 1, excluding {@code excludeSlot}.
     */
    private static ItemStack findSplittableStack(PlayerInventory inv, Item item, int excludeSlot) {
        for (int i = 0; i < MAIN_INV_END; i++) {
            if (i == excludeSlot) continue;
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() == item && s.getCount() > 1) {
                return s;
            }
        }
        return null;
    }
}
