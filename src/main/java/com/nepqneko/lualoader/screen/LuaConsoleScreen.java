package com.nepqneko.lualoader.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nepqneko.lualoader.lualoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.gui.ScrollPanel;

import java.util.ArrayList;
import java.util.List;

public class LuaConsoleScreen extends Screen {
    private ScrollPanel lcsp;
    private TextFieldWidget text;

    public LuaConsoleScreen(ITextComponent titleIn){
        super(titleIn);
    }

    public void closeScreen() {
        assert this.minecraft != null;
        this.minecraft.displayGuiScreen(lualoader.oldscreen);
    }

    @Override
    protected void init(){
        assert minecraft != null;
        minecraft.keyboardListener.enableRepeatEvents(true);

        int offset = 60;
        int offsety = 20;

        lcsp = new LuaConsolePanel(this.minecraft,this.width-offset,this.height-offset-offsety,offset/2+offsety/2,offset/2,font);
        text = new LuaConsoleTextField(font,offset/2,this.height-offsety*2,this.width-offset,14,getTitle());

        children.add(lcsp);
        children.add(text);
        text.setMaxStringLength(32500);

        super.init();
    }

    @Override
    public void tick(){
        text.tick();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float particleTick) {
        this.renderBackground(matrixStack);

        drawCenteredString(matrixStack,this.font,this.title,this.width/2,16,16777215);

        lcsp.render(matrixStack,mouseX,mouseY,particleTick);
        text.render(matrixStack,mouseX,mouseY,particleTick);

        super.render(matrixStack,mouseX,mouseY,particleTick);
    }

    @Override
    public void renderBackground(MatrixStack matrixStack, int vOffset){
        assert this.minecraft != null;
        if (this.minecraft.world != null){
            this.fillGradient(matrixStack,0,0,this.width,this.height,-1072689136,-804253680);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent(this,matrixStack));
        }
        else {
            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.defaultBlendFunc();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);

            int x1 = 0;
            int y1 = 0;
            int x2 = this.width;
            int y2 = this.height;

            bufferbuilder.pos(matrixStack.getLast().getMatrix(),(float)x2,(float)y1,(float)0).color(0,60,255,255).endVertex();
            bufferbuilder.pos(matrixStack.getLast().getMatrix(),(float)x1,(float)y1,(float)0).color(0,60,255,255).endVertex();
            bufferbuilder.pos(matrixStack.getLast().getMatrix(),(float)x1,(float)y2,(float)0).color(0,60,255,255).endVertex();
            bufferbuilder.pos(matrixStack.getLast().getMatrix(),(float)x2,(float)y2,(float)0).color(0,60,255,255).endVertex();
            tessellator.draw();
            RenderSystem.disableBlend();
            RenderSystem.enableAlphaTest();
            RenderSystem.enableTexture();
        }
    }

    @Override
    public void resize(Minecraft mc, int width, int height){
        String s = text.getText();

        super.resize(mc,width,height);

        text.setText(s);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        if (text.isFocused()){
            text.keyPressed(keyCode,scanCode,modifiers);
        }

        return super.keyPressed(keyCode,scanCode,modifiers);
    }
}
