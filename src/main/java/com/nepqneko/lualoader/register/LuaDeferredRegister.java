/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.nepqneko.lualoader.register;

import com.nepqneko.lualoader.item.luaitem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraftforge.registries.*;
import com.nepqneko.lualoader.lualoader;
import org.luaj.lualoader.vm2.LuaTable;
import org.luaj.lualoader.vm2.LuaValue;
import org.luaj.lualoader.vm2.Varargs;

/**
 * Utility class to help with managing registry entries.
 * Maintains a list of all suppliers for entries and registers them during the proper Register event.
 * Suppliers should return NEW instances every time.
 *
 *Example Usage:
 *<pre>
 *   private static final LuaDeferredRegister<Item> ITEMS = LuaDeferredRegister.create(ForgeRegistries.ITEMS, MODID);
 *   private static final LuaDeferredRegister<Block> BLOCKS = LuaDeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
 *
 *   public static final RegistryObject<Block> ROCK_BLOCK = BLOCKS.register("rock", () -> new Block(Block.Properties.create(Material.ROCK)));
 *   public static final RegistryObject<Item> ROCK_ITEM = ITEMS.register("rock", () -> new BlockItem(ROCK_BLOCK.get(), new Item.Properties().group(ItemGroup.MISC)));
 *
 *   public ExampleMod() {
 *       ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
 *       BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
 *   }
 *</pre>
 *
 * @param <T> The base registry type, must be a concrete base class, do not use subclasses or wild cards.
 */
public class LuaDeferredRegister<T extends IForgeRegistryEntry<T>>
{
    /**
     * Use for vanilla/forge registries. See example above.
     */
    public static <B extends IForgeRegistryEntry<B>> LuaDeferredRegister<B> create(IForgeRegistry<B> reg, String luatype, final Function<String,B> func)
    {
        return new LuaDeferredRegister<B>(reg, luatype, func);
    }

    /**
     * Use for custom registries that are made during the NewRegistry event.
     */
    public static <B extends IForgeRegistryEntry<B>> LuaDeferredRegister<B> create(Class<B> base, String luatype, final Function<String,B> func)
    {
        return new LuaDeferredRegister<B>(base, luatype, func);
    }

    private final Class<T> superType;
    private String modid;
    private final String defmodid;
    private final Map<RegistryObject<T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private final Set<RegistryObject<T>> entriesView = Collections.unmodifiableSet(entries.keySet());
    private final ArrayList<RegistryObject<T>> isreg = new ArrayList<>();
    private final String luatype;
    private final Function<String,T> luaclass;

    private IForgeRegistry<T> type;
    private Supplier<RegistryBuilder<T>> registryFactory;

    private LuaDeferredRegister(Class<T> base, String luatype, final Function<String,T> func)
    {
        this.superType = base;
        this.modid = lualoader.MOD_ID;
        this.defmodid = lualoader.MOD_ID;
        this.luatype = luatype;
        this.luaclass = func;
    }

    private LuaDeferredRegister(IForgeRegistry<T> reg, String luatype, final Function<String,T> func)
    {
        this(reg.getRegistrySuperType(), luatype, func);
        this.type = reg;
    }

    /**
     * Adds a new supplier to the list of entries to be registered, and returns a RegistryObject that will be populated with the created entry automatically.
     *
     * @param name The new entry's name, it will automatically have the modid prefixed.
     * @param sup A factory for the new entry, it should return a new instance every time it is called.
     * @return A RegistryObject that will be updated with when the entries in the registry change.
     */
    @SuppressWarnings("unchecked")
    public RegistryObject<T> register(final String name, final T sup)
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(sup);
        final ResourceLocation key = new ResourceLocation(modid, name);

        RegistryObject<T> ret;
        if (this.type != null)
            ret = RegistryObject.of(key, this.type);
        else if (this.superType != null)
            ret = RegistryObject.of(key, this.superType, this.modid);
        else
            throw new IllegalStateException("Could not create RegistryObject in LuaDeferredRegister");

        if (entries.putIfAbsent(ret,() -> sup.setRegistryName(key)) != null){
            isreg.add(ret);
        }

