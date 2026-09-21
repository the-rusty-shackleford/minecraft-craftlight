/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight.client;

import com.chunkworks.craftlight.*;
import com.chunkworks.craftlight.domain.Catalogue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Learned-recipe browser over a real vanilla container.
 * AF: filtered is the current page source; selected names a preview, never an item grant.
 * RI: every entry is learned; pages are bounded; caches rebuild on recipe reloads and unlock changes.
 * The server rechecks every action independently. */
public final class CraftlightScreen extends AbstractContainerScreen<CraftlightMenu> implements net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener {
    private Catalogue catalogue=new Catalogue(List.of(),Set.of());
    private final Map<String,RecipeHolder<?>> recipes=new HashMap<>();
    private List<Catalogue.Entry> filtered=List.of();
    private EditBox search;
    private Button previous,next;
    private RecipeHolder<?> selected;
    private int page,columns,rows;
    private static final int CELL=22,GRID_X=190,GRID_Y=39;

    /** requires: live menu; effects: creates its browser; throws: none. */
    public CraftlightScreen(CraftlightMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override protected void init() {
        imageWidth=Math.min(520,width-8);imageHeight=Math.min(258,height-40);
        super.init();topPos=(height-imageHeight+28)/2;inventoryLabelY=73;
        addRenderableWidget(new InventoryTab(this::getGuiLeft,this::getGuiTop,0,false,new ItemStack(net.minecraft.world.item.Items.CHEST),Component.translatable("container.inventory"),()->{
            minecraft.player.closeContainer();minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(minecraft.player));
        }));
        addRenderableWidget(new InventoryTab(this::getGuiLeft,this::getGuiTop,1,true,new ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE),title,()->{}));
        columns=Math.max(1,(imageWidth-GRID_X-10)/CELL);rows=Math.max(1,(imageHeight-GRID_Y-36)/CELL);
        String old=search==null?"":search.getValue();
        search=addRenderableWidget(new EditBox(font,leftPos+GRID_X,topPos+16,imageWidth-GRID_X-10,18,Component.translatable("craftlight.search")));
        search.setMaxLength(100);search.setHint(Component.translatable("craftlight.search"));search.setResponder(this::filter);
        previous=addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.max(0,page-1);buttons();}).bounds(leftPos+GRID_X,topPos+imageHeight-25,22,18).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;buttons();}).bounds(leftPos+imageWidth-32,topPos+imageHeight-25,22,18).build());
        refresh();search.setValue(old);setInitialFocus(search);
    }
    private final net.minecraft.client.gui.screens.recipebook.RecipeBookComponent recipeUpdates=new net.minecraft.client.gui.screens.recipebook.RecipeBookComponent() {
        @Override public void setupGhostRecipe(RecipeHolder<?> recipe,java.util.List<net.minecraft.world.inventory.Slot> slots) { selected=recipe; }
    };
    @Override public void recipesUpdated() { if(search!=null)refresh(); }
    @Override public net.minecraft.client.gui.screens.recipebook.RecipeBookComponent getRecipeBookComponent() { return recipeUpdates; }
    private void refresh() {
        recipes.clear();var entries=new ArrayList<Catalogue.Entry>();var known=new HashSet<String>();
        for(var holder:minecraft.level.getRecipeManager().getRecipes()) {
            if(!minecraft.player.getRecipeBook().contains(holder))continue;
            var output=holder.value().getResultItem(minecraft.level.registryAccess());if(output.isEmpty())continue;
            String id=holder.id().toString();recipes.put(id,holder);known.add(id);
            entries.add(new Catalogue.Entry(id,output.getHoverName().getString(),holder.value().getType().toString()));
        }
        catalogue=new Catalogue(entries,known);
        if(selected!=null&&!recipes.containsKey(selected.id().toString()))selected=null;
        filtered=catalogue.search(search.getValue());
        page=Math.min(page,Math.max(0,(filtered.size()-1)/pageSize()));buttons();
    }
    private void filter(String query) { filtered=catalogue.search(query);page=0;buttons(); }
    private int pageSize() { return columns*rows; }
    private void buttons() { if(previous!=null)previous.active=page>0;if(next!=null)next.active=(page+1)*pageSize()<filtered.size(); }
    private RecipeHolder<?> at(double mx,double my) {
        int x=(int)Math.floor(mx-leftPos-GRID_X),y=(int)Math.floor(my-topPos-GRID_Y);
        if(x<0||y<0||x>=columns*CELL||y>=rows*CELL)return null;
        int index=page*pageSize()+y/CELL*columns+x/CELL;
        return index<filtered.size()?recipes.get(filtered.get(index).id()):null;
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        var recipe=at(mx,my);
        if(recipe!=null&&(button==0||button==1)) {
            selected=recipe;
            if(button==0)PacketDistributor.sendToServer(new Payloads.Craft(menu.containerId,recipe.id(),hasShiftDown()));
            return true;
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(mx>=leftPos+GRID_X&&mx<leftPos+imageWidth&&my>=topPos&&my<topPos+imageHeight) {
            page=Math.max(0,Math.min(Math.max(0,(filtered.size()-1)/pageSize()),page+(vertical<0?1:-1)));buttons();return true;
        }
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(search.isFocused()&&key!=256)return search.keyPressed(key,scan,modifiers)||search.canConsumeInput();
        return super.keyPressed(key,scan,modifiers);
    }
    private static void panel(GuiGraphics g,int x,int y,int w,int h,int fill) {
        g.fill(x,y,x+w,y+h,0xFF272722);g.fill(x+1,y+1,x+w-1,y+h-1,0xFF57564D);
        g.fill(x+2,y+2,x+w-2,y+h-2,fill);g.fill(x+2,y+2,x+w-2,y+3,0xFFE6E0CB);
        g.fill(x+2,y+3,x+3,y+h-2,0xFFE6E0CB);
    }
    private static void slot(GuiGraphics g,int x,int y) {
        g.fill(x-1,y-1,x+17,y+17,0xFF41423D);g.fill(x,y,x+17,y+17,0xFFE2DDC9);g.fill(x,y,x+16,y+16,0xFF8A897C);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        int x=leftPos,y=topPos;
        panel(g,x,y,imageWidth,imageHeight,0xFFC6C2AD);
        g.fill(x+177,y+8,x+179,y+imageHeight-8,0xFF838272);
        for(var s:menu.slots)slot(g,x+s.x,y+s.y);
        g.drawString(font,"→",x+99,y+39,0xFF55564C,false);
        g.drawString(font,Component.translatable("craftlight.known"),x+GRID_X,y+5,0xFF414638,false);
        preview(g,x,y);
        var hover=at(mx,my);
        int end=Math.min(filtered.size(),(page+1)*pageSize());
        for(int i=page*pageSize();i<end;i++) {
            var holder=recipes.get(filtered.get(i).id());int n=i%pageSize();int cx=x+GRID_X+n%columns*CELL,cy=y+GRID_Y+n/columns*CELL;
            boolean chosen=selected!=null&&selected.id().equals(holder.id());
            panel(g,cx,cy,CELL-1,CELL-1,chosen?0xFFBBD49A:holder==hover?0xFFE3DDBD:0xFFAAA998);
            var stack=holder.value().getResultItem(minecraft.level.registryAccess());g.renderItem(stack,cx+3,cy+2);g.renderItemDecorations(font,stack,cx+3,cy+2);
        }
        if(filtered.isEmpty())g.drawWordWrap(font,Component.translatable("craftlight.empty"),x+GRID_X+4,y+55,imageWidth-GRID_X-18,0xFF535547);
        String count=(page+1)+" / "+Math.max(1,(filtered.size()+pageSize()-1)/pageSize())+"  ·  "+filtered.size();
        g.drawCenteredString(font,count,x+GRID_X+(imageWidth-GRID_X-10)/2,y+imageHeight-20,0xFFEFEBD8);
    }
    private void preview(GuiGraphics g,int x,int y) {
        if(selected==null)return;
        var ingredients=selected.value().getIngredients();int w=selected.value() instanceof ShapedRecipe shaped?shaped.getWidth():3;
        for(int i=0;i<Math.min(9,ingredients.size());i++) {
            if(!menu.getSlot(1+(i/w)*3+i%w).getItem().isEmpty())continue;
            var options=ingredients.get(i).getItems();if(options.length==0)continue;
            int sx=x+30+i%w*18,sy=y+17+i/w*18;
            g.renderItem(options[0],sx,sy);g.fill(sx,sy,sx+16,sy+16,0x668A897C);
        }
        if(menu.getSlot(0).getItem().isEmpty())g.renderItem(selected.value().getResultItem(minecraft.level.registryAccess()),x+124,y+35);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        g.drawString(font,title,8,6,0xFF343B30,false);
        g.drawString(font,playerInventoryTitle,8,73,0xFF414638,false);
        g.drawString(font,Component.translatable(menu.hasWorkbench()?"craftlight.table.yes":"craftlight.table.no"),8,166,menu.hasWorkbench()?0xFF35602F:0xFF69533E,false);
        if(imageHeight>=235)g.drawWordWrap(font,Component.translatable("craftlight.controls"),8,182,160,0xFF535547);
        g.drawWordWrap(font,Component.translatable("craftlight.status."+menu.status()),8,imageHeight>=235?213:179,160,0xFF343B30);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        var holder=at(mx,my);if(holder==null)return;
        var output=holder.value().getResultItem(minecraft.level.registryAccess());
        var lines=new ArrayList<Component>();lines.add(output.getHoverName());
        lines.add(Component.literal(holder.id().getNamespace()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        boolean craft=holder.value() instanceof CraftingRecipe;
        lines.add(Component.translatable(craft?"craftlight.tooltip.craft":"craftlight.tooltip.station").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(!craft) {
            String kind=net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()).getPath();
            net.minecraft.network.chat.MutableComponent station=switch(kind) {
                case "smelting" -> Component.translatable("block.minecraft.furnace");
                case "blasting" -> Component.translatable("block.minecraft.blast_furnace");
                case "smoking" -> Component.translatable("block.minecraft.smoker");
                case "campfire_cooking" -> Component.translatable("block.minecraft.campfire");
                case "stonecutting" -> Component.translatable("block.minecraft.stonecutter");
                case "smithing" -> Component.translatable("block.minecraft.smithing_table");
                default -> Component.literal(kind.replace('_',' '));
            };
            lines.add(station.withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        if(craft&&!((CraftingRecipe)holder.value()).canCraftInDimensions(2,2))lines.add(Component.translatable("craftlight.tooltip.table").withStyle(net.minecraft.ChatFormatting.GOLD));
        var costs=new LinkedHashMap<String,Integer>();
        for(var ingredient:holder.value().getIngredients()) {
            var items=ingredient.getItems();if(items.length>0)costs.merge(items[0].getHoverName().getString()+(items.length>1?" / …":""),1,Integer::sum);
        }
        costs.forEach((name,count)->lines.add(Component.literal(count+" × "+name).withStyle(net.minecraft.ChatFormatting.GRAY)));
        g.renderComponentTooltip(font,lines,mx,my);
    }
}
