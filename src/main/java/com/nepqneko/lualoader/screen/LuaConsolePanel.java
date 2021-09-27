package com.nepqneko.lualoader.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nepqneko.lualoader.lualoader;
import javafx.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.Style;
import net.minecraftforge.client.gui.ScrollPanel;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.client.gui.screen.ModListScreen;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class LuaConsolePanel extends ScrollPanel {
    private final Minecraft client;
    private final int barWidth = 6;
    private final int barLeft;
    protected FontRenderer font;
    private List<Pair<IReorderingProcessor,Integer>> lines = Collections.emptyList();
    private int logsoldsize = -1;
    private boolean scrolling;
    protected float scrollDistance;
    private boolean scrolldontdown;

    public LuaConsolePanel(Minecraft client, int width, int height, int top, int left, FontRenderer font){
        super(client,width,height,top,left);
        this.client = client;
        this.barLeft = this.left + this.width - barWidth;
        this.font = font;
    }

    private List<Pair<IReorderingProcessor,Integer>> resizeContent(List<Pair<String,Integer>> lines){
        List<Pair<IReorderingProcessor,Integer>> ret = new ArrayList<>();
        for (Pair<String,Integer> pair:lines){
            String line = pair.getKey();
            int color = pair.getValue();

            if (line == null){
                ret.add(null);
                continue;
            }

            ITextComponent chat = ForgeHooks.newChatWithLinks(line,false);
            int maxTextLength = this.width - 12;
            if (maxTextLength >= 0){
                for (IReorderingProcessor ir:LanguageMap.getInstance().func_244260_a(font.getCharacterManager().func_238362_b_(chat, maxTextLength, Style.EMPTY))){
                    ret.add(new Pair<>(ir,color));
                }
            }
        }
        return ret;
    }

    @Override
    public int getContentHeight(){
        int height = 50;
        height += (lines.size() * font.FONT_HEIGHT);
        if (height < this.bottom - this.top - 8)
            height = this.bottom - this.top - 8;
        return height;
    }

    @Override
    protected void drawPanel(MatrixStack mStack, int entryRight, int relativeY, Tessellator tess, int mouseX, int mouseY){
        if (logsoldsize != lualoader.lualogs.size()){
            logsoldsize = lualoader.lualogs.size();
            lines = resizeContent(lualoader.lualogs);

            if (!scrolldontdown){
                setScroll();
            }
        }

        if (scrolldontdown){
            int max = getMaxScroll();

            if (max < 0){
                max /= 2;
            }

            if (this.scrollDistance >= max){
                scrolldontdown = false;
            }
        }

        for (Pair<IReorderingProcessor,Integer> pair:lines){
            IReorderingProcessor line = pair.getKey();
            int color = pair.getValue();

            if (line != null){
                RenderSystem.enableBlend();
                this.font.func_238407_a_(mStack, line, left+6, relativeY,color);
                RenderSystem.disableAlphaTest();
                RenderSystem.disableBlend();
            }
            relativeY += font.FONT_HEIGHT;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks)
    {
        this.drawBackground();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder worldr = tess.getBuffer();

        double scale = client.getMainWindow().getGuiScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int)(left  * scale), (int)(client.getMainWindow().getFramebufferHeight() - (bottom * scale)),
                (int)(width * scale), (int)(height * scale));

        this.drawGradientRect(matrix, this.left, this.top, this.right, this.bottom, 0xC0101010, 0xD0101010);

        int baseY = this.top + border - (int)this.scrollDistance;
        this.drawPanel(matrix, right, baseY, tess, mouseX, mouseY);

        RenderSystem.disableDepthTest();

        int extraHeight = (this.getContentHeight() + border) - height;
        if (extraHeight > 0)
        {
            int barHeight = getBarHeight();

            int barTop = (int)this.scrollDistance * (height - barHeight) / extraHeight + this.top;
            if (barTop < this.top)
            {
                barTop = this.top;
            }

            RenderSystem.disableTexture();
            worldr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldr.pos(barLeft,            this.bottom, 0.0D).tex(0.0F, 1.0F).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            worldr.pos(barLeft + barWidth, this.bottom, 0.0D).tex(1.0F, 1.0F).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            worldr.pos(barLeft + barWidth, this.top,    0.0D).tex(1.0F, 0.0F).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            worldr.pos(barLeft,            this.top,    0.0D).tex(0.0F, 0.0F).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            tess.draw();
            worldr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldr.pos(barLeft,            barTop + barHeight, 0.0D).tex(0.0F, 1.0F).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            worldr.pos(barLeft + barWidth, barTop + barHeight, 0.0D).tex(1.0F, 1.0F).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            worldr.pos(barLeft + barWidth, barTop,             0.0D).tex(1.0F, 0.0F).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            worldr.pos(barLeft,            barTop,             0.0D).tex(0.0F, 0.0F).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            tess.draw();
            worldr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldr.pos(barLeft,                barTop + barHeight - 1, 0.0D).tex(0.0F, 1.0F).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            worldr.pos(barLeft + barWidth - 1, barTop + barHeight - 1, 0.0D).tex(1.0F, 1.0F).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            worldr.pos(barLeft + barWidth - 1, barTop,                 0.0D).tex(1.0F, 0.0F).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            worldr.pos(barLeft,                barTop,                 0.0D).tex(0.0F, 0.0F).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            tess.draw();
        }

        RenderSystem.enableTexture();
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private int getBarHeight(){
        int barHeight = (height * height) / this.getContentHeight();

        if (barHeight < 32) barHeight = 32;

        if (barHeight > height - border*2)
            barHeight = height - border*2;

        return barHeight;
    }

    private void applyScrollLimits(){
        int max = getMaxScroll();

        if (max < 0){
            max /= 2;
        }

        if (this.scrollDistance < 0.0F)
        {
            this.scrollDistance = 0.0F;
        }

        if (this.scrollDistance > max)
        {
            this.scrollDistance = max;
        }
    }

    private void setScroll(){
        int max = getMaxScroll();

        if (max < 0){
            max /= 2;
        }

        this.scrollDistance = max;
        applyScrollLimits();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll){
        if (scroll != 0)
        {
            this.scrollDistance += -scroll * getScrollAmount();
            applyScrollLimits();
            scrolldontdown = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if (super.mouseClicked(mouseX, mouseY, button))
            return true;

        this.scrolling = button == 0 && mouseX >= barLeft && mouseX < barLeft + barWidth;
        if (this.scrolling){
            scrolldontdown = true;

            return true;
        }
        int mouseListY = ((int)mouseY) - this.top - this.getContentHeight() + (int)this.scrollDistance - border;
        if (mouseX >= left && mouseX <= right && mouseListY < 0){
            scrolldontdown = true;

            return this.clickPanel(mouseX - left, mouseY - this.top + (int)this.scrollDistance - border, button);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double p_mouseReleased_1_, double p_mouseReleased_3_, int p_mouseReleased_5_){
        if (super.mouseReleased(p_mouseReleased_1_, p_mouseReleased_3_, p_mouseReleased_5_))
            return true;
        boolean ret = this.scrolling;
        this.scrolling = false;
        return ret;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY){
        if (this.scrolling)
        {
            int maxScroll = height - getBarHeight();
            double moved = deltaY / maxScroll;
            this.scrollDistance += getMaxScroll() * moved;
            applyScrollLimits();
            return true;
        }
        return false;
    }

    private int getMaxScroll()
    {
        return this.getContentHeight() - (this.height - this.border);
    }
}
