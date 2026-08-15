package infiniteinvo.integration.client;

import infiniteinvo.client.ScrollableInventoryScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.neoforged.fml.ModList;

/** Adds optional player-window integrations using their native anchors. */
public final class InventoryEntryCompat {
    private final VanillaInventoryScreenCompat vanillaScreen;
    private final List<AbstractWidget> entryButtons = new ArrayList<>();
    private final ApothicAttributesClientCompat.AttributesPanel attributesPanel;

    private InventoryEntryCompat(VanillaInventoryScreenCompat vanillaScreen,
                                 ApothicAttributesClientCompat.AttributesPanel attributesPanel) {
        this.vanillaScreen = vanillaScreen;
        this.attributesPanel = attributesPanel;
    }

    public static InventoryEntryCompat create(ScrollableInventoryScreen screen) {
        VanillaInventoryScreenCompat vanillaScreen = new VanillaInventoryScreenCompat(
                Minecraft.getInstance().player, screen);
        ApothicAttributesClientCompat.AttributesPanel attributesPanel = null;
        if (ModList.get().isLoaded("apothic_attributes")) {
            attributesPanel = ApothicAttributesClientCompat.createPanel(screen, vanillaScreen);
        }
        InventoryEntryCompat entries = new InventoryEntryCompat(vanillaScreen, attributesPanel);
        if (ModList.get().isLoaded("curios") && CuriosClientCompat.isButtonEnabled()) {
            entries.entryButtons.add(CuriosClientCompat.createButton(screen));
        }
        if (ModList.get().isLoaded("accessories")) {
            entries.entryButtons.add(AccessoriesClientCompat.createButton(screen));
        }
        if (ModList.get().isLoaded("aether") && AetherClientCompat.isButtonEnabled()) {
            AbstractWidget button = AetherClientCompat.createButton(vanillaScreen);
            if (button != null) {
                entries.entryButtons.add(button);
            }
        }
        if (attributesPanel != null) {
            entries.entryButtons.add(attributesPanel.toggleButton());
        }
        return entries;
    }

    public void addWidgets(ScrollableInventoryScreen screen) {
        for (AbstractWidget button : entryButtons) {
            screen.addCompatibilityWidget(button);
        }
        if (attributesPanel != null) {
            screen.addCompatibilityWidget(attributesPanel.hideUnchangedButton());
        }
        updatePositions(screen);
    }

    public void updatePositions(ScrollableInventoryScreen screen) {
        for (AbstractWidget button : entryButtons) {
            if (button instanceof AccessoriesClientCompat.AccessoryEntryButton accessoriesButton) {
                accessoriesButton.updatePosition(screen);
            } else if (button instanceof CuriosClientCompat.CuriosEntryButton curiosButton) {
                curiosButton.updatePosition(screen);
            }
        }
        if (attributesPanel != null) {
            attributesPanel.updatePosition();
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (attributesPanel != null) {
            attributesPanel.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return attributesPanel != null && attributesPanel.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return attributesPanel != null && attributesPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return attributesPanel != null && attributesPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
