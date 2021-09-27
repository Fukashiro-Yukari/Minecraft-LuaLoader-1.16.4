package com.nepqneko.lualoader.item;

import com.google.common.collect.Sets;
import com.nepqneko.lualoader.lualoader;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Rarity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraftforge.common.ToolType;
import org.luaj.lualoader.vm2.LuaTable;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;

import java.util.Set;
import java.util.concurrent.Callable;

public class luaitemset {
    private LuaValue t;

    public luaitemset(String tn){
        this.t = lualoader.lua.get("Register").get("GetList").call().get("ITEM").get(tn);
    }

    public Item.Properties SetItemPr(){
        Item.Properties pr = new Item.Properties();

        pr = pr.group((ItemGroup)t.get("Group").touserdata(ItemGroup.class));
        pr = pr.rarity((Rarity)t.get("Rarity").touserdata(Rarity.class));
        pr = pr.containerItem((Item)t.get("ContainerItem").touserdata(Item.class));
        pr = pr.maxStackSize(t.get("MaxStackSize").toint());
        pr = pr.maxDamage(t.get("MaxDamage").toint());
        pr = pr.food(new Food.Builder().build());

        if (t.get("ImmuneToFire").toboolean()) pr = pr.isImmuneToFire();
        if (!t.get("CanRepair").toboolean()) pr = pr.setNoRepair();

        Food.Builder food = (new Food.Builder());

        if (t.get("IsMeat").toboolean()) food = food.meat();
        if (t.get("FastToEat").toboolean()) food = food.fastToEat();
        if (t.get("CanEatWhenFull").toboolean()) food = food.setAlwaysEdible();

        try {
            LuaTable tbl = t.get("FoodEffects").checktable();
            LuaValue k = LuaValue.NIL;

            while (true){
                Varargs n = tbl.next(k);

                if ((k = n.arg1()).isnil()) break;

                LuaValue v = n.arg(2);

                food = food.effect(() -> {
                    v.get("func").call();

                    return (EffectInstance) v.get("effect").touserdata(EffectInstance.class);
                },v.get("probability").tofloat());
            }
        }
        catch (Exception e){
            lualoader.LuaError(e,"luaitemset.FoodEffects");
        }

        food = food.hunger(t.get("Hunger").toint());
        food = food.saturation(t.get("Saturation").tofloat());

        pr = pr.food(food.build());

        LuaTable tbl = !t.get("ToolClasses").isnil() ? t.get("ToolClasses").checktable() : null;
        LuaValue k = LuaValue.NIL;

        if (tbl != null){
            while (true){
                Varargs n = tbl.next(k);

                if ((k = n.arg1()).isnil()) break;

                LuaValue v = n.arg(2);

                pr = pr.addToolType((ToolType)k.touserdata(ToolType.class),v.toint());
            }
        }

        return pr;
    }
}
