package com.nepqneko.lualoader.mixin;

import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackList;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.packs.ModFileResourcePack;
import net.minecraftforge.fml.packs.ResourcePackLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.nepqneko.lualoader.lualoader;
import com.nepqneko.lualoader.resource.luaresourcepackfinder;

@Mixin(ResourcePackLoader.class)
public class mixinresourcelualoader {
    @Inject(method = "loadResourcePacks(Lnet/minecraft/resources/ResourcePackList;Ljava/util/function/BiFunction;)V", at = @At("RETURN"), remap = false)
    private static <T extends ResourcePackInfo> void injectPacks(ResourcePackList resourcePacks, BiFunction<Map<ModFile, ? extends ModFileResourcePack>, BiConsumer<? super ModFileResourcePack, T>, ResourcePackLoader.IPackInfoFinder> packFinder, CallbackInfo callback){
        resourcePacks.addPackFinder(luaresourcepackfinder.DATA);

        lualoader.logger.info("Injecting data pack finder.");
    }
}