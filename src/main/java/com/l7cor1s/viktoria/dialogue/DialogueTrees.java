package com.l7cor1s.viktoria.dialogue;

import com.l7cor1s.viktoria.character.ViktoriaCharacter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Built-in dialogue trees used as fallback. Datapack JSON overlays merge on top via {@link DialogueLoader}.
 */
public final class DialogueTrees {
    private static final Map<String, Map<String, DialogueNode>> BUILTINS = new HashMap<>();
    private static final Map<String, Map<String, DialogueNode>> LOADED = new HashMap<>();

    static {
        Map<String, DialogueNode> vika = new HashMap<>();
        vika.put("greeting", new DialogueNode("greeting", "viktoria.dialogue.greeting",
                Map.of(
                        "lonely", "viktoria.dialogue.greeting_lonely",
                        "irritation", "viktoria.dialogue.greeting_irritated",
                        "joy", "viktoria.dialogue.greeting_joy",
                        "affection", "viktoria.dialogue.greeting_affection",
                        "jealousy", "viktoria.dialogue.greeting_jealousy"
                ),
                List.of(
                        new DialogueChoice("chat", "viktoria.choice.chat", "smalltalk", 2, "affection", null),
                        new DialogueChoice("comfort", "viktoria.choice.comfort", "comfort", 4, "affection", null, 0, List.of("lonely", "irritation")),
                        new DialogueChoice("flirt", "viktoria.choice.flirt", "flirt", 3, "affection", null, 35, List.of()),
                        new DialogueChoice("gift_hint", "viktoria.choice.gift_hint", "gift_hint", 0, null, null),
                        new DialogueChoice("follow", "viktoria.choice.follow", "follow_confirm", 1, "joy", "toggle_follow"),
                        new DialogueChoice("status", "viktoria.choice.status", "status", 0, null, "show_status"),
                        new DialogueChoice("home", "viktoria.choice.home", "home", 0, null, null),
                        new DialogueChoice("quest", "viktoria.choice.quest", "quest", 0, null, null),
                        new DialogueChoice("intimate", "viktoria.choice.intimate", "intimate_gate", 0, null, null, 60, List.of()),
                        new DialogueChoice("bye", "viktoria.choice.bye", "bye", 0, null, "close")
                ), 0, null));
        vika.put("smalltalk", new DialogueNode("smalltalk", "viktoria.dialogue.smalltalk", List.of(
                new DialogueChoice("day", "viktoria.choice.ask_day", "smalltalk_day", 2, "joy", null),
                new DialogueChoice("more", "viktoria.choice.more", "smalltalk2", 3, "affection", null),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("smalltalk_day", new DialogueNode("smalltalk_day", "viktoria.dialogue.smalltalk_day", List.of(
                new DialogueChoice("listen", "viktoria.choice.listen", "smalltalk2", 3, "affection", null),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("smalltalk2", new DialogueNode("smalltalk2", "viktoria.dialogue.smalltalk2", List.of(
                new DialogueChoice("compliment", "viktoria.choice.compliment", "complimented", 4, "joy", null),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 1, "joy", null)
        )));
        vika.put("complimented", new DialogueNode("complimented", "viktoria.dialogue.complimented", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 1, "affection", null)
        )));
        vika.put("comfort", new DialogueNode("comfort", "viktoria.dialogue.comfort", List.of(
                new DialogueChoice("hug", "viktoria.choice.hug", "comforted", 5, "affection", null),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("comforted", new DialogueNode("comforted", "viktoria.dialogue.comforted", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("flirt", new DialogueNode("flirt", "viktoria.dialogue.flirt", List.of(
                new DialogueChoice("soft", "viktoria.choice.flirt_soft", "flirt_ok", 4, "affection", null),
                new DialogueChoice("bold", "viktoria.choice.flirt_bold", "flirt_bold", 2, "joy", null, 50, List.of()),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        ), 35, null));
        vika.put("flirt_ok", new DialogueNode("flirt_ok", "viktoria.dialogue.flirt_ok", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("flirt_bold", new DialogueNode("flirt_bold", "viktoria.dialogue.flirt_bold", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 2, "affection", null)
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
                new DialogueChoice("live", "viktoria.choice.live_together", "live_together", 5, "affection", "live_together", 25, List.of()),
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
                new DialogueChoice("progress", "viktoria.choice.quest_progress", "quest_progress", 0, null, "quest_status"),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("quest_accepted", new DialogueNode("quest_accepted", "viktoria.dialogue.quest_accepted", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("quest_progress", new DialogueNode("quest_progress", "viktoria.dialogue.quest_progress", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("intimate_gate", new DialogueNode("intimate_gate", "viktoria.dialogue.intimate_gate", List.of(
                new DialogueChoice("consent", "viktoria.choice.intimate_yes", "intimate_ok", 3, "affection", "intimate_consent"),
                new DialogueChoice("no", "viktoria.choice.intimate_no", "intimate_no", 0, null, null),
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        ), 60, null));
        vika.put("intimate_ok", new DialogueNode("intimate_ok", "viktoria.dialogue.intimate_ok", List.of(
                new DialogueChoice("pose", "viktoria.choice.intimate_pose", "bye", 0, null, "intimate_pose"),
                new DialogueChoice("close_pose", "viktoria.choice.intimate_close", "bye", 0, null, "intimate_close"),
                new DialogueChoice("embrace", "viktoria.choice.intimate_embrace", "bye", 0, null, "intimate_embrace")
        )));
        vika.put("intimate_no", new DialogueNode("intimate_no", "viktoria.dialogue.intimate_no", List.of(
                new DialogueChoice("back", "viktoria.choice.back", "greeting", 0, null, null)
        )));
        vika.put("bye", new DialogueNode("bye", "viktoria.dialogue.bye", List.of(
                new DialogueChoice("close", "viktoria.choice.close", "bye", 0, null, "close")
        )));
        BUILTINS.put(ViktoriaCharacter.ID, vika);
    }

    private DialogueTrees() {
    }

    public static void replaceLoaded(Map<String, Map<String, DialogueNode>> trees) {
        LOADED.clear();
        trees.forEach((character, nodes) -> LOADED.put(character, new HashMap<>(nodes)));
    }

    public static Optional<DialogueNode> get(String characterId, String nodeId) {
        Map<String, DialogueNode> loaded = LOADED.get(characterId);
        if (loaded != null && loaded.containsKey(nodeId)) {
            return Optional.ofNullable(loaded.get(nodeId));
        }
        Map<String, DialogueNode> builtin = BUILTINS.get(characterId);
        if (builtin == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(builtin.get(nodeId));
    }

    public static Map<String, DialogueNode> builtinTree(String characterId) {
        return BUILTINS.getOrDefault(characterId, Map.of());
    }
}
