package com.nepqneko.lualoader.resource;

import com.nepqneko.lualoader.lualoader;
import net.minecraft.resources.*;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.function.Consumer;

public final class luaresourcepackfinder implements IPackFinder {
    public static final luaresourcepackfinder DATA = new luaresourcepackfinder("Data Pack","data");
    public static final luaresourcepackfinder RESOUCE = new luaresourcepackfinder("Resource Pack","resources");

    private final String type;
    private final String path;
    private final File dir;
    private final File moddir;

    private luaresourcepackfinder(String type,String path){
        this.type = type;
        this.path = path;
        this.dir = new File("lua/"+path);
        this.moddir = new File("lua/mods/");

        lualoader.MakeDir(this.dir);
    }

    @Override
    public void findPacks(Consumer<ResourcePackInfo> packs,ResourcePackInfo.IFactory factory){
        int modn = 1;

        for (final File f:lualoader.GetFilesFromDir(moddir)){
            File f2 = new File("lua/mods/"+f.getName()+"/"+path);
            String ext = FilenameUtils.getExtension(f.getName());

            if (f.isFile() && ext.equals("zip")){
                final ResourcePackInfo packInfo = new ResourcePackInfo(f.getName()+" "+type,true,() -> new LuaFilePack(f,path),new StringTextComponent(f.getName()+" "+type),new StringTextComponent(f.getName()+" "+type),PackCompatibility.COMPATIBLE,ResourcePackInfo.Priority.TOP,false,IPackNameDecorator.PLAIN,true);

                modn++;
                packs.accept(packInfo);
            }
            else if (f2.isDirectory()){
                final ResourcePackInfo packInfo = new ResourcePackInfo(f.getName()+" "+type,true,() -> new FolderPack(f2),new StringTextComponent(f.getName()+" "+type),new StringTextComponent(f.getName()+" "+type),PackCompatibility.COMPATIBLE,ResourcePackInfo.Priority.TOP,false,IPackNameDecorator.PLAIN,true);

                modn++;
                packs.accept(packInfo);
            }
        }

        if (dir.isDirectory()){
            final ResourcePackInfo packInfo = new ResourcePackInfo(path == "data" ? "Lua Loader Data" : "Lua Loader Resources",true,() -> new FolderPack(dir),new StringTextComponent(path == "data" ? "Lua Loader Data" : "Lua Loader Resources"),new StringTextComponent("Resources for "+modn+" mod files"),PackCompatibility.COMPATIBLE,ResourcePackInfo.Priority.TOP,false,IPackNameDecorator.PLAIN, path == "data");

            packs.accept(packInfo);
        }
        else {
            lualoader.logger.warn("Failed to load {} from {}.",type,dir.getAbsolutePath());
        }
    }
}
