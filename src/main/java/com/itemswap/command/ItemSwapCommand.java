package com.itemswap.command;

import com.itemswap.config.ItemSwapConfig;
import com.itemswap.handler.ItemSwapHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ItemArgument;
import net.minecraft.command.arguments.ItemInput;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registers the /itemswap command with its subcommands:
 *   /itemswap tool       <item> <slot>  – swap worn-out tools automatically
 *   /itemswap consumable <item> <slot>  – keep a hotbar slot stocked with consumables
 *   /itemswap clear                     – disable all tracking
 */
public class ItemSwapCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("itemswap")

                // ── /itemswap tool <item> <slot> ──────────────────────────────
                .then(Commands.literal("tool")
                    .then(Commands.argument("item", ItemArgument.item())
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                            .executes(ctx -> {
                                ItemInput input = ItemArgument.getItem(ctx, "item");
                                int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                                ItemSwapConfig.setTool(input.getItem(), slot);
                                ctx.getSource().sendSuccess(
                                    new StringTextComponent(
                                        "\u00a7aItemSwap \u00bb\u00a7r Отслеживание инструмента: "
                                        + "\u00a7e" + input.getItem().getRegistryName()
                                        + "\u00a7r в слот \u00a7e" + (slot + 1)),
                                    false);
                                return 1;
                            })
                        )
                    )
                )

                // ── /itemswap consumable <item> <slot> ────────────────────────
                .then(Commands.literal("consumable")
                    .then(Commands.argument("item", ItemArgument.item())
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                            .executes(ctx -> {
                                ItemInput input = ItemArgument.getItem(ctx, "item");
                                int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                                ItemSwapConfig.setConsumable(input.getItem(), slot);

                                // Distribute item to all empty main-inventory slots
                                try {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrException();
                                    ItemSwapHandler.distributeConsumable(
                                        player.inventory, input.getItem(), slot);
                                } catch (CommandSyntaxException e) {
                                    LOGGER.debug("ItemSwap: consumable command not run by a player, skipping distribution", e);
                                }

                                ctx.getSource().sendSuccess(
                                    new StringTextComponent(
                                        "\u00a7aItemSwap \u00bb\u00a7r Отслеживание расходника: "
                                        + "\u00a7e" + input.getItem().getRegistryName()
                                        + "\u00a7r в слот \u00a7e" + (slot + 1)),
                                    false);
                                return 1;
                            })
                        )
                    )
                )

                // ── /itemswap clear ───────────────────────────────────────────
                .then(Commands.literal("clear")
                    .executes(ctx -> {
                        ItemSwapConfig.clearAll();
                        ctx.getSource().sendSuccess(
                            new StringTextComponent(
                                "\u00a7aItemSwap \u00bb\u00a7r Отслеживание отключено."),
                            false);
                        return 1;
                    })
                )
        );
    }
}
