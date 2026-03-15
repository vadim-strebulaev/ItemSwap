package com.itemswap.config;

import net.minecraft.item.Item;

/**
 * Holds the runtime state of what items are currently being tracked
 * by the /itemswap command. Shared between the command and the tick handler.
 */
public class ItemSwapConfig {

    // ── Tool tracking ────────────────────────────────────────────────────────
    private static volatile Item trackedTool = null;
    /** Inventory slot index (0-based) in the hotbar. -1 means disabled. */
    private static volatile int toolSlot = -1;

    // ── Consumable tracking ──────────────────────────────────────────────────
    private static volatile Item trackedConsumable = null;
    /** Hotbar slot index (0-based) that should be kept stocked. -1 means disabled. */
    private static volatile int consumableHotbarSlot = -1;

    // ── Getters ──────────────────────────────────────────────────────────────

    public static Item getTrackedTool() {
        return trackedTool;
    }

    public static int getToolSlot() {
        return toolSlot;
    }

    public static Item getTrackedConsumable() {
        return trackedConsumable;
    }

    public static int getConsumableHotbarSlot() {
        return consumableHotbarSlot;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public static void setTool(Item item, int slot) {
        trackedTool = item;
        toolSlot = slot;
    }

    public static void setConsumable(Item item, int slot) {
        trackedConsumable = item;
        consumableHotbarSlot = slot;
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    public static void clearTool() {
        trackedTool = null;
        toolSlot = -1;
    }

    public static void clearConsumable() {
        trackedConsumable = null;
        consumableHotbarSlot = -1;
    }

    public static void clearAll() {
        clearTool();
        clearConsumable();
    }
}
