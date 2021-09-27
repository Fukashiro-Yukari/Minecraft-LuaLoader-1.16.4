package com.nepqneko.lualoader;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Sets;
import com.itranswarp.lualoader.compiler.JavaStringCompiler;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nepqneko.lualoader.command.luacommand;
import com.nepqneko.lualoader.event.forgeevent;
import com.nepqneko.lualoader.item.luaitem;
import com.nepqneko.lualoader.item.luaitemgroup;
import com.nepqneko.lualoader.lualib.Loader;
import com.nepqneko.lualoader.lualib.LuaJavaThreadLib;
import com.nepqneko.lualoader.lualib.MAPI;
import com.nepqneko.lualoader.lualib.net;
import com.nepqneko.lualoader.register.LuaDeferredRegister;
import com.nepqneko.lualoader.resource.luaresourcepackfinder;
import com.nepqneko.lualoader.screen.LuaConsoleScreen;
import com.nepqneko.lualoader.screen.LuaErrorToast;
import com.nepqneko.lualoader.event.modevent;
import javafx.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.client.event.sound.*;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.event.server.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.luaj.lualoader.vm2.*;
import org.luaj.lualoader.vm2.lib.DebugLib;
import org.luaj.lualoader.vm2.lib.VarArgFunction;
import org.luaj.lualoader.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.lualoader.vm2.lib.jse.JavaClass;
import org.luaj.lualoader.vm2.lib.jse.JsePlatform;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.luaj.lualoader.vm2.LuaValue.NIL;

@Mod(lualoader.MOD_ID)
public class lualoader {
//    public static final lualoader instance = new lualoader();
    public static Globals lua;
    public static final String MOD_ID = "lualoader";
    public static Logger logger = LogManager.getLogger("LuaLoader");
    public static boolean errorinchat;
    private Button button;
    private static List<String> errorlist = new ArrayList<String>();
    public static List<String> msglist = new ArrayList<String>();
    public static List<luaitem> allluaitemlist = new ArrayList<luaitem>();
    public static List<luaitemgroup> allluaitemgrouplist = new ArrayList<luaitemgroup>();
    public static HashMap<String,HashMap<String,RegistryObject<?>>> registerslist = new HashMap<String,HashMap<String,RegistryObject<?>>>();
    public static HashMap<String,luaitemgroup> luaitemgrouplist = new HashMap<String,luaitemgroup>();
    public static MinecraftServer server;
    public static long modtime = 0;
    public static LuaValue mcptosrg;
    public static LuaValue functable;
    public static World world;
    public static final HashMap<File,Long> reloadfiles = new HashMap<>();
    public static final HashMap<File,Pair> reloadfiles2 = new HashMap<>();
    private static final List<LuaValue> luamodinfo = new ArrayList<>();
    private static final List<String> guib = new ArrayList<>();
    private static final ResourceLocation LUA_ICON = new ResourceLocation(lualoader.MOD_ID,"textures/gui/lua.png");
    public static Screen oldscreen;
    public static List<Pair<String,Integer>> lualogs = new ArrayList<>();
    private static final HashMap<String,Pair<Consumer<String[]>,String>> commands = new HashMap<>();
    public static List<String> oldcommands = new ArrayList<>();
    @OnlyIn(Dist.CLIENT)
    public static KeyBinding OPEN_CONSOLE = new KeyBinding("Open Lua Console",GLFW.GLFW_KEY_GRAVE_ACCENT,"Lua");

    public static void MakeDir(File dir){
        try {
            Files.createDirectories(dir.toPath());
        }
        catch (final IOException e) {
            logger.error("Failed to create folder.", e);
        }
    }

    public static void MakeDir(String path){
        MakeDir(new File("lua/"+path));
    }

    public static File[] GetFilesFromDir(File file){
        File[] files = new File[0];

        if (file == null) logger.error("Attempted to read from a null file.");
        else if (!file.isDirectory()) logger.error("Can not read from {}. It's not a directory.", file.getAbsolutePath());
        else {
            try {
                final File[] readFiles = file.listFiles();

                if (readFiles == null){
                    logger.error("Could not read from {} due to a system error. This is likely an issue with your computer.", file.getAbsolutePath());
                }
                else {
                    files = readFiles;
                }
            }
            catch (final SecurityException e){
                logger.error("Could not read from {}. Blocked by system level security. This is likely an issue with your computer.", file.getAbsolutePath(), e);
            }
        }

        return files;
    }

    public static void OpenConsole(){
        Screen luac = new LuaConsoleScreen(new StringTextComponent("Lua Console"));

        if (Minecraft.getInstance().currentScreen == null || !Minecraft.getInstance().currentScreen.getClass().getName().equals(luac.getClass().getName())){
            oldscreen = Minecraft.getInstance().currentScreen;
            Minecraft.getInstance().displayGuiScreen(luac);
        }
        else {
            Minecraft.getInstance().displayGuiScreen(oldscreen);
        }
    }

    public static void RunCommand(String com,String... args){
        if (commands.containsKey(com)){
            commands.get(com).getKey().accept(args);
        }
        else if (!com.equals("")) {
            LuaLogsAdd("Unknown command ("+com+")");
        }
    }

    public static void AddCommand(String com,Consumer<String[]> func){
        AddCommand(com,func,"");
    }

    public static void AddCommand(String com,Consumer<String[]> func,String help){
        commands.put(com,new Pair<>(func,help));
    }

    public static void LuaLogsAdd(String msg,int col){
        lualoader.lualogs.add(new Pair<>(msg,col));
    }

    public static void LuaLogsAdd(String msg){
        LuaLogsAdd(msg,0xFFFFFF);
    }

    public static void LuaError(Exception e,String info){
        logger.error(IsClient() ? "(Client) " : "(Server) "+"("+info+") "+e.toString());

        if (FMLEnvironment.dist == Dist.CLIENT){
            lualoader.LuaLogsAdd("("+info+") "+e.toString(),IsClient() ? 0xFFC800 : 0x3200FF);
            Minecraft.getInstance().getToastGui().add(new LuaErrorToast(info,IsClient() ? 0xFFC800 : 0x3200FF));
        }
    }

