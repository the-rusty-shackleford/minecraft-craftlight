/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight.gametest;
import com.chunkworks.craftlight.*;
import com.chunkworks.craftlight.client.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.*;
import java.util.function.Consumer;

/** Hardware-client gate: genuine E-inventory, tab, widget and network paths; recipe unlock
 * updates, station refusal/success, normal outputs, bucket returns and GUI scaling.
 * Screenshots require human visual inspection; this fixture never ships. */
@EventBusSubscriber(modid="craftlight_gametest",value=Dist.CLIENT)
public final class CraftlightBooth {
    private static final Logger LOG=LoggerFactory.getLogger("Craftlight booth");
    private static int tick;
    private static final BlockPos TABLE=new BlockPos(1,100,0);
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("craftlight.booth"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(mc.screen instanceof PauseScreen)mc.setScreen(null);
        mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
        try {
            switch(++tick) {
                case 20 -> server(mc,p->{
                    var l=p.serverLevel();l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());
                    l.setDayTime(1800);l.setWeatherParameters(6000,0,false,false);
                    for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++) {
                        l.setBlock(new BlockPos(x,99,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
                        for(int y=100;y<105;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
                    }
                    p.teleportTo(l,.5,100,.5,180,10);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
                    p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,16));p.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,16));
                    p.getInventory().setItem(2,new ItemStack(Items.COBBLESTONE,32));p.getInventory().setItem(3,new ItemStack(Items.STICK,8));
                    for(String id:new String[]{"oak_planks","stick","crafting_table","chest","cake","stone_pickaxe","furnace"})learn(p,id);
                    p.inventoryMenu.broadcastChanges();
                });
                case 70 -> inventory(mc);
                case 75 -> { check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen,"E opens normal inventory");photo(mc,"00-inventory-tabs"); }
                case 76 -> mc.gameMode.handleInventoryMouseClick(0,36,0,net.minecraft.world.inventory.ClickType.PICKUP,mc.player);
                case 80 -> { check(mc.player.containerMenu.getCarried().is(Items.OAK_LOG),"cursor holds real inventory logs before tab switch");tab(mc,1); }
                case 100 -> { screen(mc);check(!((CraftlightMenu)mc.player.containerMenu).hasWorkbench(),"inventory mode without table");photo(mc,"01-browser"); }
                case 110 -> search(mc,"chest");
                case 115 -> click(mc,1);
                case 120 -> click(mc,0);
                case 140 -> { check(((CraftlightMenu)mc.player.containerMenu).status()==CraftlightMenu.STATION,"real client request refuses distant station");photo(mc,"02-table-required"); }
                case 150 -> { mc.player.closeContainer();server(mc,p->{check(CraftingGameTests.count(p,Items.CHEST)==0,"no chest granted without table");p.serverLevel().setBlock(TABLE,Blocks.CRAFTING_TABLE.defaultBlockState(),3);}); }
                case 175 -> inventory(mc);
                case 185 -> tab(mc,1);
                case 195 -> { check(((CraftlightMenu)mc.player.containerMenu).hasWorkbench(),"nearby table synchronized");search(mc,"chest");click(mc,0); }
                case 220 -> { server(mc,p->check(CraftingGameTests.count(p,Items.CHEST)==1&&CraftingGameTests.count(p,Items.OAK_PLANKS)==8,"client click consumes eight planks for chest"));photo(mc,"03-crafted-chest"); }
                case 230 -> { search(mc,"oak planks");click(mc,0); }
                case 250 -> server(mc,p->{
                    check(CraftingGameTests.count(p,Items.OAK_LOG)==15&&CraftingGameTests.count(p,Items.OAK_PLANKS)==12,"client small recipe has normal yield");
                    for(int i=0;i<3;i++)p.getInventory().setItem(9+i,new ItemStack(Items.MILK_BUCKET));
                    p.getInventory().setItem(12,new ItemStack(Items.WHEAT,3));p.getInventory().setItem(13,new ItemStack(Items.SUGAR,2));p.getInventory().setItem(14,new ItemStack(Items.EGG));
                    p.containerMenu.broadcastChanges();
                });
                case 280 -> { search(mc,"cake");click(mc,1); }
                case 300 -> photo(mc,"04-recipe-preview");
                case 310 -> click(mc,0);
                case 335 -> { server(mc,p->check(CraftingGameTests.count(p,Items.CAKE)==1&&CraftingGameTests.count(p,Items.BUCKET)==3,"client cake craft returns three buckets"));photo(mc,"05-returned-buckets"); }
                case 345 -> search(mc,"diamond sword");
                case 355 -> { click(mc,0);photo(mc,"06-unknown-filtered"); }
                case 365 -> server(mc,p->learn(p,"diamond_sword"));
                case 385 -> { click(mc,1);photo(mc,"07-live-unlock"); }
                case 390 -> click(mc,0);
                case 410 -> check(((CraftlightMenu)mc.player.containerMenu).status()==CraftlightMenu.MATERIALS,"newly unlocked recipe visible, ingredients still required");
                case 415 -> tab(mc,0);
                case 425 -> {
                    check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen,"Inventory tab returns directly to vanilla inventory");
                    check(mc.player.containerMenu.getCarried().isEmpty(),"returning to inventory leaves no ghost cursor stack");
                    var inv=(net.minecraft.client.gui.screens.inventory.InventoryScreen)mc.screen;
                    inv.mouseClicked(inv.getGuiLeft()+110,inv.getGuiTop()+66,0);
                }
                case 440 -> { photo(mc,"08-inventory-recipe-book");tab(mc,1); }
                case 465 -> { screen(mc);mc.options.guiScale().set(3);mc.resizeDisplay();search(mc,"cake");click(mc,1); }
                case 490 -> photo(mc,"09-compact-tabs");
                case 510 -> { mc.player.closeContainer();LOG.info("craftlight booth: COMPLETE");mc.stop(); }
            }
        } catch(Throwable failure) { LOG.error("craftlight booth: FAIL",failure);mc.stop(); }
    }
    private static void inventory(Minecraft mc) { KeyMapping.click(mc.options.keyInventory.getKey()); }
    private static void tab(Minecraft mc,int column) {
        check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>,"tab belongs to inventory container");
        var s=(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)mc.screen;
        s.mouseClicked(s.getGuiLeft()+column*28+13,s.getGuiTop()-14,0);
    }
    private static CraftlightScreen screen(Minecraft mc) { check(mc.screen instanceof CraftlightScreen,"registered Craftlight screen opened");return (CraftlightScreen)mc.screen; }
    private static void search(Minecraft mc,String text) {
        var s=screen(mc);var field=(EditBox)s.children().stream().filter(e->e instanceof EditBox).findFirst().orElseThrow();
        field.setValue("");s.setFocused(field);field.setFocused(true);for(char c:text.toCharArray())s.charTyped(c,0);
    }
    private static void click(Minecraft mc,int button) {
        var s=screen(mc);s.mouseClicked(s.getGuiLeft()+200,s.getGuiTop()+49,button);
    }
    private static void learn(ServerPlayer p,String id) { p.awardRecipes(java.util.List.of(p.server.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace(id)).orElseThrow())); }
    private static void server(Minecraft mc,Consumer<ServerPlayer> action) {
        var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
        server.execute(()->{try { action.accept(server.getPlayerList().getPlayer(id)); }catch(Throwable failure){ LOG.error("craftlight booth: FAIL",failure);mc.execute(mc::stop); }});
    }
    private static void photo(Minecraft mc,String name) { mc.getToasts().clear();Screenshot.grab(mc.gameDirectory,"craftlight-"+name+".png",mc.getMainRenderTarget(),m->LOG.info("craftlight booth: {}",m.getString())); }
    private static void check(boolean ok,String message) { if(!ok)throw new IllegalStateException(message);LOG.info("craftlight booth: PASS {}",message); }
}
