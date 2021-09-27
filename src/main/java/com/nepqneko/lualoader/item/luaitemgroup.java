package com.nepqneko.lualoader.item;

import com.nepqneko.lualoader.lualoader;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.NonNullList;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;
import org.luaj.lualoader.vm2.lib.VarArgFunction;
import org.luaj.lualoader.vm2.lib.jse.CoerceJavaToLua;

import javax.annotation.Nullable;

public class luaitemgroup extends ItemGroup {
    public LuaValue luatable;
    private String luaname;
    private final String tabLabel;
    private final ITextComponent groupName;
    private String tabPath;
    private String backgroundTexture = "items.png";
    private boolean hasScrollbar = true;
    private boolean drawTitle = true;
    private EnchantmentType[] enchantmentTypes = new EnchantmentType[0];
    private ItemStack icon;
    private final LuaValue thismeta;

    public luaitemgroup(String tn){
        super(lualoader.lua.get("Register").get("GetList").call().get("ITEMGROUP").get(tn).get("Name").tojstring());
        this.luaname = tn;
        this.luatable = lualoader.lua.get("Register").get("GetList").call().get("ITEMGROUP").get(tn);
        this.tabLabel = this.luatable.get("Name").tojstring();

        if (!this.luatable.get("GroupName").isnil()){
            this.groupName = new StringTextComponent(this.luatable.get("GroupName").tojstring());
        }
        else {
            this.groupName = new TranslationTextComponent("itemGroup."+this.luatable.get("Name").tojstring());
        }

        this.icon = ItemStack.EMPTY;

        LuaValue meta = LuaValue.tableOf();

        meta.set("__index",this.luatable);
        meta.set("__newindex",this.luatable);

        LuaValue rt = CoerceJavaToLua.coerce(this);

        rt.setmetatable(meta);

        this.thismeta = rt;

        try {
            luatable.get("Init").call(thismeta);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEMGROUP:Init");
        }

        lualoader.allluaitemgrouplist.add(this);
    }

    public void ReGetLuaTable(){
        try {
            luatable = lualoader.lua.get("table").get("merge").call(luatable,lualoader.lua.get("Register").get("GetList").call().get("ITEMGROUP").get(luaname));
            luatable.get("OnReloaded").call(thismeta);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEMGROUP:ReGetLuaTable");
        }
    }

    final class index extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return CoerceJavaToLua.coerce(this);
        }
    }

    /**
     * Gets the name that's valid for use in a ResourceLocation's path. This should be set if the tabLabel contains
     * illegal characters.
     */
    @Override
    public String getPath() {
        return this.tabPath == null ? this.tabLabel : this.tabPath;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ITextComponent getGroupName() {
        return this.groupName;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ItemStack getIcon() {
        if (this.icon.isEmpty()) {
            this.icon = this.createIcon();
        }

        return this.icon;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ItemStack createIcon(){
        try {
            return new ItemStack((IItemProvider)luatable.get("CreateIcon").call(thismeta).touserdata(IItemProvider.class));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEMGROUP:CreateIcon");

            return new ItemStack(Blocks.PEONY);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public String getBackgroundImageName() {
        return this.backgroundTexture;
    }

    @Override
    public ItemGroup setBackgroundImageName(String texture) {
        this.backgroundTexture = texture;
        return this;
    }

    @Override
    public ItemGroup setTabPath(String pathIn) {
        this.tabPath = pathIn;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean drawInForegroundOfTab() {
        try {
            return luatable.get("DrawInForegroundOfTab").call(thismeta).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEMGROUP:DrawInForegroundOfTab");

            return this.drawTitle;
        }
    }

    @Override
    public ItemGroup setNoTitle() {
        this.drawTitle = false;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean hasScrollbar() {
        return this.hasScrollbar;
    }

    @Override
    public ItemGroup setNoScrollbar() {
        this.hasScrollbar = false;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isAlignedRight() {
        return this.getColumn() == 5;
    }

    /**
     * Returns the enchantment types relevant to this tab
     */
    @Override
    public EnchantmentType[] getRelevantEnchantmentTypes() {
        return this.enchantmentTypes;
    }

    /**
     * Sets the enchantment types for populating this tab with enchanting books
     */
    @Override
    public ItemGroup setRelevantEnchantmentTypes(EnchantmentType... types) {
        this.enchantmentTypes = types;
        return this;
    }

    @Override
    public boolean hasRelevantEnchantmentType(@Nullable EnchantmentType enchantmentType) {
        if (enchantmentType != null) {
            for(EnchantmentType enchantmenttype : this.enchantmentTypes) {
                if (enchantmenttype == enchantmentType) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Fills {@code items} with all items that are in this group.
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void fill(NonNullList<ItemStack> items) {
        for(Item item : Registry.ITEM) {
            item.fillItemGroup(this, items);
        }
    }

    @Override
    public boolean hasSearchBar() {
        return luatable.get("HasSearchBar").toboolean();
    }

    /**
     * Gets the width of the search bar of the creative tab, use this if your
     * creative tab name overflows together with a custom texture.
     *
     * @return The width of the search bar, 89 by default
     */
    @Override
    public int getSearchbarWidth() {
        try {
            return luatable.get("GetSearchbarWidth").call(thismeta).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEMGROUP:GetSearchbarWidth");

            return 89;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public net.minecraft.util.ResourceLocation getBackgroundImage() {
        try {
            return (net.minecraft.util.ResourceLocation)luatable.get("GetBackgroundImage").call(thismeta).touserdata(net.minecraft.util.ResourceLocation.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEMGROUP:GetBackgroundImage");

            return new net.minecraft.util.ResourceLocation("textures/gui/container/creative_inventory/tab_" + this.getBackgroundImageName());
        }
    }

    private static final net.minecraft.util.ResourceLocation CREATIVE_INVENTORY_TABS = new net.minecraft.util.ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    @OnlyIn(Dist.CLIENT)
    @Override
    public net.minecraft.util.ResourceLocation getTabsImage() {
        try {
            return (net.minecraft.util.ResourceLocation)luatable.get("GetTabsImage").call(thismeta).touserdata(net.minecraft.util.ResourceLocation.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEMGROUP:GetTabsImage");

            return CREATIVE_INVENTORY_TABS;
        }
    }

    @Override
    public int getLabelColor() {
        try {
            return luatable.get("GetLabelColor").call(thismeta).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEMGROUP:GetLabelColor");

            return 4210752;
        }
    }

    @Override
    public int getSlotColor() {
        try {
            return luatable.get("GetSlotColor").call(thismeta).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEMGROUP:GetSlotColor");

            return -2130706433;
        }
    }
}