    public static void LuaError(Exception e){
        logger.error(IsClient() ? "(Client) " : "(Server) "+e.toString());

        if (FMLEnvironment.dist == Dist.CLIENT){
            lualoader.LuaLogsAdd(e.toString(),IsClient() ? 0xFFC800 : 0x3200FF);
            Minecraft.getInstance().getToastGui().add(new LuaErrorToast(IsClient() ? 0xFFC800 : 0x3200FF));
        }
    }

    public static void LuaError(String e){
        logger.error(IsClient() ? "(Client) " : "(Server) "+e);

        if (FMLEnvironment.dist == Dist.CLIENT){
            lualoader.LuaLogsAdd(e,IsClient() ? 0xFFC800 : 0x3200FF);
            Minecraft.getInstance().getToastGui().add(new LuaErrorToast(IsClient() ? 0xFFC800 : 0x3200FF));
        }
    }

    public static void printStackTrace(Exception e){
        printStackTrace(e,"");
    }

    public static void printStackTrace(Exception e,String info){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            e.printStackTrace(pw);

            LuaError(!info.equals("") ? "("+info+") "+sw.toString() : sw.toString());
        }
        finally {
            pw.close();
        }
    }

    private static void loadingluapath(String path){
        File file = new File("lua/"+path);

        MakeDir(file);

        for (File f:GetFilesFromDir(file)){
            String ext = FilenameUtils.getExtension(f.getName());

            if (f.isFile() && ext.equals("lua")){
                try {
                    lua.loadfile(f.getPath()).call();
                    reloadfiles.put(f,f.lastModified());
                }
                catch (LuaError e){
                    LuaError(e,"loadingluapath:"+path);
                }
            }
        }

        File file2 = new File("lua/mods");

        MakeDir(file2);

        for (File f2:GetFilesFromDir(file2)){
            String ext2 = FilenameUtils.getExtension(f2.getName());

            if (f2.isFile() && ext2.equals("zip")){
                String zippath = "lua/mods/"+f2.getName();

                try {
                    ZipFile zf = new ZipFile(zippath);
                    InputStream in = new BufferedInputStream(new FileInputStream(zippath));
                    ZipInputStream zin = new ZipInputStream(in);
                    ZipEntry ze;

                    while ((ze = zin.getNextEntry()) != null){
                        if (!ze.isDirectory() && ze.getName().equals("info.lua")){
                            try {
                                Globals lua = JsePlatform.standardGlobals();
                                lua.load(zf.getInputStream(ze),"@"+zf.getName()+"/"+ze.getName(),"bt",lua).call();
                                luamodinfo.add(lua);
                            }
                            catch (Exception e){
                                LuaError(e,"loadingluapath:"+zf.getName()+"/"+ze.getName());
                            }
                        }
                        if (!ze.isDirectory() && ze.getName().startsWith(path)){
                            try {
                                lua.load(zf.getInputStream(ze),"@"+zf.getName()+"/"+ze.getName(),"bt",lua).call();
                            }
                            catch (Exception e){
                                LuaError(e,"loadingluapath:"+zf.getName()+"/"+ze.getName());
                            }
                        }
                    }
                }
                catch (IOException e){
                    LuaError(e);
                }
            }
            else if (f2.isDirectory()){
                File file3 = new File("lua/mods/"+f2.getName()+"/"+path);
                File file4 = new File("lua/mods/"+f2.getName()+"/info.lua");

                if (file4.isFile()){
                    try {
                        Globals lua = JsePlatform.standardGlobals();
                        lua.loadfile(file4.getPath()).call();
                        luamodinfo.add(lua);
                    }
                    catch (Exception e){
                        LuaError(e,"loadingluapath:"+file4.getPath());
                    }
                }
                if (file3.isDirectory()){
                    for (File f3:GetFilesFromDir(file3)){
                        String ext3 = FilenameUtils.getExtension(f3.getName());

                        if (f3.isFile() && ext3.equals("lua")){
                            try {
                                lua.loadfile(f3.getPath()).call();
                                reloadfiles.put(f3,f3.lastModified());
                            }
                            catch (LuaError e){
                                LuaError(e,"loadingluapath:"+f3.getPath());
                            }
                        }
                    }
                }
            }
        }
    }

    private static void loadluapath(File f){
        String ext = FilenameUtils.getExtension(f.getName());

        if (f.isFile() && ext.equals("lua")){
            try {
                lua.loadfile(f.getPath()).call();
            }
            catch (LuaError e){
                LuaError(e,"loadluapath:"+f.getPath());
            }
        }
    }

    private static void loadingluapathinittbl(String path, String tabname){
        File file = new File("lua/"+path);

        MakeDir(file);

        for (File f:GetFilesFromDir(file)){
            String ext = FilenameUtils.getExtension(f.getName());
            String basename = FilenameUtils.getBaseName(f.getName());

            if (f.isFile() && ext.equals("lua")){
                try {
                    lua.set(tabname,LuaValue.tableOf());

                    lua.loadfile(f.getPath()).call();

                    lua.get("Register").get("Add").call(LuaValue.valueOf(tabname),lua.get(tabname),LuaValue.valueOf(basename));
                    lua.set(tabname,LuaValue.NIL);
                    reloadfiles2.put(f,new Pair(tabname,f.lastModified()));
                }
                catch (Exception e){
                    LuaError(e,"loadingluapathinittbl:"+path+"."+tabname);
                }
            }
        }

        File file2 = new File("lua/mods");

        MakeDir(file2);

        for (File f2:GetFilesFromDir(file2)){
            String ext2 = FilenameUtils.getExtension(f2.getName());

            if (f2.isFile() && ext2.equals("zip")){
                String zippath = "lua/mods/"+f2.getName();

                try {
                    ZipFile zf = new ZipFile(zippath);
                    InputStream in = new BufferedInputStream(new FileInputStream(zippath));
                    ZipInputStream zin = new ZipInputStream(in);
                    ZipEntry ze;

                    while ((ze = zin.getNextEntry()) != null){
                        if (!ze.isDirectory() && ze.getName().startsWith(path)){
                            try {
                                lua.set(tabname,LuaValue.tableOf());
                                String basename = FilenameUtils.getBaseName(ze.getName());

                                lua.load(zf.getInputStream(ze),"@"+zf.getName()+"/"+ze.getName(),"bt",lua).call();

                                lua.get("Register").get("Add").call(LuaValue.valueOf(tabname),lua.get(tabname),LuaValue.valueOf(basename));
                                lua.set(tabname,LuaValue.NIL);
                            }
                            catch (Exception e){
                                LuaError(e,"loadingluapath:"+zf.getName()+"/"+ze.getName());
                            }
                        }
                    }
                }
                catch (IOException e){
                    LuaError(e,"loadingluapath:"+zippath);
                }
            }
            else if (f2.isDirectory()){
                File file3 = new File("lua/mods/"+f2.getName()+"/"+path);

                if (file3.isDirectory()){
                    for (File f3:GetFilesFromDir(file3)){
                        String ext3 = FilenameUtils.getExtension(f3.getName());
                        String basename2 = FilenameUtils.getBaseName(f3.getName());

                        if (f3.isFile() && ext3.equals("lua")){
                            try {
                                lua.set(tabname,LuaValue.tableOf());

                                lua.loadfile(f3.getPath()).call();

                                lua.get("Register").get("Add").call(LuaValue.valueOf(tabname),lua.get(tabname),LuaValue.valueOf(basename2));
                                lua.set(tabname,LuaValue.NIL);
                                reloadfiles2.put(f3,new Pair(tabname,f3.lastModified()));
                            }
                            catch (LuaError e){
                                LuaError(e,"loadingluapath:"+f3.getPath());
                            }
                        }
                    }
                }
            }
        }
    }

    private static void loadluapathinittbl(File f,String tabname){
        String ext = FilenameUtils.getExtension(f.getName());
        String basename = FilenameUtils.getBaseName(f.getName());

        if (f.isFile() && ext.equals("lua")){
            try {
                lua.set(tabname,LuaValue.tableOf());

                lua.loadfile(f.getPath()).call();

                lua.get("Register").get("Add").call(LuaValue.valueOf(tabname),lua.get(tabname),LuaValue.valueOf(basename));
                lua.set(tabname,LuaValue.NIL);
                ReGetLuaTable();
            }
            catch (Exception e){
                LuaError(e,"loadluapathinittbl:"+f.getPath()+"."+tabname);
            }
        }
    }

    private void initlua(){
        Globals getobfuscatedtbl = JsePlatform.standardGlobals();

        try {
            getobfuscatedtbl.loadfile("lualoader.getobfuscatedtbl.lua").call();

            mcptosrg = getobfuscatedtbl.get("GetTable");
            functable = getobfuscatedtbl.get("GetFuncTable");
        }
        catch (Exception e){
            printStackTrace(e,"Serious error: Obfuscated table failed to get");
        }

        lua = JsePlatform.standardGlobals();
        lua.load(new DebugLib());
        lua.load(new MAPI());
        lua.load(new Loader());
        lua.load(new LuaJavaThreadLib());
        lua.load(new net());

        lua.get("luajava").set("runString",new runString());
        lua.set("new", LuaValue.tableOf());

        LuaTable _gmeta = LuaValue.tableOf();

        _gmeta.set("__index",new _Gi());

        lua.setmetatable(_gmeta);

        LuaTable newmeta = LuaValue.tableOf();

        newmeta.set("__index",new New());
        newmeta.set("__newindex",new New2());

        lua.get("new").setmetatable(newmeta);
        lua.set("instanceof",new Luainstanceof());
        lua.set("todouble",new todouble());
        lua.set("Consumer",new LuaConsumer());
        lua.set("Supplier",new LuaSupplier());
        lua.set("Predicate",new LuaPredicate());
        lua.set("Function",new LuaFunction());
        lua.set("Runnable",new LuaRunnable());
        lua.set("LocalPlayer",new LocalPlayer());

        lua.set("ATTACK_DAMAGE_MODIFIER",CoerceJavaToLua.coerce(UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF")));
        lua.set("ATTACK_SPEED_MODIFIER",CoerceJavaToLua.coerce(UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3")));
        lua.set("UUID",CoerceJavaToLua.coerce(UUID.class));
        lua.set("ItemGroup",CoerceJavaToLua.coerce(ItemGroup.class));
        lua.set("Effects",CoerceJavaToLua.coerce(Effects.class));
        lua.set("Rarity",CoerceJavaToLua.coerce(Rarity.class));
        lua.set("ActionResultType",CoerceJavaToLua.coerce(ActionResultType.class));
        lua.set("ActionResult",CoerceJavaToLua.coerce(ActionResult.class));
        lua.set("Hand",CoerceJavaToLua.coerce(Hand.class));
        lua.set("EffectInstance",CoerceJavaToLua.coerce(EffectInstance.class));
        lua.set("UseAction",CoerceJavaToLua.coerce(UseAction.class));
        lua.set("Items",CoerceJavaToLua.coerce(Items.class));
        lua.set("SoundEvents",CoerceJavaToLua.coerce(SoundEvents.class));
        lua.set("SoundCategory",CoerceJavaToLua.coerce(SoundCategory.class));
        lua.set("Block",CoerceJavaToLua.coerce(Block.class));
        lua.set("Blocks",CoerceJavaToLua.coerce(Blocks.class));
        lua.set("World",CoerceJavaToLua.coerce(World.class));
        lua.set("Entity",CoerceJavaToLua.coerce(Entity.class));
        lua.set("PlayerEntity",CoerceJavaToLua.coerce(PlayerEntity.class));
        lua.set("ResourceLocation",CoerceJavaToLua.coerce(ResourceLocation.class));
        lua.set("Orientation",CoerceJavaToLua.coerce(Orientation.class));
        lua.set("MathHelper",CoerceJavaToLua.coerce(MathHelper.class));
        lua.set("SectionPos",CoerceJavaToLua.coerce(SectionPos.class));
        lua.set("Ingredient",CoerceJavaToLua.coerce(Ingredient.class));
        lua.set("ItemTags",CoerceJavaToLua.coerce(ItemTags.class));
        lua.set("Material",CoerceJavaToLua.coerce(Material.class));
        lua.set("ArmorMaterial",CoerceJavaToLua.coerce(ArmorMaterial.class));
        lua.set("Item",CoerceJavaToLua.coerce(Item.class));
        lua.set("ItemStack",CoerceJavaToLua.coerce(ItemStack.class));
        lua.set("MobEntity",CoerceJavaToLua.coerce(MobEntity.class));
        lua.set("ForgeHooks",CoerceJavaToLua.coerce(ForgeHooks.class));
        lua.set("ImmutableMultimap",CoerceJavaToLua.coerce(ImmutableMultimap.class));
        lua.set("AttributeModifier",CoerceJavaToLua.coerce(AttributeModifier.class));
        lua.set("Attributes",CoerceJavaToLua.coerce(Attributes.class));
        lua.set("BlockTags",CoerceJavaToLua.coerce(BlockTags.class));
        lua.set("LivingEntity",CoerceJavaToLua.coerce(LivingEntity.class));
        lua.set("EquipmentSlotType",CoerceJavaToLua.coerce(EquipmentSlotType.class));
        lua.set("MinecraftServer",CoerceJavaToLua.coerce(MinecraftServer.class));
        lua.set("ServerWorld",CoerceJavaToLua.coerce(ServerWorld.class));
        lua.set("ToolType",CoerceJavaToLua.coerce(ToolType.class));
        lua.set("Sets",CoerceJavaToLua.coerce(Sets.class));
        lua.set("CampfireBlock",CoerceJavaToLua.coerce(CampfireBlock.class));
        lua.set("Direction",CoerceJavaToLua.coerce(Direction.class));
        lua.set("ForgeEventFactory",CoerceJavaToLua.coerce(ForgeEventFactory.class));
        lua.set("Foods",CoerceJavaToLua.coerce(Foods.class));
        lua.set("EnchantmentHelper",CoerceJavaToLua.coerce(EnchantmentHelper.class));
        lua.set("RenderSystem",CoerceJavaToLua.coerce(RenderSystem.class));
        lua.set("Minecraft",CoerceJavaToLua.coerce(Minecraft.getInstance()));
        lua.set("GLFW",CoerceJavaToLua.coerce(GLFW.class));

        lua.set("Matrix3",JavaClass.forClass(Matrix3f.class).getConstructor());
        lua.set("Matrix4",JavaClass.forClass(Matrix4f.class).getConstructor());
        lua.set("Vector2",JavaClass.forClass(Vector2f.class).getConstructor());
        lua.set("Vector3",JavaClass.forClass(Vector3f.class).getConstructor());
        lua.set("Vector",JavaClass.forClass(Vector3f.class).getConstructor());
        lua.set("Vector4",JavaClass.forClass(Vector4f.class).getConstructor());
        lua.set("Quaternion",JavaClass.forClass(Quaternion.class).getConstructor());
        lua.set("TransformationMatrix",JavaClass.forClass(TransformationMatrix.class).getConstructor());
        lua.set("AxisAlignedBB",JavaClass.forClass(AxisAlignedBB.class).getConstructor());
        lua.set("BlockPos",JavaClass.forClass(BlockPos.class).getConstructor());
        lua.set("BlockPosWrapper",JavaClass.forClass(BlockPosWrapper.class).getConstructor());
        lua.set("BlockRayTraceResult",JavaClass.forClass(BlockRayTraceResult.class).getConstructor());
        lua.set("ChunkPos",JavaClass.forClass(ChunkPos.class).getConstructor());
        lua.set("ColumnPos",JavaClass.forClass(ColumnPos.class).getConstructor());
        lua.set("CubeCoordinateIterator",JavaClass.forClass(CubeCoordinateIterator.class).getConstructor());
        lua.set("EntityPosWrapper",JavaClass.forClass(EntityPosWrapper.class).getConstructor());
        lua.set("EntityRayTraceResult",JavaClass.forClass(EntityRayTraceResult.class).getConstructor());
        lua.set("EntityRayTraceResult",JavaClass.forClass(EntityRayTraceResult.class).getConstructor());
//        lua.set("GlobalPos",JavaClass.forClass(GlobalPos.class).getConstructor());
        lua.set("MutableBoundingBox",JavaClass.forClass(MutableBoundingBox.class).getConstructor());
        lua.set("RayTraceContext",JavaClass.forClass(RayTraceContext.class).getConstructor());
        lua.set("RayTraceContextBlockMode",CoerceJavaToLua.coerce(RayTraceContext.BlockMode.class));
        lua.set("RayTraceContextFluidMode",CoerceJavaToLua.coerce(RayTraceContext.FluidMode.class));
//        lua.set("RayTraceResult",JavaClass.forClass(RayTraceResult.class).getConstructor());
        lua.set("Rotations",JavaClass.forClass(Rotations.class).getConstructor());
        lua.set("Tuple3",JavaClass.forClass(Tuple3d.class).getConstructor());

        LoadingLua();

        //TODO: luamodinfo
        for (LuaValue lua:luamodinfo){
//            logger.info("Lua Info: "+lua.get("license"));
//            logger.info("Lua Info: "+lua.get("issueTrackerURL"));
//            logger.info("Lua Info: "+lua.get("version"));
//            logger.info("Lua Info: "+lua.get("displayName"));
//            logger.info("Lua Info: "+lua.get("updateJSONURL"));
//            logger.info("Lua Info: "+lua.get("displayURL"));
//            logger.info("Lua Info: "+lua.get("logoFile"));
//            logger.info("Lua Info: "+lua.get("credits"));
//            logger.info("Lua Info: "+lua.get("authors"));
//            logger.info("Lua Info: "+lua.get("description"));
        }
    }

    private static void LoadingLua(){
        MakeDir("modules");

        loadingluapath("libs");
        loadingluapath("autorun");

        loadingluapathinittbl("itemgroups", "ITEMGROUP");
        loadingluapathinittbl("items", "ITEM");
    }

    final class runString extends VarArgFunction {
        public Varargs invoke(Varargs args){
            Class<?> c = null;

//          ehh....

            String arg2 = !args.arg(2).isnil() ? args.arg(2).tojstring() : null;
            String arg3 = !args.arg(3).isnil() ? args.arg(3).tojstring() : null;
            String arg4 = !args.arg(4).isnil() ? args.arg(4).tojstring() : null;
            String arg5 = !args.arg(5).isnil() ? args.arg(5).tojstring() : null;
            String arg6 = !args.arg(6).isnil() ? args.arg(6).tojstring() : null;
            String arg7 = !args.arg(7).isnil() ? args.arg(7).tojstring() : null;
            String arg8 = !args.arg(8).isnil() ? args.arg(8).tojstring() : null;
            String arg9 = !args.arg(9).isnil() ? args.arg(9).tojstring() : null;
            String arg10 = !args.arg(10).isnil() ? args.arg(10).tojstring() : null;
            String arg11 = !args.arg(11).isnil() ? args.arg(11).tojstring() : null;
            String arg12 = !args.arg(12).isnil() ? args.arg(12).tojstring() : null;
            String arg13 = !args.arg(13).isnil() ? args.arg(13).tojstring() : null;
            String arg14 = !args.arg(14).isnil() ? args.arg(14).tojstring() : null;
            String arg15 = !args.arg(15).isnil() ? args.arg(15).tojstring() : null;
            String arg16 = !args.arg(16).isnil() ? args.arg(16).tojstring() : null;
            String arg17 = !args.arg(17).isnil() ? args.arg(17).tojstring() : null;
            String arg18 = !args.arg(18).isnil() ? args.arg(18).tojstring() : null;
            String arg19 = !args.arg(19).isnil() ? args.arg(19).tojstring() : null;
            String arg20 = !args.arg(20).isnil() ? args.arg(20).tojstring() : null;
            String arg21 = !args.arg(21).isnil() ? args.arg(21).tojstring() : null;
            String arg22 = !args.arg(22).isnil() ? args.arg(22).tojstring() : null;
            String arg23 = !args.arg(23).isnil() ? args.arg(23).tojstring() : null;
            String arg24 = !args.arg(24).isnil() ? args.arg(24).tojstring() : null;
            String arg25 = !args.arg(25).isnil() ? args.arg(25).tojstring() : null;
            String arg26 = !args.arg(26).isnil() ? args.arg(26).tojstring() : null;
            String arg27 = !args.arg(27).isnil() ? args.arg(27).tojstring() : null;
            String arg28 = !args.arg(28).isnil() ? args.arg(28).tojstring() : null;
            String arg29 = !args.arg(29).isnil() ? args.arg(29).tojstring() : null;
            String arg30 = !args.arg(30).isnil() ? args.arg(30).tojstring() : null;

            c = RunString(args.arg1().tojstring(),true,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10,arg11,arg12,arg13,arg14,arg15,arg16,arg17,arg18,arg19,arg20,arg21,arg22,arg23,arg24,arg25,arg26,arg27,arg28,arg29,arg30);

            return CoerceJavaToLua.coerce(c);
        }
    }

    public final static Class<?> RunString(String content,boolean islua,String...args){
        String className = "temp";
        String packageName = "com.nepqneko.lualoader";
        String prefix = String.format("package %s;",packageName);
        String fullName = String.format("%s.%s",packageName,className);

        JavaStringCompiler compiler = new JavaStringCompiler(islua);

        try {
            Map<String, byte[]> results = results = compiler.compile(className + ".java",prefix+content);
            Class<?> clazz = compiler.loadClass(fullName,results);
            Object instance = clazz.newInstance();

            try {
                Method mainMethod = clazz.getMethod("main", String[].class);

                mainMethod.invoke(instance,new Object[]{args});
            }
            catch (Exception e){}

            return clazz;
        } catch (Exception e){
            if (islua) LuaError(e);
            else logger.error(e);

            return null;
        }
    }

    public static boolean IsServer(){
        return !IsClient();
    }

    public static boolean IsClient(){
        return Thread.currentThread().getName().equals("Render thread") || Thread.currentThread().getThreadGroup() == SidedThreadGroups.CLIENT;
    }

    final class _Gi extends VarArgFunction {
        public Varargs invoke(Varargs args){
            if (args.arg(2).tojstring().equals("SERVER")){
                return LuaValue.valueOf(IsServer());
            }

            if (args.arg(2).tojstring().equals("CLIENT")){
                return LuaValue.valueOf(IsClient());
            }

            return NIL;
        }
    }

    final class New extends VarArgFunction {
        public Varargs invoke(Varargs args){
            LuaValue c = lua.get(args.checkvalue(2));
            Class clazz = (Class)c.touserdata(Class.class);

            if (clazz != null) return JavaClass.forClass(clazz).getConstructor();

            c = args.checkvalue(2);
            clazz = (Class)c.touserdata(Class.class);

            if (clazz != null) return JavaClass.forClass(clazz).getConstructor();

            try {
                clazz = Class.forName(c.tojstring(),true,ClassLoader.getSystemClassLoader());
            } catch (ClassNotFoundException e) {
                LuaError(e,"New");
            }

            if (clazz != null) return JavaClass.forClass(clazz).getConstructor();

            return NIL;
        }
    }

    final class New2 extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return NIL;
        }
    }

    final class Luainstanceof extends VarArgFunction {
        public Varargs invoke(Varargs args){
            Object arg1 = args.arg1().touserdata();
            Class arg2 = (Class)args.arg(2).touserdata(Class.class);

            return LuaValue.valueOf(arg2.isInstance(arg1));
        }
    }

    final class todouble extends VarArgFunction {
        public Varargs invoke(Varargs args){
            return LuaValue.valueOf(args.arg1().todouble());
        }
    }

    final class LuaConsumer extends VarArgFunction {
        public Varargs invoke(Varargs args){
            Consumer<Object> consumer = (o) -> {
                try {
                    args.arg1().call(CoerceJavaToLua.coerce(o));
                }
                catch (Exception err){
                    LuaError(err,"LuaConsumer");
                }
            };

            return CoerceJavaToLua.coerce(consumer);
        }
    }

    final class LuaSupplier extends VarArgFunction {
        public Varargs invoke(Varargs args){
            Supplier<Object> supplier = () -> {
                try {
                    return args.arg1().call().touserdata();
                }
                catch (Exception err){
                    LuaError(err,"LuaSupplier");

                    return null;
                }
            };

            return CoerceJavaToLua.coerce(supplier);
        }
    }

    final class LuaPredicate extends VarArgFunction {
        public Varargs invoke(Varargs args){
            Predicate<Object> predicate = (o) -> {
                try {
                    return args.arg1().call(CoerceJavaToLua.coerce(o)).toboolean();
                }
                catch (Exception err){
                    LuaError(err,"LuaPredicate");

                    return false;
                }
            };

            return CoerceJavaToLua.coerce(predicate);
        }
    }

    final class LuaFunction extends VarArgFunction {
        public Varargs invoke(Varargs args){
            Function<Object,Object> supplier = (o) -> {
                try {
                    return args.arg1().call(CoerceJavaToLua.coerce(o)).touserdata();
                }
                catch (Exception err){
                    LuaError(err,"LuaFunction");

                    return false;
                }
            };

            return CoerceJavaToLua.coerce(supplier);
        }
    }

    final class LuaRunnable extends VarArgFunction {
        public Varargs invoke(Varargs args){
            Runnable runnable = () -> {
                try {
                    args.arg1().call();
                }
                catch (Exception err){
                    LuaError(err,"LuaRunnable");
                }
            };

            return CoerceJavaToLua.coerce(runnable);
        }
    }

    final class LocalPlayer extends VarArgFunction {
        public Varargs invoke(Varargs args){
            if (IsServer()) return NIL;

            return CoerceJavaToLua.coerce(Minecraft.getInstance().player);
        }
    }

    private static void ReGetLuaTable(){
        try {
            lualoader.lua.get("Register").get("OnRegister").call();

            for (luaitem v:allluaitemlist){
                v.ReGetLuaTable();
            }

            for (luaitemgroup v:allluaitemgrouplist){
                v.ReGetLuaTable();
            }
        }
        catch (Exception e){
            LuaError(e,"ReloadLua");
        }
    }

    public static void ReloadLua(){
        LoadingLua();
        ReGetLuaTable();
    }

    public static void AutoReloadLua(){
        for (File f:reloadfiles.keySet()){
            long lastm = reloadfiles.get(f);

            if (f.lastModified() != lastm){
                loadluapath(f);
                reloadfiles.put(f,f.lastModified());
            }
        }

        for (File f:reloadfiles2.keySet()){
            Pair<String,Long> pair = reloadfiles2.get(f);
            String tabname = pair.getKey();
            long lastm = pair.getValue();

            if (f.lastModified() != lastm){
                loadluapathinittbl(f,tabname);
                reloadfiles2.put(f,new Pair(tabname,f.lastModified()));
            }
        }

        if (IsClient() && Minecraft.getInstance().player != null && PermissionAPI.hasPermission(Minecraft.getInstance().player,"OP")) {
            logger.info("Client Reload Lua Test");
        }
    }

    private static LuaDeferredRegister<Item> ITEMS = LuaDeferredRegister.create(ForgeRegistries.ITEMS,"ITEM", luaitem::new);

    public lualoader(){
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        initlua();

        if (FMLEnvironment.dist == Dist.CLIENT){
            Minecraft.getInstance().getResourcePackList().addPackFinder(luaresourcepackfinder.RESOUCE);
        }

        guib.add("net.minecraftforge.fml.client.gui.screen.ModListScreen");
        guib.add("net.minecraft.client.gui.screen.WorldLoadProgressScreen");
        guib.add("net.minecraft.client.gui.screen.WorkingScreen");
        guib.add("net.minecraft.client.gui.screen.DirtMessageScreen");

        AddCommand("clear",(String[] args) -> {
            lualogs.clear();
            LuaErrorToast.ClearToast = true;
        },"Clear lua console logs");

        AddCommand("lua_run",(String[] args) -> {
            if (args.length < 1) return;

            String luacode = "";

            for (String s:args){
                luacode += s+" ";
            }

            luacode = luacode.substring(1,luacode.length()-1);

            logger.info(luacode);

            try {
                lua.load(luacode);
            }
            catch (Exception e){
                LuaError(e);
            }
        },"Runing lua code");

        AddCommand("help",(String[] args) -> {
            LuaLogsAdd("All Lua Console Commands:");

            for (String com:commands.keySet()){
                String help = commands.get(com).getValue();

                LuaLogsAdd(!help.equals("") ? com+" - "+help : com);
            }
        },"View all commands");

        MinecraftForge.EVENT_BUS.register(this);

        new modevent();
        new forgeevent();

        ITEMS.register(modBus);
    }

    public static void hookcall(String s,Varargs args){
        try {
            lua.get("hook").get("Call").invoke(LuaValue.valueOf(s),args);
        }
        catch (Exception e2){
            LuaError(e2,"hookcall:"+s);
        }
    }

    public static void hookcall(String s,LuaValue arg1,LuaValue arg2,LuaValue arg3,LuaValue arg4,LuaValue arg5,LuaValue arg6,LuaValue arg7,LuaValue arg8){
        hookcall(s,LuaValue.varargsOf(arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8));
    }

    public static void hookcall(String s,LuaValue arg1,LuaValue arg2,LuaValue arg3,LuaValue arg4,LuaValue arg5,LuaValue arg6,LuaValue arg7){
        hookcall(s,LuaValue.varargsOf(arg1,arg2,arg3,arg4,arg5,arg6,arg7));
    }

    public static void hookcall(String s,LuaValue arg1,LuaValue arg2,LuaValue arg3,LuaValue arg4,LuaValue arg5,LuaValue arg6){
        hookcall(s,LuaValue.varargsOf(arg1,arg2,arg3,arg4,arg5,arg6));
    }

    public static void hookcall(String s,LuaValue arg1,LuaValue arg2,LuaValue arg3,LuaValue arg4,LuaValue arg5){
        hookcall(s,LuaValue.varargsOf(arg1,arg2,arg3,arg4,arg5));
    }

    public static void hookcall(String s,LuaValue arg1,LuaValue arg2,LuaValue arg3,LuaValue arg4){
        hookcall(s,LuaValue.varargsOf(arg1,arg2,arg3,arg4));
    }

    public static void hookcall(String s,LuaValue arg1,LuaValue arg2,LuaValue arg3){
        hookcall(s,LuaValue.varargsOf(arg1,arg2,arg3));
    }

    public static void hookcall(String s,LuaValue arg1,LuaValue arg2){
        hookcall(s,LuaValue.varargsOf(arg1,arg2));
    }

    public static void hookcall(String s){
        hookcall(s,NIL);
    }

    @SubscribeEvent
    public void addHookEvent13(FMLServerStartedEvent e){
        hookcall("OnServerStarted",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent14(FMLServerStartingEvent e){
        hookcall("OnServerStarting",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent15(FMLServerStoppedEvent e){
        hookcall("OnServerStopped",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent16(FMLServerStoppingEvent e){
        hookcall("OnServerStopping",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent17(ServerLifecycleEvent e){
        hookcall("OnServerLifecycle",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent18(PlaySoundEvent e){
        hookcall("OnPlaySound",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent19(PlaySoundSourceEvent e){
        hookcall("OnPlaySoundSource",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent20(PlayStreamingSourceEvent e){
        hookcall("OnPlayStreamingSource",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent21(SoundEvent e){
        hookcall("OnSoundEvent",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent22(SoundLoadEvent e){
        hookcall("OnSoundLoad",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent23(SoundSetupEvent e){
        hookcall("OnSoundSetup",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent24(ClientChatEvent e){
        hookcall("OnClientChat",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent25(ClientChatReceivedEvent e){
        hookcall("OnClientChatReceived",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent26(ClientPlayerChangeGameModeEvent e){
        hookcall("OnClientPlayerChangeGameMode",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent27(ClientPlayerNetworkEvent e){
        hookcall("OnClientPlayerNetworkEvent",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent28(ClientPlayerNetworkEvent.LoggedInEvent e){
        hookcall("OnClientPlayerNetworkLoggedIn",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent29(ClientPlayerNetworkEvent.LoggedOutEvent e){
        hookcall("OnClientPlayerNetworkLoggedOut",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent30(ClientPlayerNetworkEvent.RespawnEvent e){
        hookcall("OnClientPlayerNetworkEvent",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent31(ColorHandlerEvent e){
        hookcall("OnColorHandlerEvent",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent32(ColorHandlerEvent.Block e){
        hookcall("OnColorHandlerBlock",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent33(ColorHandlerEvent.Item e){
        hookcall("OnColorHandlerItem",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent34(DrawHighlightEvent e){
        hookcall("OnDrawHighlightEvent",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent35(DrawHighlightEvent.HighlightBlock e){
        hookcall("OnDrawHighlightBlock",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent36(DrawHighlightEvent.HighlightEntity e){
        hookcall("OnDrawHighlightEntity",CoerceJavaToLua.coerce(e));
    }

    // TODO: EntityViewRenderEvent

    @SubscribeEvent
    public void addHookEvent37(GuiScreenEvent e){
        hookcall("OnGuiEvent",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent38(GuiScreenEvent.InitGuiEvent e){
        hookcall("OnGuiInit",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent39(GuiScreenEvent.InitGuiEvent.Pre e){
        hookcall("OnPreGuiInit",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent40(GuiScreenEvent.InitGuiEvent.Post e){
        Screen gui = e.getGui();

        if (!guib.contains(gui.getClass().getName())){
            button = new Button(5,5,20,20, new StringTextComponent("Lua Console"),(b) -> {
                OpenConsole();
            }){
                @Override
                public void renderButton(MatrixStack matrixStack,int mouseX,int mouseY,float partialTicks){
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.getTextureManager().bindTexture(WIDGETS_LOCATION);
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
                    int i = this.getYImage(this.isHovered());
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.enableDepthTest();
                    this.blit(matrixStack, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
                    this.blit(matrixStack, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
                    this.renderBg(matrixStack,minecraft,mouseX,mouseY);
                    RenderSystem.color4f(1.0F,1.0F,1.0F,this.alpha);
                    minecraft.getTextureManager().bindTexture(LUA_ICON);
                    int offset = 4;
                    blit(matrixStack,this.x+offset/2,this.y+offset/2,0,0,this.width-offset,this.height-offset,this.width-offset,this.height-offset);
                }
            };
            e.addWidget(button);
        }

        hookcall("OnPostGuiInit",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent41(GuiScreenEvent.DrawScreenEvent e){
        hookcall("OnDrawScreen",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent42(GuiScreenEvent.DrawScreenEvent.Pre e){
        hookcall("OnPreDrawScreen",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent43(GuiScreenEvent.DrawScreenEvent.Post e){
        Screen gui = e.getGui();

        if (!guib.contains(gui.getClass().getName())){
            button.render(e.getMatrixStack(),e.getMouseX(),e.getMouseY(),e.getRenderPartialTicks());
        }

        hookcall("OnPostDrawScreen",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent44(GuiScreenEvent.BackgroundDrawnEvent e){
        hookcall("OnGuiBackgroundDrawn",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent45(GuiScreenEvent.PotionShiftEvent e){
        hookcall("OnGuiPotionShift",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent46(GuiScreenEvent.MouseInputEvent e){
        hookcall("OnGuiMouseInput",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent47(GuiScreenEvent.MouseClickedEvent e){
        hookcall("OnGuiMouseClicked",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent48(GuiScreenEvent.MouseClickedEvent.Pre e){
        hookcall("OnPreGuiMouseClicked",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent49(GuiScreenEvent.MouseClickedEvent.Post e){
        hookcall("OnPostGuiMouseClicked",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent50(GuiScreenEvent.MouseReleasedEvent e){
        hookcall("OnGuiMouseReleased",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent51(GuiScreenEvent.MouseReleasedEvent.Pre e){
        hookcall("OnPreGuiMouseReleased",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent52(GuiScreenEvent.MouseReleasedEvent.Post e){
        hookcall("OnPostGuiMouseReleased",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent53(GuiScreenEvent.MouseDragEvent e){
        hookcall("OnGuiMouseDrag",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent54(GuiScreenEvent.MouseDragEvent.Pre e){
        hookcall("OnGuiPreMouseDrag",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent55(GuiScreenEvent.MouseDragEvent.Post e){
        hookcall("OnGuiPostMouseDrag",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent56(GuiScreenEvent.MouseScrollEvent e){
        hookcall("OnGuiMouseScroll",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent57(GuiScreenEvent.MouseScrollEvent.Pre e){
        hookcall("OnGuiPreMouseScroll",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent58(GuiScreenEvent.MouseScrollEvent.Post e){
        hookcall("OnGuiPostMouseScroll",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent59(GuiScreenEvent.KeyboardKeyEvent e){
        hookcall("OnGuiKeyboardKey",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent60(GuiScreenEvent.KeyboardKeyPressedEvent e){
        hookcall("OnGuiKeyboardKeyPressed",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent61(GuiScreenEvent.KeyboardKeyPressedEvent.Pre e){
        hookcall("OnPreGuiKeyboardKeyPressed",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent62(GuiScreenEvent.KeyboardKeyPressedEvent.Post e){
        hookcall("OnPostGuiKeyboardKeyPressed",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent63(GuiScreenEvent.KeyboardKeyReleasedEvent e){
        hookcall("OnGuiKeyboardKeyReleased",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent64(GuiScreenEvent.KeyboardKeyReleasedEvent.Pre e){
        hookcall("OnPreGuiKeyboardKeyReleased",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent65(GuiScreenEvent.KeyboardKeyReleasedEvent.Post e){
        hookcall("OnPostGuiKeyboardKeyReleased",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent66(GuiScreenEvent.KeyboardCharTypedEvent e){
        hookcall("OnGuiKeyboardCharTyped",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent67(GuiScreenEvent.KeyboardCharTypedEvent.Pre e){
        hookcall("OnPreGuiKeyboardCharTyped",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent68(GuiScreenEvent.KeyboardCharTypedEvent.Post e){
        hookcall("OnPostGuiKeyboardCharTyped",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent69(RegisterCommandsEvent e){
        hookcall("OnCommandsRegister",CoerceJavaToLua.coerce(e));

        luacommand.register(e.getDispatcher());
    }

    @SubscribeEvent
    public void addHookEvent70(PlayerEvent e){
        hookcall("OnPlayerEvent",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent71(PlayerEvent.HarvestCheck e){
        hookcall("OnPlayerHarvestCheck",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent72(PlayerEvent.BreakSpeed e){
        hookcall("OnPlayerBreakSpeed",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent73(PlayerEvent.NameFormat e){
        hookcall("OnPlayerNameFormat",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent74(PlayerEvent.Clone e){
        hookcall("OnPlayerClone",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent75(PlayerEvent.StartTracking e){
        hookcall("OnPlayerStartTracking",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent76(PlayerEvent.StopTracking e){
        hookcall("OnPlayerStopTracking",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent77(PlayerEvent.LoadFromFile e){
        hookcall("OnPlayerLoadFromFile",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent78(PlayerEvent.SaveToFile e){
        hookcall("OnPlayerSaveToFile",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent79(PlayerEvent.Visibility e){
        hookcall("OnPlayerVisibility",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent80(PlayerEvent.ItemPickupEvent e){
        hookcall("OnPlayerItemPickup",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent81(PlayerEvent.ItemCraftedEvent e){
        hookcall("OnPlayerItemCrafted",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent82(PlayerEvent.ItemSmeltedEvent e){
        hookcall("OnPlayerItemSmelted",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent83(PlayerEvent.PlayerLoggedInEvent e){
        hookcall("OnPlayerLoggedIn",CoerceJavaToLua.coerce(e));

        errorinchat = true;

        for(int i = 0; i < msglist.size(); i++){
            e.getPlayer().sendMessage(new StringTextComponent(TextFormatting.getValueByName(IsClient() ? "yellow" : "blue").toString()+msglist.get(i)),e.getPlayer().getUniqueID());

            msglist.remove(i);
        }
    }

    @SubscribeEvent
    public void addHookEvent84(PlayerEvent.PlayerLoggedOutEvent e){
        hookcall("OnPlayerLoggedOut",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent85(PlayerEvent.PlayerRespawnEvent e){
        hookcall("OnPlayerRespawnEvent",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent86(PlayerEvent.PlayerChangedDimensionEvent e){
        hookcall("OnPlayerChangedDimension",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent87(PlayerEvent.PlayerChangeGameModeEvent e){
        hookcall("OnPlayerChangeGameMode",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent88(TickEvent e){
        modtime++;

        AutoReloadLua();
        hookcall("OnTick",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent89(TickEvent.ServerTickEvent e){
        hookcall("OnServerTick",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent90(TickEvent.ClientTickEvent e){
        hookcall("OnClientTick",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent91(TickEvent.WorldTickEvent e){
        world = e.world;

        hookcall("OnWorldTick",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent92(TickEvent.PlayerTickEvent e){
        hookcall("OnPlayerTick",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent93(TickEvent.RenderTickEvent e){
        hookcall("OnRenderTick",CoerceJavaToLua.coerce(e));
    }

    @SubscribeEvent
    public void addHookEvent94(InputEvent.KeyInputEvent e){
        if (e.getKey() == OPEN_CONSOLE.getKey().getKeyCode() && e.getAction() == GLFW.GLFW_PRESS){
            OpenConsole();
        }

        hookcall("OnKeyInput",CoerceJavaToLua.coerce(e));
    }
}
