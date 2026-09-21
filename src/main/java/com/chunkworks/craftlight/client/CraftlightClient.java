/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight.client;
import com.chunkworks.craftlight.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Registers the Craftlight inventory page; no separate keybinding or item grants. */
@EventBusSubscriber(modid=Craftlight.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class CraftlightClient {
    private CraftlightClient() {}
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) { event.register(Craftlight.MENU.get(),CraftlightScreen::new); }
    @EventBusSubscriber(modid=Craftlight.ID,value=Dist.CLIENT)
    public static final class InventoryPages {
        @SubscribeEvent public static void recipes(RecipesUpdatedEvent event) {
            if(Minecraft.getInstance().screen instanceof CraftlightScreen screen)screen.recipesUpdated();
        }
        @SubscribeEvent public static void inventory(ScreenEvent.Init.Post event) {
            var mc=Minecraft.getInstance();
            if(!(event.getScreen() instanceof InventoryScreen screen)||mc.player==null||mc.player.isCreative()||mc.player.isSpectator())return;
            event.addListener(new InventoryTab(screen::getGuiLeft,screen::getGuiTop,0,true,new ItemStack(Items.CHEST),Component.translatable("container.inventory"),()->{}));
            event.addListener(new InventoryTab(screen::getGuiLeft,screen::getGuiTop,1,false,new ItemStack(Items.CRAFTING_TABLE),Component.translatable("container.craftlight"),()->PacketDistributor.sendToServer(new Payloads.Open())));
        }
    }
}
