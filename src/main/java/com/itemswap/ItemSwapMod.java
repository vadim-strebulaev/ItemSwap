package com.itemswap;

import com.itemswap.command.ItemSwapCommand;
import com.itemswap.handler.ItemSwapHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ItemSwapMod.MOD_ID)
public class ItemSwapMod {

    public static final String MOD_ID = "itemswap";
    public static final Logger LOGGER = LogManager.getLogger();

    public ItemSwapMod() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ItemSwapHandler.class);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ItemSwapCommand.register(event.getDispatcher());
    }
}
