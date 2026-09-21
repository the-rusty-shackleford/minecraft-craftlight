/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.*;

/** Composition root for the optional crafting convenience screen. */
@Mod(Craftlight.ID)
public final class Craftlight {
    public static final String ID="craftlight";
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ID);
    public static final DeferredHolder<MenuType<?>,MenuType<CraftlightMenu>> MENU=MENUS.register("crafting",
            ()->new MenuType<>(CraftlightMenu::new,FeatureFlags.DEFAULT_FLAGS));
    /** requires: mod bus; effects: registers menu and protocol; throws: registry errors. */
    public Craftlight(IEventBus bus) { MENUS.register(bus);bus.addListener(Payloads::register); }
}
