package com.nepqneko.lualoader.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nepqneko.lualoader.lualoader;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.ToastGui;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;

public class LuaErrorToast implements IToast {
    ITextComponent luaerror;
    int color;
    public static boolean ClearToast;

    public LuaErrorToast(int color){
        this("",color);
    }

    public LuaErrorToast(String info,int color){
        String s = !info.equals("") ? "Lua Code Error ("+info+")" : "Lua Code Error";
        
        this.luaerror = new StringTextComponent(s);
        this.color = color;
        ClearToast = false;
    }

    public IToast.Visibility func_230444_a_(MatrixStack ms,ToastGui tg,long time){
        tg.getMinecraft().getTextureManager().bindTexture(TEXTURE_TOASTS);
        RenderSystem.color3f(1.0F, 1.0F, 1.0F);
        tg.blit(ms, 0, 0, 0, 64, this.func_230445_a_(), this.func_238540_d_());

        List<IReorderingProcessor> list = tg.getMinecraft().fontRenderer.trimStringToWidth(luaerror,125);

        if (list.size() == 1){
            tg.getMinecraft().fontRenderer.func_238422_b_(ms,list.get(0),20.0F,12F,color);
        }
        else {
            int l = this.func_238540_d_() / 2 - list.size()*9/2;

            for(IReorderingProcessor ireorderingprocessor : list){
                tg.getMinecraft().fontRenderer.func_238422_b_(ms,ireorderingprocessor,20.0F,(float)l,color);
                l += 9;
            }
        }

        if (ClearToast){
            return IToast.Visibility.HIDE;
        }

        return time >= 6000L ? IToast.Visibility.HIDE : IToast.Visibility.SHOW;
    }
}
