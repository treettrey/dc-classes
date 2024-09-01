package com.afunproject.dawncraft.classes.client;

import com.afunproject.dawncraft.classes.ClassesLogger;
import com.afunproject.dawncraft.classes.Constants;
import com.afunproject.dawncraft.classes.data.AttributeEntry;
import com.afunproject.dawncraft.classes.data.DCClass;
import com.afunproject.dawncraft.classes.data.ItemEntry;
import com.afunproject.dawncraft.classes.integration.BaublesIntegration;
import com.afunproject.dawncraft.classes.integration.epicfight.EpicFightIntegration;
import com.afunproject.dawncraft.classes.integration.epicfight.client.EpicFightPlayerRenderer;
import com.afunproject.dawncraft.classes.integration.epicfight.client.SkillSlot;
import com.afunproject.dawncraft.classes.network.NetworkHandler;
import com.afunproject.dawncraft.classes.network.PickClassMessage;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentBase;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Loader;

import java.awt.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClassSelectionScreen extends GuiScreen {
    
    public static final ResourceLocation TEXTURE = Constants.loc("textures/gui/class_selection.png");
    
    private static final int TEXT_WIDTH = 40;
    protected int guiWidth = 168;
    protected int guiHeight = 180;
    private int page = 0;
    private final List<DCClass> classes;
    private final EntityOtherPlayerMP player;
    private final EpicFightPlayerRenderer playerRenderer;
    private final List<GuiButton> buttons = Lists.newArrayList();
    protected int leftPos;
    protected int topPos;
    protected final List<TextComponentBase> description = Lists.newArrayList();
    private final List<ClassSlot> slots = Lists.newArrayList();
    private int itemX, itemWidth, itemHeight, skillX, skillWidth, skillHeight;

    public ClassSelectionScreen(List<DCClass> cache) {
        super();
        Minecraft minecraft = Minecraft.getMinecraft();
        player = new EntityOtherPlayerMP(minecraft.world, minecraft.player.getGameProfile());
        playerRenderer = Loader.isModLoaded("epicfight") ? new EpicFightPlayerRenderer(player) : null;
        if (cache.isEmpty()) {
            ClassesLogger.logError("no enabled classes ", new Exception());
            classes = null;
            return;
        }
        classes = cache.stream().sorted(Comparator.comparingInt(DCClass::getIndex)).collect(Collectors.toList());
        reloadEquipment();
        reloadText();
    }

    @Override
    public void init() {
        buttons.clear();
        leftPos = (width - guiWidth) / 2;
        topPos = (height - guiHeight) / 2;
        buttons.add(new ClassSwitchButton(this, leftPos + 4, topPos - 10, true));
        buttons.add(new ClassSwitchButton(this, leftPos + guiWidth - 16, topPos - 10, false));
        buttons.add(new ConfirmButtom(this, leftPos + guiWidth / 2 - 30, topPos + guiHeight));
        reloadSlots();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiUtils.drawContinuousTexturedBox(TEXTURE, leftPos + 10, topPos + 10, 0, 0, 148, 106, 32, 42, 19, 9, 9,9, 1);
        for(GuiButton widget : buttons) widget.drawButton(mc, mouseX, mouseY, partialTicks);
        DCClass clazz = getSelectedClass();
        if (clazz == null) return;
        //title
        drawBox(leftPos + 18, topPos - 10, 131, 17);
        drawCenteredString(mc.fontRenderer, new TextComponentTranslation(clazz.getTranslationKey()).getFormattedText(), leftPos + guiWidth /2, topPos -6, 0x9E0CD2);
        //description
        int offset = (int)((float)(description.size() * 9)/2f);
        drawBox(leftPos - 6, topPos + guiHeight / 2 + 48 - offset, 179, description.size() * 9 + 8);
        for (int i = 0; i < description.size(); i ++) {
            Component component = description.get(i);
            drawCenteredString(poseStack, minecraft.font,  component, leftPos + guiWidth / 2, topPos + guiHeight / 2 + 52 + i * 9 - offset, 0xFFFFFF);
        }
        //player
        int entityX = leftPos + guiWidth / 2;
        int entityY = topPos + guiHeight / 2 + 13;
        if (playerRenderer != null) playerRenderer.render(entityX + clazz.getXOffset(), entityY + clazz.getYOffset(), partialTicks, clazz.getAnimation());
        else InventoryScreen.renderEntityInInventory(entityX, entityY, 38, entityX - mouseX, entityY + (player.getEyeHeight()) - mouseY, player);
        //items, skills and attributes
        if (itemHeight > 0) {
            drawBox(itemX, topPos + 17, itemWidth, itemHeight);
            drawCenteredString(mc.fontRenderer, new TextComponentTranslation("text.dcclasses.items").getFormattedText(), leftPos - 12, topPos + 21, 0xFFFFFF);
        }
        if (skillHeight > 0) {
            drawBox(skillX, topPos + 17, skillWidth, skillHeight);
            drawCenteredString(mc.fontRenderer, new TextComponentTranslation("text.dcclasses.skills"), leftPos + guiWidth + 12, topPos + 21, 0xFFFFFF);
        }
        ClassSlot hoveredSlot = null;
        for (ClassSlot slot : slots) {
            slot.render(mouseX, mouseY, partialTicks);
            if (hoveredSlot == null && slot.isMouseOver(mouseX, mouseY)) hoveredSlot = slot;
        }
        if (hoveredSlot != null) renderTooltip(poseStack, hoveredSlot.getTooltip(), Optional.empty(), mouseX, mouseY);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int p_95587_) {
        for (AbstractButton button : buttons) if (button.isMouseOver(mouseX, mouseY)) return button.mouseClicked(mouseX, mouseY, p_95587_);
        return false;
    }
    
    @Override
    public void onClose() {
        if (classes != null) classes.clear();
        super.onClose();
    }

    public void switchPage(int page) {
        this.page = Math.floorMod(page + this.page, classes.size());
        reloadEquipment();
        reloadText();
        reloadSlots();
    }
    
    public void confirm() {
        NetworkHandler.NETWORK_INSTANCE.sendTo(new PickClassMessage(getSelectedClass().getRegistryName()),
                minecraft.player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
        onClose();
    }
    
    public DCClass getSelectedClass() {
        if (classes == null) {
            onClose();
            return null;
        }
        if (classes.size() == 0) return null;
        return classes.get(page);
    }
    
    private void reloadEquipment() {
        DCClass clazz = getSelectedClass();
        if (clazz == null) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
        player.getInventory().clearContent();
        if (ModList.get().isLoaded("curios")) BaublesIntegration.clear(player);
        clazz.setVisualEquipment(player);
    }
    
    private void reloadText() {
        description.clear();
        DCClass clazz = getSelectedClass();
        if (clazz == null) return;
        String str = new TextComponentTranslation(clazz.getTranslationKey() + ".desc").getString();
        int position = 0;
        while (position < str.length()) {
            if (description.size() >= 7) break;
            int size = Math.min(TEXT_WIDTH, str.length() - position);
            while (Minecraft.getInstance().font.width(str.substring(position, position + size)) > 169) size--;
            int newPos = position + size;
            if (str.substring(position, newPos).contains("\n")) {
                int i = str.substring(position, newPos).indexOf("\n");
                description.add(new TextComponent(str.substring(position, position + i)));
                position = position + i + 1;
                continue;
            }
            if (newPos >= str.length()) {
                description.add(new TextComponent(str.substring(position)));
                break;
            }
            for (int i = 0; i <= size; i++) {
                if (i == size) {
                    description.add(new TextComponent(str.substring(position, newPos + 1)));
                    position = newPos;
                    break;
                } else if (str.charAt(newPos - i) == ' ') {
                    description.add(new TextComponent(str.substring(position, newPos - i + 1)));
                    position = newPos - i + 1;
                    break;
                }
            }
        }
    }
    
    private void reloadSlots() {
        slots.clear();
        DCClass clazz = getSelectedClass();
        if (clazz == null) return;
        List <AttributeEntry> attributes = clazz.getAttributes();
        for (int i = 0; i < attributes.size(); i++) {
            AttributeEntry attribute = attributes.get(i);
            int width = mc.fontRenderer.getStringWidth(attribute.getText().getFormattedText()) + 11;
            slots.add(new AttributeSlot(attribute, width, leftPos + 11 + (int)((((float)guiWidth - 22f) * (float) (i + 1)) / (attributes.size() + 1f)) - (int)((float)width * 0.5f), topPos + 13));
        }
        List<ItemEntry> items = clazz.getItems();
        int itemRows = (int)(((float)items.size() -1) / 3f) + 1;
        itemWidth = Math.max(itemRows * 18, mc.fontRenderer.getStringWidth(new TextComponentTranslation("text.dcclasses.items").getFormattedText())) + 8;
        itemHeight = items.isEmpty() ? 0 : 20 + (int)Math.ceil((float)items.size()/(float)itemRows) * 18;
        itemX = leftPos - 12 - (int)((float)itemWidth / 2f);
        for (int i = 0; i < items.size(); i++) slots.add(new ItemSlot(items.get(i),leftPos - 28 + itemRows * 8 - i % itemRows * 18, topPos + 32 + (i / itemRows) * 18));
        if (Loader.isModLoaded("epicfight")) return;
        List<String> skills = EpicFightIntegration.getVerifiedSkills(clazz);
        int skillRows = (int)((float)(skills.size() -1) / 3f) + 1;
        skillWidth = Math.max(skillRows * 18, mc.fontRenderer.getStringWidth(new TextComponentTranslation("text.dcclasses.skills").getFormattedText())) + 8;
        skillHeight = skills.isEmpty() ? 0 : 20 + (int)Math.ceil((float)skills.size()/(float)skillRows) * 18;
        skillX = leftPos + guiWidth + 12 - (int)((float)skillWidth / 2f);
        for (int i = 0; i < skills.size(); i++) slots.add(new SkillSlot(skills.get(i), leftPos + guiWidth + 12 - skillRows * 8 + i % skillRows * 18, topPos + 32 + (i / skillRows) * 18));
    }
    
    private void drawBox(int x, int y, int width, int height) {
        GuiUtils.drawContinuousTexturedBox(TEXTURE, x, y, 0, 42, width, height, 32, 32, 4, 4, 4, 4, 1);
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) {}

}
