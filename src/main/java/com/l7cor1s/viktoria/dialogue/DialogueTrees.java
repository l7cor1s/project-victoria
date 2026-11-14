package com.l7cor1s.viktoria.dialogue;

import com.l7cor1s.viktoria.character.ViktoriaCharacter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Built-in Russian dialogue trees. Additional characters register their own maps.
 * JSON loaders can replace/extend this later without changing the GUI.
 */
public final class DialogueTrees {
    private static final Map<String, Map<String, DialogueNode>> TREES = new HashMap<>();

    static {
        Map<String, DialogueNode> vika = new HashMap<>();
        vika.put("greeting", new DialogueNode("greeting", "viktoria.dialogue.greeting", List.of(
                new DialogueChoice("chat", "viktoria.choice.chat", "smalltalk", 2, "affection", null),
                new DialogueChoice("gift_hint", "viktoria.choice.gift_hint", "gift_hint", 0, null, null),
                new DialogueChoice("follow", "viktoria.choice.follow", "follow_confirm", 1, "joy", "toggle_follow"),
                new DialogueChoice("status", "viktoria.choice.status", "status", 0, null, "show_status"),
                new DialogueChoice("home", "viktoria.choice.home", "home", 0, null, null),
                new DialogueChoice("quest", "viktoria.choice.quest", "quest", 0, null, null),
                new DialogueChoice("intimate", "viktoria.choice.intimate", "intimate_gate", 0, null, null),
                new DialogueChoice("bye", "viktoria.choice.bye", "bye", 0, null, "close")
        )));
        vika.put("smalltalk", new DialogueNode("smalltalk", "viktoria.dialogue.smalltalk", List.of(
                new DialogueChoice("more", "viktoria.choice.more", "smalltalk2", 3, "affection", null),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("smalltalk2", new DialogueNode("smalltalk2", "viktoria.dialogue.smalltalk2", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 1, "joy", null)
        )));
        vika.put("gift_hint", new DialogueNode("gift_hint", "viktoria.dialogue.gift_hint", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("follow_confirm", new DialogueNode("follow_confirm", "viktoria.dialogue.follow_confirm", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("status", new DialogueNode("status", "viktoria.dialogue.status", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("home", new DialogueNode("home", "viktoria.dialogue.home", List.of(
                new DialogueChoice("claim", "viktoria.choice.claim_home", "home_claimed", 5, "affection", "claim_home"),
                new DialogueChoice("live", "viktoria.choice.live_together", "live_together", 5, "affection", "live_together"),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("home_claimed", new DialogueNode("home_claimed", "viktoria.dialogue.home_claimed", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("live_together", new DialogueNode("live_together", "viktoria.dialogue.live_together", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        ), 25, null));
        vika.put("quest", new DialogueNode("quest", "viktoria.dialogue.quest", List.of(
                new DialogueChoice("accept", "viktoria.choice.accept_quest", "quest_accepted", 2, "joy", "start_quest"),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("quest_accepted", new DialogueNode("quest_accepted", "viktoria.dialogue.quest_accepted", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("intimate_gate", new DialogueNode("intimate_gate", "viktoria.dialogue.intimate_gate", List.of(
                new DialogueChoice("consent", "viktoria.choice.intimate_yes", "intimate_ok", 3, "affection", "intimate_consent"),
                new DialogueChoice("no", "viktoria.choice.intimate_no", "intimate_no", 0, null, null),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        ), 60, null));
        vika.put("intimate_ok", new DialogueNode("intimate_ok", "viktoria.dialogue.intimate_ok", List.of(
                new DialogueChoice("close", "viktoria.choice.close", "bye", 0, null, "intimate_pose")
        )));
        vika.put("intimate_no", new DialogueNode("intimate_no", "viktoria.dialogue.intimate_no", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("bye", new DialogueNode("bye", "viktoria.dialogue.bye", List.of(
                new DialogueChoice("close", "viktoria.choice.close", "bye", 0, null, "close")
        )));
        TREES.put(ViktoriaCharacter.ID, vika);
    }

    private DialogueTrees() {
    }

    public static Optional<DialogueNode> get(String characterId, String nodeId) {
        Map<String, DialogueNode> tree = TREES.get(characterId);
        if (tree == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tree.get(nodeId));
    }
}
