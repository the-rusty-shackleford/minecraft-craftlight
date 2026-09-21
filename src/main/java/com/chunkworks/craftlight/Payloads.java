/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Protocol 1 carries only an open request or a bounded recipe action in a live menu. */
public final class Payloads {
    private Payloads() {}
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Craftlight.ID,"open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.unit(new Open());
        @Override public Type<Open> type() { return TYPE; }
    }
    public record Craft(int menu,ResourceLocation recipe,boolean stack) implements CustomPacketPayload {
        public static final Type<Craft> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Craftlight.ID,"craft"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Craft> CODEC=StreamCodec.composite(
                ByteBufCodecs.VAR_INT,Craft::menu,ResourceLocation.STREAM_CODEC,Craft::recipe,ByteBufCodecs.BOOL,Craft::stack,Craft::new);
        @Override public Type<Craft> type() { return TYPE; }
    }
    /** requires: mod payload-registration event; effects: registers server-owned actions; throws: registry errors. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r=event.registrar("1");
        r.playToServer(Open.TYPE,Open.CODEC,(payload,context)->context.enqueueWork(()->{
            if(context.player() instanceof ServerPlayer player)Crafting.open(player);
        }));
        r.playToServer(Craft.TYPE,Craft.CODEC,(payload,context)->context.enqueueWork(()->{
            if(context.player() instanceof ServerPlayer player)Crafting.craft(player,payload.menu(),payload.recipe(),payload.stack());
        }));
    }
}
