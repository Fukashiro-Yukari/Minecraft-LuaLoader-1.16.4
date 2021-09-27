package com.nepqneko.lualoader.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.nepqneko.lualoader.lualoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tags.ITag;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;
import org.luaj.lualoader.vm2.LuaTable;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;
import org.luaj.lualoader.vm2.lib.jse.CoerceJavaToLua;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class luaitem extends Item {
    public static final Map<Block, Item> BLOCK_TO_ITEM = net.minecraftforge.registries.GameData.getBlockItemMap();
    protected final ItemGroup group;
    public LuaValue luatable;
    private final String luaname;
    private final Rarity rarity;
    private final int maxStackSize;
    private final int maxDamage;
    private final boolean burnable;
    private final Item containerItem;
    @Nullable
    private String translationKey;
    @Nullable
    private final Food food;
    private final LuaValue thismeta;

    public static int getIdFromItem(Item itemIn){
        return itemIn == null ? 0 : Registry.ITEM.getId(itemIn);
    }

    public static Item getItemById(int id){
        return Registry.ITEM.getByValue(id);
    }

    @Deprecated
    public static Item getItemFromBlock(Block blockIn){
        return BLOCK_TO_ITEM.getOrDefault(blockIn, Items.AIR);
    }

    public luaitem(String tn){
        super(new luaitemset(tn).SetItemPr());

        this.luaname = tn;
        this.luatable = lualoader.lua.get("Register").get("GetList").call().get("ITEM").get(tn);
        this.group = (ItemGroup)this.luatable.get("Group").touserdata(ItemGroup.class);
        this.rarity = (Rarity)this.luatable.get("Rarity").touserdata(Rarity.class);
        this.containerItem = (Item)this.luatable.get("ContainerItem").touserdata(Item.class);
        this.maxDamage = this.luatable.get("MaxDamage").toint();
        this.maxStackSize = this.luatable.get("MaxStackSize").toint();

        Food.Builder food = (new Food.Builder());

        if (this.luatable.get("IsMeat").toboolean()) food = food.meat();
        if (this.luatable.get("FastToEat").toboolean()) food = food.fastToEat();
        if (this.luatable.get("CanEatWhenFull").toboolean()) food = food.setAlwaysEdible();

        try {
            LuaTable tbl = !this.luatable.get("FoodEffects").isnil() ? this.luatable.get("FoodEffects").checktable() : null;
            LuaValue k = LuaValue.NIL;

            if (tbl != null){
                while (true){
                    Varargs n = tbl.next(k);

                    if ((k = n.arg1()).isnil()) break;

                    LuaValue v = n.arg(2);

                    food = food.effect(() -> {
                        try {
                            v.get("func").call();
                        }
                        catch (Exception e){
                            lualoader.LuaError(e,"ITEM:FoodEffects.func");
                        }

                        return (EffectInstance) v.get("effect").touserdata(EffectInstance.class);
                    },v.get("probability").tofloat());
                }
            }
        }
        catch (Exception e){
            lualoader.LuaError(e,"luaitem:FoodEffects");
        }

        food = food.hunger(this.luatable.get("Hunger").toint());
        food = food.saturation(this.luatable.get("Saturation").tofloat());

        this.food = food.build();
        this.burnable = this.luatable.get("ImmuneToFire").toboolean();
        this.canRepair = this.luatable.get("CanRepair").toboolean();

        LuaTable tbl = this.luatable.get("ToolClasses").isnil() ? this.luatable.get("ToolClasses").checktable() : null;
        LuaValue k = LuaValue.NIL;

        if (tbl != null){
            while (true) {
                Varargs n = tbl.next(k);

                if ((k = n.arg1()).isnil()) break;

                LuaValue v = n.arg(2);

                this.toolClasses.put((ToolType)k.touserdata(ToolType.class),v.toint());
            }
        }

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
            lualoader.LuaError(e,"ITEM:Init");
        }

        lualoader.allluaitemlist.add(this);
    }

    public void ReGetLuaTable(){
        try {
            luatable = lualoader.lua.get("table").get("merge").call(luatable,lualoader.lua.get("Register").get("GetList").call().get("ITEM").get(luaname));
            luatable.get("OnReloaded").call(thismeta);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:ReGetLuaTable");
        }
    }

    /**
     * Called as the item is being used by an entity.
     */
    @Override
    public void onUse(World worldIn, LivingEntity livingEntityIn, ItemStack stack, int count){
        try {
            luatable.get("OnUse").call(thismeta,CoerceJavaToLua.coerce(worldIn),CoerceJavaToLua.coerce(livingEntityIn),CoerceJavaToLua.coerce(stack),LuaValue.valueOf(count));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnUse");
        }
    }

    /**
     * Called when an ItemStack with NBT data is read to potentially that ItemStack's NBT data
     */
    @Override
    public boolean updateItemStackNBT(CompoundNBT nbt){
        try {
            return luatable.get("UpdateItemStackNBT").call(thismeta,CoerceJavaToLua.coerce(nbt)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:UpdateItemStackNBT");

            return false;
        }
    }

    @Override
    public boolean canPlayerBreakBlockWhileHolding(BlockState state, World worldIn, BlockPos pos, PlayerEntity player){
        try {
            return luatable.get("CanPlayerBreakBlockWhileHolding").call(thismeta,CoerceJavaToLua.coerce(state),CoerceJavaToLua.coerce(worldIn),CoerceJavaToLua.coerce(pos),CoerceJavaToLua.coerce(player)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:CanPlayerBreakBlockWhileHolding");

            return true;
        }
    }

    @Override
    public Item asItem(){
        return this;
    }

    /**
     * Called when this item is used when targetting a Block
     */
    @Override
    public ActionResultType onItemUse(ItemUseContext context){
        try {
            return (ActionResultType)luatable.get("OnItemUse").call(thismeta,CoerceJavaToLua.coerce(context)).touserdata(ActionResultType.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnItemUse");

            return ActionResultType.PASS;
        }
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state){
        try {
            return luatable.get("GetDestroySpeed").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(state)).tofloat();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetDestroySpeed");

            return 1.0F;
        }
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see
     * {@link #onItemUse}.
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn){
        try {
            return (ActionResult)luatable.get("OnItemRightClick").call(thismeta, CoerceJavaToLua.coerce(worldIn), CoerceJavaToLua.coerce(playerIn), CoerceJavaToLua.coerce(handIn)).touserdata(ActionResult.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnItemRightClick");

            return ActionResult.resultPass(playerIn.getHeldItem(handIn));
        }
    }

    /**
     * Called when the player finishes using this Item (E.g. finishes eating.). Not called when the player stops using
     * the Item before the action is complete.
     */
    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving){
        try {
            return (ItemStack)luatable.get("OnItemUseFinish").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(worldIn),CoerceJavaToLua.coerce(entityLiving)).touserdata(ItemStack.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnItemUseFinish");

            return stack;
        }
    }

    @Override
    public boolean isDamageable(ItemStack stack){
        return this.getItem().isDamageable();
    }

    @Override
    public boolean isDamageable(){
        try {
            return luatable.get("IsDamageable").call(thismeta).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsDamageable");

            return this.maxDamage > 0;
        }
    }

    /**
     * Current implementations of this method in child classes do not use the entry argument beside ev. They just raise
     * the damage on the stack.
     */
    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker){
        try {
            return luatable.get("HitEntity").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(target),CoerceJavaToLua.coerce(attacker)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:HitEntity");

            return false;
        }
    }

    /**
     * Called when a Block is destroyed using this Item. Return true to trigger the "Use Item" statistic.
     */
    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving){
        try {
            return luatable.get("OnBlockDestroyed").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(worldIn),CoerceJavaToLua.coerce(state),CoerceJavaToLua.coerce(pos),CoerceJavaToLua.coerce(entityLiving)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnBlockDestroyed");

            return false;
        }
    }

    /**
     * Check whether this Item can harvest the given Block
     */
    @Override
    public boolean canHarvestBlock(BlockState blockIn){
        try {
            return luatable.get("CanHarvestBlock").call(thismeta,CoerceJavaToLua.coerce(blockIn)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:CanHarvestBlock");

            return false;
        }
    }

    /**
     * Returns true if the item can be used on the given entity, e.g. shears on sheep.
     */
    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand){
        try {
            return (ActionResultType)luatable.get("ItemInteractionForEntity").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(playerIn),CoerceJavaToLua.coerce(target),CoerceJavaToLua.coerce(hand)).touserdata(ActionResultType.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:ItemInteractionForEntity");

            return ActionResultType.PASS;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ITextComponent getName(){
        LuaValue luaname = luatable.get("Name");

        if (!luaname.isnil() && !luaname.tojstring().equals(this.getTranslationKey())) return new StringTextComponent(luaname.tojstring());
        else return new TranslationTextComponent(this.getTranslationKey());
    }

    @Override
    public String toString(){
        return Registry.ITEM.getKey(this).getPath();
    }

    protected String getDefaultTranslationKey(){
        if (this.translationKey == null){
            this.translationKey = Util.makeTranslationKey("item", Registry.ITEM.getKey(this));
        }

        return this.translationKey;
    }

    /**
     * Returns the unlocalized name of this item.
     */
    public String getTranslationKey(){
        return this.getDefaultTranslationKey();
    }

    /**
     * Returns the unlocalized name of this item. This version accepts an ItemStack so different stacks can have
     * different names based on their damage or NBT.
     */
    public String getTranslationKey(ItemStack stack){
        return this.getTranslationKey();
    }

    /**
     * If this function returns true (or the item is damageable), the ItemStack's NBT tag will be sent to the client.
     */
    @Override
    public boolean shouldSyncTag(){
        try {
            return luatable.get("ShouldSyncTag").call(thismeta).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:ShouldSyncTag");

            return true;
        }
    }

    /**
     * True if this Item has a container item (a.k.a. crafting result)
     */
    @Deprecated // Use ItemStack sensitive version.
    @Override
    public boolean hasContainerItem(){
        return this.containerItem != null;
    }

    @Override
    public boolean hasContainerItem(ItemStack stack){
        return getItem().hasContainerItem();
    }

    /**
     * Called each tick as long the item is on a player inventory. Uses by maps to check if is on a player hand and
     * update it's contents.
     */
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected){
        try {
            luatable.get("InventoryTick").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(worldIn),CoerceJavaToLua.coerce(entityIn),LuaValue.valueOf(itemSlot),LuaValue.valueOf(isSelected));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:InventoryTick");
        }
    }

    /**
     * Called when item is crafted/smelted. Used only by maps so far.
     */
    @Override
    public void onCreated(ItemStack stack, World worldIn, PlayerEntity playerIn){
        try {
            luatable.get("OnCreated").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(worldIn),CoerceJavaToLua.coerce(playerIn));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnCreated");
        }
    }

    /**
     * Returns {@code true} if this is a complex item.
     */
    @Override
    public boolean isComplex(){
        try {
            return luatable.get("IsComplex").call(thismeta).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsComplex");

            return false;
        }
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public UseAction getUseAction(ItemStack stack){
        try {
            return (UseAction)luatable.get("GetUseAction").call(thismeta,CoerceJavaToLua.coerce(stack)).touserdata(UseAction.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetUseAction");

            return UseAction.NONE;
        }
    }

    /**
     * How long it takes to use or consume an item
     */
    @Override
    public int getUseDuration(ItemStack stack){
        try {
            return luatable.get("GetUseDuration").call(thismeta,CoerceJavaToLua.coerce(stack)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetUseDuration");

            return 0;
        }
    }

    /**
     * Called when the player stops using an Item (stops holding the right mouse button).
     */
    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft){
        try {
            luatable.get("OnPlayerStoppedUsing").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(worldIn),CoerceJavaToLua.coerce(entityLiving),LuaValue.valueOf(timeLeft));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnPlayerStoppedUsing");
        }
    }

    /**
     * allows items to add custom lines of information to the mouseover description
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn){
        try {
            luatable.get("AddInformation").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(worldIn),CoerceJavaToLua.coerce(tooltip),CoerceJavaToLua.coerce(flagIn));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:AddInformation");
        }
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack){
        LuaValue luaname = luatable.get("Name");

        if (!luaname.isnil() && !luaname.tojstring().equals(this.getTranslationKey(stack))) return new StringTextComponent(luaname.tojstring());
        else return new TranslationTextComponent(this.getTranslationKey(stack));
    }

    /**
     * Returns true if this item has an enchantment glint. By default, this returns <code>stack.isItemEnchanted()</code>,
     * but other items can override it (for instance, written books always return true).
     *
     * Note that if you override this method, you generally want to also call the super version (on {@link Item}) to get
     * the glint for enchanted items. Of course, that is unnecessary if the overwritten version always returns true.
     */
    @Override
    public boolean hasEffect(ItemStack stack){
        try {
            return luatable.get("HasEffect").call(thismeta,CoerceJavaToLua.coerce(stack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:HasEffect");

            return stack.isEnchanted();
        }
    }

    /**
     * Return an item rarity from EnumRarity
     */
    @Override
    public Rarity getRarity(ItemStack stack){
        try {
            return (Rarity)luatable.get("GetRarity").call(thismeta,CoerceJavaToLua.coerce(stack)).touserdata(Rarity.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetRarity");

            if (!stack.isEnchanted()){
                return this.rarity;
            } else {
                switch(this.rarity){
                    case COMMON:
                    case UNCOMMON:
                        return Rarity.RARE;
                    case RARE:
                        return Rarity.EPIC;
                    case EPIC:
                    default:
                        return this.rarity;
                }
            }
        }
    }

    /**
     * Checks isDamagable and if it cannot be stacked
     */
    @Override
    public boolean isEnchantable(ItemStack stack){
        try {
            return luatable.get("IsEnchantable").call(thismeta,CoerceJavaToLua.coerce(stack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsEnchantable");

            return this.getItemStackLimit(stack) == 1 && this.isDamageable(stack);
        }
    }

    protected static BlockRayTraceResult rayTrace(World worldIn, PlayerEntity player, RayTraceContext.FluidMode fluidMode){
        float f = player.rotationPitch;
        float f1 = player.rotationYaw;
        Vector3d vector3d = player.getEyePosition(1.0F);
        float f2 = MathHelper.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = MathHelper.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = -MathHelper.cos(-f * ((float)Math.PI / 180F));
        float f5 = MathHelper.sin(-f * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = player.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue();;
        Vector3d vector3d1 = vector3d.add((double)f6 * d0, (double)f5 * d0, (double)f7 * d0);
        return worldIn.rayTraceBlocks(new RayTraceContext(vector3d, vector3d1, RayTraceContext.BlockMode.OUTLINE, fluidMode, player));
    }

    /**
     * Return the enchantability factor of the item, most of the time is based on material.
     */
    @Override
    public int getItemEnchantability(){
        try {
            return luatable.get("GetItemEnchantability").call(thismeta).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetItemEnchantability");

            return 0;
        }
    }

    /**
     * returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
     */
    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items){
        try {
            luatable.get("FillItemGroup").call(thismeta,CoerceJavaToLua.coerce(group),CoerceJavaToLua.coerce(items));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:FillItemGroup");
        }

        if (this.isInGroup(group)){
            items.add(new ItemStack(this));
        }
    }

    @Override
    protected boolean isInGroup(ItemGroup group){
        if (getCreativeTabs().stream().anyMatch(tab -> tab == group)) return true;
        ItemGroup itemgroup = this.getGroup();
        return itemgroup != null && (group == ItemGroup.SEARCH || group == itemgroup);
    }

    /**
     * Return whether this item is repairable in an anvil.
     */
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair){
        try {
            return luatable.get("GetIsRepairable").call(thismeta,CoerceJavaToLua.coerce(toRepair),CoerceJavaToLua.coerce(repair)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetIsRepairable");

            return false;
        }
    }

    /**
     * Gets a map of item attribute modifiers, used by ItemSword to increase hit damage.
     */
    @Deprecated // Use ItemStack sensitive version.
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType equipmentSlot){
        return ImmutableMultimap.of();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack){
        try {
            return (Multimap)luatable.get("GetAttributeModifiers").call(thismeta,CoerceJavaToLua.coerce(slot),CoerceJavaToLua.coerce(stack)).touserdata(Multimap.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetAttributeModifiers");

            return getItem().getAttributeModifiers(slot);
        }
    }

    @Nullable
//    private final java.util.function.Supplier<net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer> ister;
    public final java.util.Map<net.minecraftforge.common.ToolType, Integer> toolClasses = Maps.newHashMap();
    public final net.minecraftforge.common.util.ReverseTagWrapper<Item> reverseTags = new net.minecraftforge.common.util.ReverseTagWrapper<>(this, net.minecraft.tags.ItemTags::getCollection);
    protected final boolean canRepair;

    @Override
    public boolean isRepairable(ItemStack stack){
        try {
            return luatable.get("IsRepairable").call(thismeta,CoerceJavaToLua.coerce(stack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsRepairable");

            return canRepair && isDamageable(stack);
        }
    }

    @Override
    public java.util.Set<ToolType> getToolTypes(ItemStack stack){
        try {
            return (java.util.Set)luatable.get("GetToolTypes").call(thismeta,CoerceJavaToLua.coerce(stack)).touserdata(java.util.Set.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetToolTypes");

            assert toolClasses != null;
            return toolClasses.keySet();
        }
    }

    @Override
    public int getHarvestLevel(ItemStack stack, ToolType tool, @Nullable PlayerEntity player, @Nullable BlockState blockState){
        try {
            return luatable.get("GetHarvestLevel").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(tool),CoerceJavaToLua.coerce(player),CoerceJavaToLua.coerce(blockState)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetHarvestLevel");

            assert toolClasses != null;
            return toolClasses.getOrDefault(tool, -1);
        }
    }

    @Override
    public java.util.Set<net.minecraft.util.ResourceLocation> getTags(){
        try {
            return (java.util.Set)luatable.get("GetTags").call(thismeta).touserdata(java.util.Set.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetTags");

            return reverseTags.getTagNames();
        }
    }

    /**
     * If this itemstack's item is a crossbow
     */
    @Override
    public boolean isCrossbow(ItemStack stack){
        try {
            return luatable.get("IsCrossbow").call(thismeta,CoerceJavaToLua.coerce(stack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsCrossbow");

            return stack.getItem() == Items.CROSSBOW;
        }
    }

    @Override
    public ItemStack getDefaultInstance(){
        return new ItemStack(this);
    }

    @Override
    public boolean isIn(ITag<Item> tagIn){
        try {
            return luatable.get("IsIn").call(thismeta,CoerceJavaToLua.coerce(tagIn)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsIn");

            return tagIn.contains(this);
        }
    }

    @Override
    public boolean isFood(){
        return this.food != null;
    }

    @Nullable
    @Override
    public Food getFood(){
        return this.food;
    }

    @Override
    public SoundEvent getDrinkSound(){
        try {
            return (SoundEvent)luatable.get("GetDrinkSound").call(thismeta).touserdata(SoundEvent.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetDrinkSound");

            return SoundEvents.ENTITY_GENERIC_DRINK;
        }
    }

    @Override
    public SoundEvent getEatSound(){
        try {
            return (SoundEvent)luatable.get("GetEatSound").call(thismeta).touserdata(SoundEvent.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetEatSound");

            return SoundEvents.ENTITY_GENERIC_EAT;
        }
    }

    @Override
    public boolean isImmuneToFire(){
        try {
            return luatable.get("IsImmuneToFire").call(thismeta).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsImmuneToFire");

            return this.burnable;
        }
    }

    @Override
    public boolean isDamageable(DamageSource damageSource){
        try {
            return luatable.get("IsDamageable").call(thismeta,CoerceJavaToLua.coerce(damageSource)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsDamageable");

            return !this.isImmuneToFire() || !damageSource.isFireDamage();
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, PlayerEntity player){
        try {
            return luatable.get("OnDroppedByPlayer").call(thismeta,CoerceJavaToLua.coerce(item),CoerceJavaToLua.coerce(player)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnDroppedByPlayer");

            return true;
        }
    }

    @Override
    public ITextComponent getHighlightTip(ItemStack item, ITextComponent displayName){
        try {
            return (ITextComponent)luatable.get("GetHighlightTip").call(thismeta,CoerceJavaToLua.coerce(item),CoerceJavaToLua.coerce(displayName)).touserdata(ITextComponent.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetHighlightTip");

            return displayName;
        }
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context){
        try {
            return (ActionResultType)luatable.get("OnItemUseFirst").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(context)).touserdata(ActionResultType.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnItemUseFirst");

            return ActionResultType.PASS;
        }
    }

    @Override
    public boolean isPiglinCurrency(ItemStack stack){
        try {
            return luatable.get("IsPiglinCurrency").call(thismeta,CoerceJavaToLua.coerce(stack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsPiglinCurrency");

            return stack.getItem() == Items.GOLD_INGOT;
        }
    }

    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer){
        try {
            return luatable.get("MakesPiglinsNeutral").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(wearer)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:MakesPiglinsNeutral");

            return stack.getItem() instanceof ArmorItem && ((ArmorItem) stack.getItem()).getArmorMaterial() == ArmorMaterial.GOLD;
        }
    }

    @Override
    public float getXpRepairRatio(ItemStack stack){
        try {
            return luatable.get("GetXpRepairRatio").call(thismeta,CoerceJavaToLua.coerce(stack)).tofloat();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetXpRepairRatio");

            return 2f;
        }
    }

    @Nullable
    @Override
    public CompoundNBT getShareTag(ItemStack stack){
        try {
            return (CompoundNBT)luatable.get("GetShareTag").call(thismeta,CoerceJavaToLua.coerce(stack)).touserdata(CompoundNBT.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetShareTag");

            return stack.getTag();
        }
    }

    @Override
    public void readShareTag(ItemStack stack,@Nullable CompoundNBT nbt){
        try {
            luatable.get("ReadShareTag").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(nbt));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:ReadShareTag");

            stack.setTag(nbt);
        }
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack,BlockPos pos,PlayerEntity player){
        try {
            return luatable.get("OnBlockStartBreak").call(thismeta,CoerceJavaToLua.coerce(itemstack),CoerceJavaToLua.coerce(pos),CoerceJavaToLua.coerce(player)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnBlockStartBreak");

            return false;
        }
    }

    @Override
    public void onUsingTick(ItemStack stack,LivingEntity player,int count){
        try {
            luatable.get("OnUsingTick").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(player),LuaValue.valueOf(count));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnUsingTick");
        }
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, PlayerEntity player, Entity entity){
        try {
            return luatable.get("OnLeftClickEntity").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(player),CoerceJavaToLua.coerce(entity)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnLeftClickEntity");

            return false;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemStack getContainerItem(ItemStack itemStack)
    {
        try {
            return (ItemStack)luatable.get("GetContainerItem").call(thismeta,CoerceJavaToLua.coerce(itemStack)).touserdata(ItemStack.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetContainerItem");

            if (!hasContainerItem(itemStack))
            {
                return ItemStack.EMPTY;
            }
            return new ItemStack(getItem().getContainerItem());
        }
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, World world){
        try {
            return luatable.get("GetEntityLifespan").call(thismeta,CoerceJavaToLua.coerce(itemStack),CoerceJavaToLua.coerce(world)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetEntityLifespan");

            return 6000;
        }
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack){
        try {
            return luatable.get("HasCustomEntity").call(thismeta,CoerceJavaToLua.coerce(stack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:HasCustomEntity");

            return false;
        }
    }

    @Override
    @Nullable
    public Entity createEntity(World world, Entity location, ItemStack itemstack){
        try {
            return (Entity)luatable.get("CreateEntity").call(thismeta,CoerceJavaToLua.coerce(world),CoerceJavaToLua.coerce(location),CoerceJavaToLua.coerce(itemstack)).touserdata(Entity.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:CreateEntity");

            return null;
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity){
        try {
            return luatable.get("OnEntityItemUpdate").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(entity)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnEntityItemUpdate");

            return false;
        }
    }

    @Override
    public java.util.Collection<ItemGroup> getCreativeTabs(){
        try {
            return java.util.Collections.singletonList((ItemGroup)luatable.get("GetCreativeTabs").call(thismeta).touserdata(ItemGroup.class));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetCreativeTabs");

            return java.util.Collections.singletonList(getItem().getGroup());
        }
    }

    @Override
    public float getSmeltingExperience(ItemStack item){
        try {
            return luatable.get("GetSmeltingExperience").call(thismeta,CoerceJavaToLua.coerce(item)).tofloat();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetSmeltingExperience");

            return -1; // -1 will default to the old lookups.
        }
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.IWorldReader world, BlockPos pos, PlayerEntity player){
        try {
            return luatable.get("DoesSneakBypassUse").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(world),CoerceJavaToLua.coerce(pos),CoerceJavaToLua.coerce(player)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:DoesSneakBypassUse");

            return false;
        }
    }

    @Override
    public void onArmorTick(ItemStack stack, World world, PlayerEntity player){
        try {
            luatable.get("OnArmorTick").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(world),CoerceJavaToLua.coerce(player));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnArmorTick");
        }
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlotType armorType, Entity entity){
        try {
            return luatable.get("CanEquip").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(armorType),CoerceJavaToLua.coerce(entity)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:CanEquip");

            return MobEntity.getSlotForItemStack(stack) == armorType;
        }
    }

    @Nullable
    @Override
    public EquipmentSlotType getEquipmentSlot(ItemStack stack){
        try {
            return (EquipmentSlotType)luatable.get("GetEquipmentSlot").call(thismeta,CoerceJavaToLua.coerce(stack)).touserdata(EquipmentSlotType.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetEquipmentSlot");

            return null;
        }
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book){
        try {
            return luatable.get("IsBookEnchantable").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(book)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsBookEnchantable");

            return true;
        }
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type){
        try {
            return luatable.get("GetArmorTexture").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(entity),CoerceJavaToLua.coerce(slot),CoerceJavaToLua.coerce(type)).tojstring();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetArmorTexture");

            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    @Override
    public net.minecraft.client.gui.FontRenderer getFontRenderer(ItemStack stack)
    {
        try {
            return (net.minecraft.client.gui.FontRenderer)luatable.get("GetFontRenderer").call(thismeta,CoerceJavaToLua.coerce(stack)).touserdata(net.minecraft.client.gui.FontRenderer.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetFontRenderer");

            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    @Override
    public <A extends BipedModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlotType armorSlot, A _default)
    {
        try {
            return (A)luatable.get("GetArmorModel").call(thismeta,CoerceJavaToLua.coerce(entityLiving),CoerceJavaToLua.coerce(itemStack),CoerceJavaToLua.coerce(armorSlot),CoerceJavaToLua.coerce(_default)).touserdata();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetArmorModel");

            return null;
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity){
        try {
            return luatable.get("OnEntitySwing").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(entity)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnEntitySwing");

            return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderHelmetOverlay(ItemStack stack, PlayerEntity player, int width, int height, float partialTicks){
        try {
            luatable.get("RenderHelmetOverlay").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(player),LuaValue.valueOf(width),LuaValue.valueOf(height),LuaValue.valueOf(partialTicks));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:RenderHelmetOverlay");
        }
    }

    @Override
    public int getDamage(ItemStack stack){
        try {
            return luatable.get("GetDamage").call(thismeta,CoerceJavaToLua.coerce(stack)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetDamage");

            return !stack.hasTag() ? 0 : stack.getTag().getInt("Damage");
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack){
        try {
            return luatable.get("ShowDurabilityBar").call(thismeta,CoerceJavaToLua.coerce(stack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:ShowDurabilityBar");

            return stack.isDamaged();
        }
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack){
        try {
            return luatable.get("GetDurabilityForDisplay").call(thismeta,CoerceJavaToLua.coerce(stack)).todouble();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetDurabilityForDisplay");

            return (double) stack.getDamage() / (double) stack.getMaxDamage();
        }
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack){
        try {
            return luatable.get("GetRGBDurabilityForDisplay").call(thismeta,CoerceJavaToLua.coerce(stack)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetRGBDurabilityForDisplay");

            return MathHelper.hsvToRGB(Math.max(0.0F, (float) (1.0F - getDurabilityForDisplay(stack))) / 3.0F, 1.0F, 1.0F);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getMaxDamage(ItemStack stack){
        try {
            return luatable.get("GetMaxDamage").call(thismeta,CoerceJavaToLua.coerce(stack)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetMaxDamage");

            return getItem().getMaxDamage();
        }
    }

    @Override
    public boolean isDamaged(ItemStack stack){
        try {
            return luatable.get("IsDamaged").call(thismeta,CoerceJavaToLua.coerce(stack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsDamaged");

            return stack.getDamage() > 0;
        }
    }

    @Override
    public void setDamage(ItemStack stack,int damage){
        try {
            luatable.get("SetDamage").call(thismeta,CoerceJavaToLua.coerce(stack),LuaValue.valueOf(damage));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:SetDamage");

            stack.getOrCreateTag().putInt("Damage", Math.max(0, damage));
        }
    }

    @Override
    public boolean canHarvestBlock(ItemStack stack, BlockState state){
        try {
            return luatable.get("CanHarvestBlock").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(state)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:CanHarvestBlock");

            return getItem().canHarvestBlock(state);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getItemStackLimit(ItemStack stack){
        try {
            return luatable.get("GetItemStackLimit").call(thismeta,CoerceJavaToLua.coerce(stack)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetItemStackLimit");

            return getItem().getMaxStackSize();
        }
    }

    @Override
    public int getItemEnchantability(ItemStack stack){
        try {
            return luatable.get("GetItemEnchantability").call(thismeta,CoerceJavaToLua.coerce(stack)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetItemEnchantability");

            return getItem().getItemEnchantability();
        }
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.enchantment.Enchantment enchantment){
        try {
            return luatable.get("CanApplyAtEnchantingTable").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(enchantment)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:CanApplyAtEnchantingTable");

            return enchantment.type.canEnchantItem(stack.getItem());
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged){
        try {
            return luatable.get("ShouldCauseReequipAnimation").call(thismeta,CoerceJavaToLua.coerce(oldStack),CoerceJavaToLua.coerce(newStack),LuaValue.valueOf(slotChanged)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:ShouldCauseReequipAnimation");

            return !oldStack.equals(newStack); // !ItemStack.areItemStacksEqual(oldStack, newStack);
        }
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack){
        try {
            return luatable.get("ShouldCauseBlockBreakReset").call(thismeta,CoerceJavaToLua.coerce(oldStack),CoerceJavaToLua.coerce(newStack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:ShouldCauseBlockBreakReset");

            return !(newStack.getItem() == oldStack.getItem() && ItemStack.areItemStackTagsEqual(newStack, oldStack)
                    && (newStack.isDamageable() || newStack.getDamage() == oldStack.getDamage()));
        }
    }

    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack){
        try {
            return luatable.get("CanContinueUsing").call(thismeta,CoerceJavaToLua.coerce(oldStack),CoerceJavaToLua.coerce(newStack)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:CanContinueUsing");

            return ItemStack.areItemsEqualIgnoreDurability(oldStack, newStack);
        }
    }

    @Nullable
    @Override
    public String getCreatorModId(ItemStack itemStack){
        try {
            return luatable.get("GetCreatorModId").call(thismeta,CoerceJavaToLua.coerce(itemStack)).tojstring();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetCreatorModId");

            return net.minecraftforge.common.ForgeHooks.getDefaultCreatorModId(itemStack);
        }
    }

    @Nullable
    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt){
        try {
            return (net.minecraftforge.common.capabilities.ICapabilityProvider)luatable.get("InitCapabilities").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(nbt)).touserdata(net.minecraftforge.common.capabilities.ICapabilityProvider.class);
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:InitCapabilities");

            return null;
        }
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker){
        try {
            return luatable.get("CanDisableShield").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(shield),CoerceJavaToLua.coerce(entity),CoerceJavaToLua.coerce(attacker)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:CanDisableShield");

//            return this instanceof AxeItem;
            return false;
        }
    }

    @Override
    public boolean isShield(ItemStack stack, @Nullable LivingEntity entity){
        try {
            return luatable.get("IsShield").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(entity)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsShield");

            return stack.getItem() == Items.SHIELD;
        }
    }

    @Override
    public int getBurnTime(ItemStack itemStack){
        try {
            return luatable.get("GetBurnTime").call(thismeta,CoerceJavaToLua.coerce(itemStack)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:GetBurnTime");

            return -1;
        }
    }

    @Override
    public void onHorseArmorTick(ItemStack stack, World world, MobEntity horse){
        try {
            luatable.get("OnHorseArmorTick").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(world),CoerceJavaToLua.coerce(horse));
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:OnHorseArmorTick");
        }
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken){
        try {
            return luatable.get("DamageItem").call(thismeta,CoerceJavaToLua.coerce(stack),LuaValue.valueOf(amount),CoerceJavaToLua.coerce(entity),CoerceJavaToLua.coerce(onBroken)).toint();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:DamageItem");

            return amount;
        }
    }

    @Override
    public boolean isEnderMask(ItemStack stack, PlayerEntity player, EndermanEntity endermanEntity){
        try {
            return luatable.get("IsEnderMask").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(player),CoerceJavaToLua.coerce(endermanEntity)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:IsEnderMask");

            return stack.getItem() == Blocks.CARVED_PUMPKIN.asItem();
        }
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity){
        try {
            return luatable.get("CanElytraFly").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(entity)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:CanElytraFly");

            return false;
        }
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks){
        try {
            return luatable.get("ElytraFlightTick").call(thismeta,CoerceJavaToLua.coerce(stack),CoerceJavaToLua.coerce(entity),LuaValue.valueOf(flightTicks)).toboolean();
        }
        catch (Exception e){
            lualoader.LuaError(e,"ITEM:ElytraFlightTick");

            return false;
        }
    }

    @Nullable
    public static BlockState getAxeStrippingState(BlockState originalState) {
        return null;
    }
}