        return ret;
    }

    /**
     * For custom registries only, fills the {@link #registryFactory} to be called later see {@link #register(IEventBus)}
     *
     * Calls {@link RegistryBuilder#setName} and {@link RegistryBuilder#setType} automatically.
     *
     * @param name  Path of the registry's {@link ResourceLocation}
     * @param sup   Supplier of the RegistryBuilder that is called to fill {@link #type} during the NewRegistry event
     * @return      A supplier of the {@link IForgeRegistry} created by the builder.
     */
    public Supplier<IForgeRegistry<T>> makeRegistry(final String name, final Supplier<RegistryBuilder<T>> sup) {
        if (this.superType == null)
            throw new IllegalStateException("Cannot create a registry without specifying a base type");
        if (this.type != null || this.registryFactory != null)
            throw new IllegalStateException("Cannot create a registry for a type that already exists");

        this.registryFactory = () -> sup.get().setName(new ResourceLocation(modid, name)).setType(this.superType);
        return () -> this.type;
    }

    private void registerluaitem(){
        String errname = "Init";

        LuaTable tbl = null;
        LuaValue k = LuaValue.NIL;

        try {
            lualoader.lua.get("Register").get("OnRegister").call();

            tbl = lualoader.lua.get("Register").get("GetList").call().get(luatype).checktable();
        }
        catch (Exception e){
            lualoader.LuaError(e,"LuaDeferredRegister:"+luatype+"."+errname);
        }

        while (true){
            if (tbl == null) break;

            Varargs n = tbl.next(k);

            if ((k = n.arg1()).isnil()) break;

            LuaValue v = n.arg(2);

            try {
                if (!v.get("Register").toboolean()) continue;

                LuaValue modid = v.get("ModID");

                if (!modid.isnil() && !modid.tojstring().equals("")){
                    this.modid = modid.tojstring().toLowerCase();
                }
                else {
                    this.modid = this.defmodid;
                }

                errname = this.modid+"."+k.tojstring().toLowerCase();

                if (!lualoader.registerslist.containsKey(luatype)) lualoader.registerslist.put(luatype,new HashMap<String,RegistryObject<?>>());

                for(String key:lualoader.registerslist.keySet()){
                    if (key.equals(luatype)){
                        lualoader.registerslist.get(key).put(k.tojstring(),register(k.tojstring().toLowerCase(),luaclass.apply(k.tojstring().toLowerCase())));
                    }
                }
            }
            catch (Exception e){
                lualoader.LuaError(e,"LuaDeferredRegister:"+luatype+"."+errname);
            }
        }
    }

    /**
     * Adds our event handler to the specified event bus, this MUST be called in order for this class to function.
     * See the example usage.
     *
     * @param bus The Mod Specific event bus.
     */
    public void register(IEventBus bus)
    {
        registerluaitem();

        bus.register(new EventDispatcher(this));

        if (this.type == null && this.registryFactory != null){
            bus.addListener(this::createRegistry);
        }
    }
    public static class EventDispatcher {
        private final LuaDeferredRegister<?> register;

        public EventDispatcher(final LuaDeferredRegister<?> register) {
            this.register = register;
        }

        @SubscribeEvent
        public void handleEvent(RegistryEvent.Register<?> event) {
            register.addEntries(event);
        }
    }
    /**
     * @return The unmodifiable view of registered entries. Useful for bulk operations on all values.
     */
    public Collection<RegistryObject<T>> getEntries()
    {
        return entriesView;
    }

    private ForgeRegistry<T> registry;

    public void ReloadLuaMod(){
        registerluaitem();

        registry.unfreeze();

        for (Entry<RegistryObject<T>, Supplier<? extends T>> e : entries.entrySet())
        {
            if (!isreg.contains(e)){
                registry.register(e.getValue().get());
                e.getKey().updateReference(registry);
            }
        }

        //ehh..

        registry.freeze();
    }

    private void addEntries(RegistryEvent.Register<?> event)
    {
        if (this.type == null && this.registryFactory == null)
        {
            //If there is no type yet and we don't have a registry factory, attempt to capture the registry
            //Note: This will only ever get run on the first registry event, as after that time,
            // the type will no longer be null. This is needed here rather than during the NewRegistry event
            // to ensure that mods can properly use deferred registers for custom registries added by other mods
            captureRegistry();
        }
        if (this.type != null && event.getGenericType() == this.type.getRegistrySuperType())
        {
            @SuppressWarnings("unchecked")
            ForgeRegistry<T> reg = (ForgeRegistry<T>)event.getRegistry();

            registry = reg;

            for (Entry<RegistryObject<T>, Supplier<? extends T>> e : entries.entrySet())
            {
                reg.register(e.getValue().get());
                e.getKey().updateReference(reg);
            }
        }
    }

    private void createRegistry(RegistryEvent.NewRegistry event)
    {
        this.type = this.registryFactory.get().create();
    }

    private void captureRegistry()
    {
        if (this.superType != null)
        {
            this.type = RegistryManager.ACTIVE.getRegistry(this.superType);
            if (this.type == null)
                throw new IllegalStateException("Unable to find registry for type " + this.superType.getName() + " for modid \"" + modid + "\" after NewRegistry event");
        }
        else
            throw new IllegalStateException("Unable to find registry for mod \"" + modid + "\" No lookup criteria specified.");
    }
}