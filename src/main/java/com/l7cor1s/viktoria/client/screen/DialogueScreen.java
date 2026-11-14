package com.l7cor1s.viktoria.client.screen;

import com.l7cor1s.viktoria.dialogue.DialogueChoice;
import com.l7cor1s.viktoria.dialogue.DialogueNode;
import com.l7cor1s.viktoria.dialogue.DialogueTrees;
import com.l7cor1s.viktoria.network.DialogueChoicePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class DialogueScreen extends Screen {
    private final int entityId;
    private final String characterId;
    private String nodeId;
    private final int affinity;
    private final String mood;
    private final String tier;
    private DialogueNode node;
    private final List<Button> choiceButtons = new ArrayList<>();

    public DialogueScreen(int entityId, String characterId, String nodeId, int affinity, String mood, String tier) {
        super(Component.translatable("viktoria.gui.dialogue.title"));
        this.entityId = entityId;
        this.characterId = characterId;
        this.nodeId = nodeId;
        this.affinity = affinity;
        this.mood = mood;
        this.tier = tier;
        this.node = DialogueTrees.get(characterId, nodeId).orElse(null);
    }

    @Override
    protected void init() {
        rebuildChoices();
    }

    private void rebuildChoices() {
        clearWidgets();
        choiceButtons.clear();
        if (node == null) {
            return;
        }
        int panelWidth = Math.min(360, this.width - 40);
        int x = (this.width - panelWidth) / 2;
        int y = this.height - 28 - node.choices().size() * 24;
        for (DialogueChoice choice : node.choices()) {
            Button button = Button.builder(Component.translatable(choice.langKey()), b -> select(choice))
                    .bounds(x, y, panelWidth, 20)
                    .build();
            choiceButtons.add(button);
            addRenderableWidget(button);
            y += 24;
        }
    }

    private void select(DialogueChoice choice) {
        PacketDistributor.sendToServer(new DialogueChoicePayload(entityId, nodeId, choice.id()));
        if ("close".equals(choice.action()) || "intimate_pose".equals(choice.action())) {
            onClose();
            return;
        }
        // Optimistic local navigation; server will re-open authoritative node.
        if (choice.nextNode() != null && !"close".equals(choice.action())) {
            this.nodeId = choice.nextNode();
            this.node = DialogueTrees.get(characterId, nodeId).orElse(null);
            rebuildChoices();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = Math.min(360, this.width - 40);
        int panelLeft = (this.width - panelWidth) / 2;
        int panelTop = this.height / 2 - 90;
        int panelHeight = 100;

        graphics.fill(panelLeft - 6, panelTop - 28, panelLeft + panelWidth + 6, panelTop + panelHeight + 8, 0xCC1A1218);
        graphics.fill(panelLeft - 6, panelTop - 28, panelLeft + panelWidth + 6, panelTop - 26, 0xFFE8D5C4);

        graphics.drawString(this.font, Component.translatable("entity.viktoria.companion." + characterId),
                panelLeft, panelTop - 20, 0xF5E6D8, false);
        graphics.drawString(this.font, Component.translatable("viktoria.gui.affinity", affinity,
                        Component.translatable("viktoria.affinity." + tier.toLowerCase())),
                panelLeft, panelTop - 8, 0xB8A294, false);
        graphics.drawString(this.font, Component.translatable("viktoria.gui.mood",
                        Component.translatable("viktoria.mood." + mood)),
                panelLeft + panelWidth / 2, panelTop - 8, 0xB8A294, false);

        if (node != null) {
            List<FormattedCharSequence> lines = this.font.split(Component.translatable(node.textKey()), panelWidth - 8);
            int textY = panelTop + 10;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(this.font, line, panelLeft, textY, 0xFFF4EEE6, false);
                textY += 12;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
