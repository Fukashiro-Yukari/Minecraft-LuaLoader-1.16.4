package com.nepqneko.lualoader.resource;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.resources.ResourcePack;
import net.minecraft.resources.ResourcePackFileNotFoundException;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LuaFilePack extends ResourcePack {
    public static final Splitter PATH_SPLITTER = Splitter.on('/').omitEmptyStrings().limit(4);
    private ZipFile zipFile;
    private String prefix;

    public LuaFilePack(File fileIn,String prefix){
        super(fileIn);

        this.prefix = prefix+"/";
    }

    private ZipFile getResourcePackZipFile() throws IOException {
        if (this.zipFile == null) {
            this.zipFile = new ZipFile(this.file);
        }

        return this.zipFile;
    }

    protected InputStream getInputStream(String resourcePath) throws IOException {
        ZipFile zipfile = this.getResourcePackZipFile();
        ZipEntry zipentry = zipfile.getEntry(prefix+resourcePath);

        if (zipentry == null) {
            throw new ResourcePackFileNotFoundException(this.file,prefix+resourcePath);
        } else {
            return zipfile.getInputStream(zipentry);
        }
    }

    public boolean resourceExists(String resourcePath) {
        try {
            return this.getResourcePackZipFile().getEntry(prefix+resourcePath) != null;
        } catch (IOException ioexception) {
            return false;
        }
    }

    public Set<String> getResourceNamespaces(ResourcePackType type) {
        ZipFile zipfile;
        try {
            zipfile = this.getResourcePackZipFile();
        } catch (IOException ioexception) {
            return Collections.emptySet();
        }

        Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
        Set<String> set = Sets.newHashSet();

        while(enumeration.hasMoreElements()) {
            ZipEntry zipentry = enumeration.nextElement();
            String s = zipentry.getName();

            if (s.startsWith(prefix+type.getDirectoryName()+"/")) {
                List<String> list = Lists.newArrayList(PATH_SPLITTER.split(s));
                if (list.size() > 2) {
                    String s1 = list.get(2);

                    if (s1.equals(s1.toLowerCase(Locale.ROOT))) {
                        set.add(s1);
                    } else {
                        this.onIgnoreNonLowercaseNamespace(s1);
                    }
                }
            }
        }

        return set;
    }

    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    public void close() {
        if (this.zipFile != null) {
            IOUtils.closeQuietly((Closeable)this.zipFile);
            this.zipFile = null;
        }
    }

    public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType type, String namespaceIn, String pathIn, int maxDepthIn, Predicate<String> filterIn) {
        ZipFile zipfile;
        try {
            zipfile = this.getResourcePackZipFile();
        } catch (IOException ioexception) {
            return Collections.emptySet();
        }

        Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
        List<ResourceLocation> list = Lists.newArrayList();
        String s = prefix+type.getDirectoryName()+"/"+namespaceIn+"/";
        String s1 = s + pathIn + "/";

        while(enumeration.hasMoreElements()) {
            ZipEntry zipentry = enumeration.nextElement();
            if (!zipentry.isDirectory()) {
                String s2 = zipentry.getName();

                if (!s2.endsWith(".mcmeta") && s2.startsWith(s1)) {
                    String s3 = s2.substring(s.length());
                    String[] astring = s3.split("/");

                    if (astring.length >= maxDepthIn + 1 && filterIn.test(astring[astring.length - 1])) {
                        list.add(new ResourceLocation(namespaceIn, s3));
                    }
                }
            }
        }

        return list;
    }
}