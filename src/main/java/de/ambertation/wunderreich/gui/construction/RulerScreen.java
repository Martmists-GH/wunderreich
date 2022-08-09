package de.ambertation.wunderreich.gui.construction;

import de.ambertation.lib.ui.layout.components.*;
import de.ambertation.lib.ui.layout.components.render.RenderHelper;
import de.ambertation.lib.ui.layout.values.Rectangle;
import de.ambertation.lib.ui.layout.values.Size;
import de.ambertation.lib.ui.layout.values.Value;
import de.ambertation.wunderreich.Wunderreich;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class RulerScreen extends AbstractContainerScreen<RulerContainerMenu> {
    public static final ResourceLocation SDF_TEXTURE = Wunderreich.ID("textures/gui/sdf.png");
    public static final Size SDF_TEXTURE_SIZE = new Size(256, 256);
    public static final Rectangle SMALL_DIAMOND = new Rectangle(77, 30, 15, 15);
    public static final Rectangle LARGE_DIAMOND = new Rectangle(95, 31, 33, 33);
    public static final Rectangle INVENTORY_SLOT = new Rectangle(129, 0, 17, 17);
    private final RulerContainerMenu menu;

    public RulerScreen(RulerContainerMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.menu = menu;
        imageWidth = 427;
        imageHeight = 240;
    }


    @Override
    protected void init() {
        super.init();
        inventoryLabelX = 1000;
        titleLabelX = 1000;
        Panel inventoryPanel = createInventoryPanel();
        this.addRenderableWidget(inventoryPanel);

        Panel materialPanel = createMaterialPanel();
        this.addRenderableWidget(materialPanel);

        Panel sdfPanel = createSDFPanel();
        this.addRenderableWidget(sdfPanel);
    }

    Button selectedPageButton;

    private Panel createMaterialPanel() {
        Panel materialPanel = new Panel(RulerContainerMenu.MATERIAL_PANEL);

        Container materialContainer = new Container(Value.fit(), Value.fit());
        Image inventoryBackground = new Image(Value.fit(), Value.fit(), SDF_TEXTURE)
                .setResourceSize(SDF_TEXTURE_SIZE)
                .setUvRect(79, 197, 177, 59);
        materialContainer.addChild(inventoryBackground);

        VerticalStack materialStack = new VerticalStack(Value.fill(), Value.fit());
        materialStack.indent(6).addText(Value.fit(), Value.fit(), Component.literal("Materialgruppe"));
        materialStack.addSpacer(-9);
        materialStack.add(materialContainer);
        var buttonRow = materialStack.addRow().addSpacer(16);
        for (int i = 0; i < RulerContainer.MAX_CATEGORIES; i++) {
            int finalI = i;
            Button b = buttonRow.addButton(Value.fixed(18), Value.fit(), Component.literal("" + (i + 1)))
                                .onPress(bt -> {
                                    if (selectedPageButton != null) selectedPageButton.setAlpha(0.5f);
                                    menu.container.setActivePage(finalI);
                                    selectedPageButton = bt;
                                    if (selectedPageButton != null) selectedPageButton.setAlpha(1f);
                                })
                                .setAlpha(i == 0 ? 1 : 0.5f);
            if (i == 0) selectedPageButton = b;
        }

        materialPanel.setChild(materialStack);
        materialPanel.calculateLayout();

        return materialPanel;
    }

    private Panel createInventoryPanel() {
        Panel inventoryPanel = new Panel(RulerContainerMenu.INVENTORY_PANEL);

        Container inventoryContainer = new Container(Value.fit(), Value.fit());
        Image inventoryBackground = new Image(Value.fit(), Value.fit(), SDF_TEXTURE)
                .setResourceSize(SDF_TEXTURE_SIZE)
                .setUvRect(83, 110, 173, 87);
        inventoryContainer.addChild(inventoryBackground);
        VerticalStack inventoryStack = new VerticalStack(Value.fill(), Value.fit());
        inventoryStack.addFiller();
        inventoryStack.indent(5).addText(Value.fit(), Value.fit(), Component.literal("Inventory"));
        inventoryStack.add(inventoryContainer);

        inventoryPanel.setChild(inventoryStack);
        inventoryPanel.calculateLayout();

        return inventoryPanel;
    }

    private Panel createSDFPanel() {

        Panel inventoryPanel = new Panel(RulerContainerMenu.SDF_PANEL);

        Container inventoryContainer = new Container(Value.fit(), Value.fit());
        Image inputSlotA = new Image(Value.fit(), Value.fit(), SDF_TEXTURE)
                .setResourceSize(SDF_TEXTURE_SIZE)
                .setUvRect(SMALL_DIAMOND);
        inventoryContainer.addChild(0, 8, inputSlotA);

        Item stackTest = new Item(Value.fit(), Value.fit())
                .setItem(new ItemStack(Items.AMETHYST_SHARD, 1));
        inventoryContainer.addChild(0, 8, stackTest);

        Image inputSlotB = new Image(Value.fit(), Value.fit(), SDF_TEXTURE)
                .setResourceSize(SDF_TEXTURE_SIZE)
                .setUvRect(SMALL_DIAMOND);
        inventoryContainer.addChild(0, 32, inputSlotB);

        Image parentSlot = new Image(Value.fit(), Value.fit(), SDF_TEXTURE)
                .setResourceSize(SDF_TEXTURE_SIZE)
                .setUvRect(SMALL_DIAMOND);
        inventoryContainer.addChild(36, 20, parentSlot);

        Image SDFSlot = new Image(Value.fit(), Value.fit(), SDF_TEXTURE)
                .setResourceSize(SDF_TEXTURE_SIZE)
                .setUvRect(LARGE_DIAMOND);
        inventoryContainer.addChild(3, 11, SDFSlot);

        Image materialSlot = new Image(Value.fit(), Value.fit(), SDF_TEXTURE)
                .setResourceSize(SDF_TEXTURE_SIZE)
                .setUvRect(INVENTORY_SLOT);
        inventoryContainer.addChild(27, 0, materialSlot);


        inventoryPanel.setChild(inventoryContainer);
        inventoryPanel.calculateLayout();
        menu.sdfSlot.callOnChange(this::slotChanged);

        return inventoryPanel;
    }

    void slotChanged(SDFSlot slot) {

    }


    @Override
    protected void renderBg(PoseStack poseStack, float deltaTime, int mouseX, int mouseY) {
        GuiComponent.fill(poseStack, 0, 0, width, height, 0xE8242424);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float f) {
        super.render(poseStack, mouseX, mouseY, f);

        if (selectedPageButton != null) {
            RenderHelper.renderImage(
                    poseStack,
                    selectedPageButton.getScreenBounds().left,
                    selectedPageButton.getScreenBounds().top - 4,
                    SDF_TEXTURE,
                    SDF_TEXTURE_SIZE,
                    new Rectangle(126, 18, 17, 3),
                    1
            );
        }

        this.renderTooltip(poseStack, mouseX, mouseY);
    }
}
