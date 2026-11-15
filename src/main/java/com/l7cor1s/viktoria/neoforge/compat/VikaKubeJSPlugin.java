package com.l7cor1s.viktoria.neoforge.compat;

import com.l7cor1s.viktoria.api.VikaAPI;
import com.l7cor1s.viktoria.api.ScriptData;
import com.l7cor1s.viktoria.entity.VikaEntity;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * KubeJS support: the {@code Laura} global (see {@link VikaAPI}) and the {@code VikaEvents}
 * server events. Listed in kubejs.plugins.txt, so it is only loaded when KubeJS is installed.
 * <pre>
 * VikaEvents.chat(e =&gt; {
 *   if (e.detail == 'hello') { Laura.say(e.laura, e.player, 'Hi from a script!'); e.cancel() }
 * })
 * Laura.addGift({ match: 'minecraft:emerald_block', affection: 60, tier: 'AMAZING' })
 * </pre>
 *
 * @author vyrriox
 */
public class VikaKubeJSPlugin implements KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("VikaEvents");
    // Every handler accepts a result, like the 1.20.1 plugins: without it KubeJS swallows
    // event.cancel() and logs an error instead. Only the chat event acts on a cancel (it stops her
    // default answer); for the others a cancel is accepted and changes nothing.
    public static final EventHandler SUMMON = GROUP.server("summon", () -> Event.class).hasResult();
    public static final EventHandler DEATH = GROUP.server("death", () -> Event.class).hasResult();
    public static final EventHandler REVIVE = GROUP.server("revive", () -> Event.class).hasResult();
    public static final EventHandler DESIRE_FULFILLED = GROUP.server("desireFulfilled", () -> Event.class).hasResult();
    public static final EventHandler DESIRE_FAILED = GROUP.server("desireFailed", () -> Event.class).hasResult();
    public static final EventHandler GIFT = GROUP.server("gift", () -> Event.class).hasResult();
    public static final EventHandler EMOTE = GROUP.server("emote", () -> Event.class).hasResult();
    public static final EventHandler CHAT = GROUP.server("chat", () -> Event.class).hasResult();

    private static final Map<String, EventHandler> HANDLERS = Map.of(
            "summon", SUMMON, "death", DEATH, "revive", REVIVE, "desire_fulfilled", DESIRE_FULFILLED,
            "desire_failed", DESIRE_FAILED, "gift", GIFT, "emote", EMOTE, "chat", CHAT);

    /** What scripts receive: the companion, the player (may be null) and a detail (intent, item id...). */
    public static class Event implements KubeEvent {
        private final VikaEntity laura;
        @Nullable
        private final ServerPlayer player;
        private final String detail;

        public Event(VikaEntity laura, @Nullable ServerPlayer player, String detail) {
            this.laura = laura;
            this.player = player;
            this.detail = detail;
        }

        public VikaEntity getLaura() {
            return laura;
        }

        @Nullable
        public ServerPlayer getPlayer() {
            return player;
        }

        public String getDetail() {
            return detail;
        }
    }

    @Override
    public void init() {
        VikaAPI.setExternalEvents((event, laura, player, detail) -> {
            EventHandler handler = HANDLERS.get(event);
            if (handler == null || !handler.hasListeners()) {
                return false;
            }
            EventResult result = handler.post(new Event(laura, player, detail));
            return result.interruptFalse();
        });
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("Laura", VikaAPI.class);
    }

    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("com.l7cor1s.viktoria.api");
    }

    @Override
    public void beforeScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType == ScriptType.SERVER) {
            // Scripts run again from scratch: forget what they added last time.
            ScriptData.clear();
            VikaAPI.clearListeners();
        }
    }

    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType == ScriptType.SERVER) {
            ScriptData.requestReload();
        }
    }
}
