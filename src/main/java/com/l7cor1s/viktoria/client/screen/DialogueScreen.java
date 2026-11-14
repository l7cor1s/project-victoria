package com.l7cor1s.viktoria.client.screen;

import com.l7cor1s.viktoria.dialogue.DialogueChoice;
import com.l7cor1s.viktoria.dialogue.DialogueNode;
import com.l7cor1s.viktoria.dialogue.DialogueTrees;
import com.l7cor1s.viktoria.network.DialogueChoicePayload;
import com.l7cor1s.viktoria.relationship.MoodType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact right-side dialogue panel (MCA-style). Leaves the world undimmed
 * so the player can see the NPC while choosing replies.
 */
public class DialogueScreen extends Screen {
    private static final int PANEL_WIDTH = 196;
    private static final int PANEL_MARGIN = 10;
    private static final int PANEL_PAD = 8;
    private static final int CHOICE_H = 18;
    private static final int CHOICE_GAP = 3;

    private final int entityId;
    private final String characterId;
    private String nodeId;
    private final int affinity;
    private final String mood;
    private final String tier;
    private DialogueNode node;
    private final List<Button> choiceButtons = new ArrayList<>();
    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int textBlockBottom;

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
        layoutPanel();
        rebuildChoices();
    }

    private void layoutPanel() {
        panelLeft = this.width - PANEL_WIDTH - PANEL_MARGIN;
        int textLines = 3;
        if (node != null) {
            String textKey = node.resolveTextKey(MoodType.byName(mood));
            textLines = Math.max(2, this.font.split(Component.translatable(textKey), PANEL_WIDTH - PANEL_PAD * 2).size());
        }
        int choiceCount = node == null ? 0 : node.visibleChoices(affinity, MoodType.byName(mood)).size();
        int headerH = 28;
        int textH = textLines * 11 + 6;
        int choicesH = choiceCount * (CHOICE_H + CHOICE_GAP) + (choiceCount > 0 ? 4 : 0);
        panelHeight = headerH + textH + choicesH + PANEL_PAD;
        panelTop = Math.max(PANEL_MARGIN, (this.height - panelHeight) / 2);
        // Keep panel from going off-screen on short windows
        if (panelTop + panelHeight > this.height - PANEL_MARGIN) {
            panelTop = Math.max(4, this.height - PANEL_MARGIN - panelHeight);
        }
        textBlockBottom = panelTop + headerH + textH;
    }

    private void rebuildChoices() {
        clearWidgets();
        choiceButtons.clear();
        layoutPanel();
        if (node == null) {
            return;
        }
        List<DialogueChoice> visible = node.visibleChoices(affinity, MoodType.byName(mood));
        int btnW = PANEL_WIDTH - PANEL_PAD * 2;
        int x = panelLeft + PANEL_PAD;
        int y = textBlockBottom + 2;
        for (DialogueChoice choice : visible) {
            Button button = Button.builder(Component.translatable(choice.langKey()), b -> select(choice))
                    .bounds(x, y, btnW, CHOICE_H)
                    .build();
            choiceButtons.add(button);
            addRenderableWidget(button);
            y += CHOICE_H + CHOICE_GAP;
        }
    }

    private void select(DialogueChoice choice) {
        PacketDistributor.sendToServer(new DialogueChoicePayload(entityId, nodeId, choice.id()));
        if ("close".equals(choice.action()) || "intimate_pose".equals(choice.action())
                || "intimate_close".equals(choice.action()) || "intimate_embrace".equals(choice.action())) {
            onClose();
            return;
        }
        if (choice.nextNode() != null && !"close".equals(choice.action())) {
            this.nodeId = choice.nextNode();
            this.node = DialogueTrees.get(characterId, nodeId).orElse(null);
            rebuildChoices();
        }
    }

    /** No full-screen dim — world and NPC stay fully visible. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // intentionally empty
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        layoutPanel();
        int left = panelLeft;
        int top = panelTop;
        int right = left + PANEL_WIDTH;
        int bottom = top + panelHeight;

        // Soft panel only — no world dim
        graphics.fill(left, top, right, bottom, 0xE816141A);
        graphics.fill(left, top, left + 2, bottom, 0xFFE8D5C4);
        graphics.fill(left, top, right, top + 1, 0x55E8D5C4);
        graphics.fill(left, bottom - 1, right, bottom, 0x33101014);

        int tx = left + PANEL_PAD;
        graphics.drawString(this.font, Component.translatable("entity.viktoria.companion." + characterId),
                tx, top + 6, 0xF5E6D8, false);
        graphics.drawString(this.font, Component.translatable("viktoria.gui.affinity", affinity,
                        Component.translatable("viktoria.affinity." + tier.toLowerCase())),
                tx, top + 17, 0x9A8A7C, false);

        if (node != null) {
            String textKey = node.resolveTextKey(MoodType.byName(mood));
            List<FormattedCharSequence> lines = this.font.split(Component.translatable(textKey), PANEL_WIDTH - PANEL_PAD * 2);
            int textY = top + 30;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(this.font, line, tx, textY, 0xFFF0E8DE, false);
                textY += 11;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
