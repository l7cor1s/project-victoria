package com.l7cor1s.viktoria.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.config.VikaConfig;
import com.l7cor1s.viktoria.desire.Desire;
import com.l7cor1s.viktoria.dialogue.DialogueManager;
import com.l7cor1s.viktoria.dialogue.LocaleUtil;
import com.l7cor1s.viktoria.entity.CombatMode;
import com.l7cor1s.viktoria.entity.Emote;
import com.l7cor1s.viktoria.entity.VikaEntity;
import com.l7cor1s.viktoria.entity.VikaJob;
import com.l7cor1s.viktoria.entity.VikaSpeech;
import com.l7cor1s.viktoria.entity.brain.Needs;
import com.l7cor1s.viktoria.entity.work.ChestPurpose;
import com.l7cor1s.viktoria.entity.work.VikaTask;
import com.l7cor1s.viktoria.entity.work.VikaWorkplace;
import com.l7cor1s.viktoria.network.VikaAction;
import com.l7cor1s.viktoria.skin.AssetKind;
import com.l7cor1s.viktoria.skin.ServerAssetStore;
import com.l7cor1s.viktoria.skin.SkinRef;
import com.l7cor1s.viktoria.skin.SkinService;
import com.l7cor1s.viktoria.world.VikaActions;
import com.l7cor1s.viktoria.world.VikaManager;
import com.l7cor1s.viktoria.world.VikaWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * The {@code /laura} command tree.
 *
 * @author vyrriox
 */
public final class VikaCommand {
    private static final SuggestionProvider<CommandSourceStack> ITEMS = (ctx, builder) -> {
        List<String> ids = new ArrayList<>();
        ids.add("held");
        BuiltInRegistries.ITEM.keySet().forEach(id -> ids.add(id.toString()));
        return SharedSuggestionProvider.suggest(ids, builder);
    };
    private static final SuggestionProvider<CommandSourceStack> EMOTES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Emote.values()).filter(e -> e.playerTriggered).map(Emote::animationName), builder);
    private static final SuggestionProvider<CommandSourceStack> SKINS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ServerAssetStore.list(AssetKind.SKIN).stream().map(ServerAssetStore.Entry::name), builder);
    private static final SuggestionProvider<CommandSourceStack> BUILTIN_SKINS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(SkinRef.BUILTIN, builder);
    private static final SuggestionProvider<CommandSourceStack> MODELS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ServerAssetStore.list(AssetKind.MODEL).stream().map(ServerAssetStore.Entry::name), builder);

    private VikaCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("vika").executes(VikaCommand::help);
        root.then(Commands.literal("help").executes(VikaCommand::help));
        root.then(Commands.literal("summon").executes(c -> {
            VikaManager.summon(c.getSource().getPlayerOrException(), false);
            return 1;
        }));
        root.then(Commands.literal("dismiss").executes(c -> withLaura(c, VikaManager::dismiss)));
        // For good: asked twice, because nothing brings her back afterwards.
        root.then(Commands.literal("release")
                .executes(c -> withLaura(c, (p, l) -> p.sendSystemMessage(
                        Component.translatable("viktoria.release.confirm", l.getLauraName()).withStyle(ChatFormatting.RED))))
                .then(Commands.literal("confirm").executes(c -> withLaura(c, VikaManager::release))));
        root.then(Commands.literal("list").executes(c -> {
            VikaManager.list(c.getSource().getPlayerOrException());
            return 1;
        }));
        root.then(Commands.literal("select").then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) {
                        return builder.buildFuture();
                    }
                    return SharedSuggestionProvider.suggest(VikaWorldData.get(p.getServer()).byOwner(p.getUUID()).stream().map(r -> r.lauraName), builder);
                })
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String name = StringArgumentType.getString(c, "name");
                    VikaWorldData data = VikaWorldData.get(p.getServer());
                    for (VikaWorldData.Record r : data.byOwner(p.getUUID())) {
                        if (r.lauraName.equalsIgnoreCase(name.trim())) {
                            data.meta(p.getUUID()).selected = r.laura;
                            data.setDirty();
                            p.sendSystemMessage(Component.translatable("viktoria.list.selected", r.lauraName));
                            return 1;
                        }
                    }
                    c.getSource().sendFailure(Component.translatable("viktoria.not_found"));
                    return 0;
                })));
        root.then(Commands.literal("where").executes(c -> {
            VikaManager.where(c.getSource().getPlayerOrException());
            return 1;
        }));
        root.then(Commands.literal("come").executes(c -> farAction(c, VikaAction.COME, "")));
        root.then(action("follow", VikaAction.FOLLOW));
        root.then(action("stay", VikaAction.STAY));
        root.then(action("wander", VikaAction.WANDER));
        root.then(action("stop", VikaAction.STOP));
        root.then(action("hug", VikaAction.HUG));
        root.then(action("kiss", VikaAction.KISS));
        root.then(action("compliment", VikaAction.COMPLIMENT));
        root.then(action("eat", VikaAction.EAT));
        root.then(action("sleep", VikaAction.SLEEP));
        root.then(action("wakeup", VikaAction.WAKE_UP));
        root.then(action("ungag", VikaAction.UNGAG));
        root.then(action("info", VikaAction.INFO));
        root.then(action("inventory", VikaAction.INVENTORY));
        root.then(Commands.literal("home")
                .executes(c -> farAction(c, VikaAction.HOME, ""))
                .then(Commands.literal("go").executes(c -> farAction(c, VikaAction.HOME, "")))
                .then(Commands.literal("set").executes(c -> run(c, VikaAction.SET_HOME, "")))
                .then(Commands.literal("clear").executes(c -> run(c, VikaAction.CLEAR_HOME, ""))));
        root.then(Commands.literal("fetch")
                .then(Commands.argument("request", StringArgumentType.greedyString()).suggests(ITEMS)
                        .executes(c -> fetch(c, StringArgumentType.getString(c, "request")))));
        root.then(taskCommand("chop", VikaTask.Type.CHOP_TREE));
        root.then(taskCommand("harvest", VikaTask.Type.HARVEST));
        root.then(taskCommand("cook", VikaTask.Type.COOK));
        root.then(Commands.literal("queue")
                .then(Commands.literal("list").executes(VikaCommand::queueList))
                .then(Commands.literal("clear").executes(c -> withLaura(c, (p, l) -> {
                    l.workplace().clearQueue();
                    l.workGoal().reset();
                    p.sendSystemMessage(Component.translatable("viktoria.queue.cleared"));
                })))
                .then(Commands.literal("remove").then(Commands.argument("index", IntegerArgumentType.integer(1, VikaWorkplace.MAX_QUEUE))
                        .executes(c -> withLaura(c, (p, l) -> {
                            boolean ok = l.workplace().removeQueued(IntegerArgumentType.getInteger(c, "index") - 1);
                            p.sendSystemMessage(Component.translatable(ok ? "viktoria.queue.removed" : "viktoria.queue.invalid"));
                        }))))
                .then(Commands.literal("add")
                        .then(queueAdd("chop", VikaTask.Type.CHOP_TREE))
                        .then(queueAdd("harvest", VikaTask.Type.HARVEST))
                        .then(queueAdd("cook", VikaTask.Type.COOK))
                        .then(queueAdd("come", VikaTask.Type.COME))
                        .then(queueAdd("home", VikaTask.Type.GO_HOME))
                        .then(queueAdd("follow", VikaTask.Type.FOLLOW))
                        .then(queueAdd("stay", VikaTask.Type.STAY))
                        .then(queueAdd("work", VikaTask.Type.WORK))));
        LiteralArgumentBuilder<CommandSourceStack> job = Commands.literal("job");
        for (VikaJob j : VikaJob.values()) {
            if (j == VikaJob.NONE) {
                continue;
            }
            job.then(Commands.literal(j.key())
                    .executes(c -> withLaura(c, (p, l) -> VikaActions.enableJob(p, l, j, 0)))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(3, 64))
                            .executes(c -> withLaura(c, (p, l) -> VikaActions.enableJob(p, l, j, IntegerArgumentType.getInteger(c, "radius"))))));
        }
        LiteralArgumentBuilder<CommandSourceStack> jobStop = Commands.literal("stop").executes(c -> withLaura(c, (p, l) -> VikaActions.disableJob(p, l, null)));
        for (VikaJob j : VikaJob.values()) {
            if (j != VikaJob.NONE) {
                jobStop.then(Commands.literal(j.key()).executes(c -> withLaura(c, (p, l) -> VikaActions.disableJob(p, l, j))));
            }
        }
        job.then(jobStop);
        job.then(Commands.literal("list").executes(VikaCommand::jobList));
        root.then(job);
        root.then(Commands.literal("work").executes(c -> withLaura(c, VikaActions::backToWork)));
        LiteralArgumentBuilder<CommandSourceStack> chest = Commands.literal("chest");
        for (ChestPurpose purpose : ChestPurpose.values()) {
            chest.then(Commands.literal(purpose.key()).executes(c -> withLaura(c, (p, l) -> VikaActions.assignChest(p, l, purpose))));
        }
        chest.then(Commands.literal("remove").executes(c -> withLaura(c, (p, l) -> VikaActions.assignChest(p, l, null))));
        chest.then(Commands.literal("clear").executes(c -> withLaura(c, (p, l) -> {
            l.workplace().clearChests();
            p.sendSystemMessage(Component.translatable("viktoria.chest.cleared"));
        })));
        chest.then(Commands.literal("list").executes(VikaCommand::chestList));
        root.then(chest);
        root.then(Commands.literal("emote").then(Commands.argument("emote", StringArgumentType.word()).suggests(EMOTES)
                .executes(c -> run(c, VikaAction.EMOTE, StringArgumentType.getString(c, "emote")))));
        LiteralArgumentBuilder<CommandSourceStack> mode = Commands.literal("mode");
        for (CombatMode m : CombatMode.values()) {
            mode.then(Commands.literal(m.key()).executes(c -> run(c, VikaAction.COMBAT, m.name())));
        }
        root.then(mode);
        root.then(Commands.literal("pickup")
                .then(Commands.literal("on").executes(c -> run(c, VikaAction.PICKUP, "on")))
                .then(Commands.literal("off").executes(c -> run(c, VikaAction.PICKUP, "off"))));
        root.then(Commands.literal("answer")
                .then(Commands.literal("yes").executes(c -> run(c, VikaAction.ANSWER, "yes")))
                .then(Commands.literal("no").executes(c -> run(c, VikaAction.ANSWER, "no"))));
        root.then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.greedyString())
                .executes(c -> run(c, VikaAction.RENAME, StringArgumentType.getString(c, "name")))));
        root.then(Commands.literal("desire").executes(c -> withLaura(c, (p, l) -> {
            Desire d = l.brain().desire();
            if (d == null) {
                VikaSpeech.say(l, p, "desire.none", com.l7cor1s.viktoria.dialogue.LineFormatter.values());
            } else {
                VikaSpeech.say(l, p, "desire.tell", com.l7cor1s.viktoria.dialogue.LineFormatter.values().with("item", d.describe()).with("place", d.describe()).with("activity", d.describe()));
            }
        })));
        root.then(skinCommand());
        root.then(modelCommand());
        root.then(Commands.literal("lang").executes(VikaCommand::lang));
        root.then(Commands.literal("reload").requires(s -> s.hasPermission(VikaConfig.reloadPermissionLevel.getInt())).executes(c -> {
            ViktoriaMod.reloadAll();
            for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) {
                com.l7cor1s.viktoria.network.VikaNetwork.sendSettings(p);
            }
            c.getSource().sendSuccess(() -> Component.translatable("viktoria.reloaded"), true);
            return 1;
        }));
        root.then(adminCommand());
        var node = dispatcher.register(root);
        dispatcher.register(Commands.literal("viktoria").executes(VikaCommand::help).redirect(node));
        dispatcher.register(Commands.literal("laura").executes(VikaCommand::help).redirect(node));
    }

    // ------------------------------------------------------------------ helpers

    private static LiteralArgumentBuilder<CommandSourceStack> action(String name, VikaAction action) {
        return Commands.literal(name).executes(c -> run(c, action, ""));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> taskCommand(String name, VikaTask.Type type) {
        return Commands.literal(name)
                .executes(c -> withLaura(c, (p, l) -> VikaActions.task(p, l, type, 0, false)))
                .then(Commands.argument("radius", IntegerArgumentType.integer(3, 64))
                        .executes(c -> withLaura(c, (p, l) -> VikaActions.task(p, l, type, IntegerArgumentType.getInteger(c, "radius"), false))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> queueAdd(String name, VikaTask.Type type) {
        return Commands.literal(name).executes(c -> withLaura(c, (p, l) -> {
            if (type.isErrand()) {
                VikaActions.task(p, l, type, 0, true);
            } else if (l.workplace().enqueue(new VikaTask(type, "", 0, null))) {
                p.sendSystemMessage(Component.translatable("viktoria.queue.added", new VikaTask(type, "", 0, null).describe()));
            }
        }));
    }

    /** Runs with the player's Laura, which must be loaded and close enough. */
    private static int withLaura(CommandContext<CommandSourceStack> c, BiConsumer<ServerPlayer, VikaEntity> body) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        VikaEntity laura = VikaManager.findNear(player, 96);
        if (laura == null) {
            c.getSource().sendFailure(Component.translatable("viktoria.not_near"));
            return 0;
        }
        body.accept(player, laura);
        return 1;
    }

    private static int run(CommandContext<CommandSourceStack> c, VikaAction action, String arg) throws CommandSyntaxException {
        return withLaura(c, (p, l) -> VikaActions.perform(p, l, action, arg, VikaActions.Source.COMMAND));
    }

    /** Actions that work even when she is far away or in another dimension. */
    private static int farAction(CommandContext<CommandSourceStack> c, VikaAction action, String arg) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        VikaEntity laura = VikaManager.find(player);
        if (laura == null) {
            if (action == VikaAction.COME) {
                // None of hers is loaded: recall them, and only create one if she has none at all.
                VikaManager.call(player);
                return 1;
            }
            c.getSource().sendFailure(Component.translatable("viktoria.not_found"));
            return 0;
        }
        return VikaActions.perform(player, laura, action, arg, VikaActions.Source.COMMAND) ? 1 : 0;
    }

    /**
     * {@code <item> [count] [queue]}. The item is an id ("minecraft:bread"), a #tag, "held" or free
     * text ("some bread"), none of which a single unquoted word argument can hold, so the whole
     * request is read at once and the optional count and "queue" are taken from its end.
     */
    private static int fetch(CommandContext<CommandSourceStack> c, String request) throws CommandSyntaxException {
        List<String> words = new java.util.ArrayList<>(List.of(request.trim().split("\\s+")));
        boolean queue = words.size() > 1 && words.get(words.size() - 1).equalsIgnoreCase("queue");
        if (queue) {
            words.remove(words.size() - 1);
        }
        int count = 0;
        if (words.size() > 1 && words.get(words.size() - 1).matches("\\d{1,3}")) {
            count = Math.max(1, Math.min(576, Integer.parseInt(words.remove(words.size() - 1))));
        }
        String item = String.join(" ", words);
        int amount = count;
        return withLaura(c, (p, l) -> VikaActions.fetch(p, l, item, amount, queue));
    }

    /** A file name or URL followed by an optional "true" or "false" (slim arms). */
    private record SkinArg(String value, boolean slim) {
        static SkinArg parse(String raw, boolean defaultSlim) {
            String text = raw.trim();
            int space = text.lastIndexOf(' ');
            if (space > 0) {
                String last = text.substring(space + 1);
                if (last.equalsIgnoreCase("true") || last.equalsIgnoreCase("false")) {
                    return new SkinArg(text.substring(0, space).trim(), Boolean.parseBoolean(last));
                }
            }
            return new SkinArg(text, defaultSlim);
        }
    }

    private static int help(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        source.sendSuccess(() -> Component.translatable("viktoria.help.title").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        for (String key : List.of("summon", "orders", "home", "fetch", "tasks", "jobs", "chests", "emotes", "look", "info", "chat")) {
            source.sendSuccess(() -> Component.translatable("viktoria.help." + key).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int queueList(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return withLaura(c, (p, l) -> {
            VikaWorkplace work = l.workplace();
            if (work.current() == null && work.queued().isEmpty()) {
                p.sendSystemMessage(Component.translatable("viktoria.queue.empty"));
                return;
            }
            if (work.current() != null) {
                p.sendSystemMessage(Component.literal("> ").append(work.current().describe()).withStyle(ChatFormatting.YELLOW));
            }
            int i = 1;
            for (VikaTask task : work.queued()) {
                p.sendSystemMessage(Component.literal(i++ + ". ").append(task.describe()));
            }
        });
    }

    private static int jobList(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return withLaura(c, (p, l) -> {
            if (l.workplace().jobs().isEmpty()) {
                p.sendSystemMessage(Component.translatable("viktoria.job.none_active"));
                return;
            }
            l.workplace().jobs().forEach((job, area) -> p.sendSystemMessage(Component.translatable("viktoria.job." + job.key()).append(": " + area.describe())));
        });
    }

    private static int chestList(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return withLaura(c, (p, l) -> {
            if (l.workplace().chests().isEmpty()) {
                p.sendSystemMessage(Component.translatable("viktoria.chest.none"));
                return;
            }
            for (VikaWorkplace.ChestAssignment a : l.workplace().chests()) {
                p.sendSystemMessage(Component.translatable("viktoria.chest." + a.purpose().key())
                        .append(": " + a.pos().getX() + " " + a.pos().getY() + " " + a.pos().getZ() + " (" + a.dimension().location() + ")"));
            }
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> skinCommand() {
        return Commands.literal("skin")
                .then(Commands.literal("reset").executes(c -> skin(c, "reset", true)))
                .then(Commands.literal("list").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MutableComponent list = Component.translatable("viktoria.skin.list_builtin").append(" " + String.join(", ", SkinRef.BUILTIN));
                    p.sendSystemMessage(list);
                    List<String> files = ServerAssetStore.list(AssetKind.SKIN).stream().map(ServerAssetStore.Entry::name).toList();
                    p.sendSystemMessage(Component.translatable("viktoria.skin.list_files").append(" " + (files.isEmpty() ? "-" : String.join(", ", files))));
                    return 1;
                }))
                .then(Commands.literal("builtin").then(Commands.argument("name", StringArgumentType.word()).suggests(BUILTIN_SKINS)
                        .executes(c -> skin(c, "builtin:" + StringArgumentType.getString(c, "name"), true))))
                // File names ("uploads/x") and URLs hold characters an unquoted word cannot: the rest of
                // the line is read at once, with an optional "true" or "false" (slim arms) at its end.
                .then(Commands.literal("file").then(Commands.argument("name", StringArgumentType.greedyString()).suggests(SKINS)
                        .executes(c -> {
                            SkinArg arg = SkinArg.parse(StringArgumentType.getString(c, "name"), true);
                            return skin(c, "server:" + arg.value(), arg.slim());
                        })))
                .then(Commands.literal("player").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> skin(c, "player:" + StringArgumentType.getString(c, "name"), true))))
                .then(Commands.literal("url").then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(c -> {
                            SkinArg arg = SkinArg.parse(StringArgumentType.getString(c, "url"), false);
                            return skin(c, "url:" + arg.value(), arg.slim());
                        })));
    }

    private static int skin(CommandContext<CommandSourceStack> c, String ref, boolean slim) throws CommandSyntaxException {
        return withLaura(c, (p, l) -> SkinService.requestSkin(p, l, ref, slim));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> modelCommand() {
        return Commands.literal("model")
                .then(Commands.literal("reset").executes(c -> withLaura(c, (p, l) -> SkinService.requestModel(p, l, ""))))
                .then(Commands.literal("list").executes(c -> {
                    List<String> models = ServerAssetStore.list(AssetKind.MODEL).stream().map(ServerAssetStore.Entry::name).toList();
                    c.getSource().getPlayerOrException().sendSystemMessage(Component.translatable("viktoria.model.list").append(" " + (models.isEmpty() ? "-" : String.join(", ", models))));
                    return 1;
                }))
                .then(Commands.argument("name", StringArgumentType.greedyString()).suggests(MODELS)
                        .executes(c -> withLaura(c, (p, l) -> SkinService.requestModel(p, l, StringArgumentType.getString(c, "name")))));
    }

    private static int lang(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<String> chain = DialogueManager.chainFor(p.clientInformation().language());
        p.sendSystemMessage(Component.translatable("viktoria.lang.player", p.clientInformation().language(), String.join(" > ", chain)));
        p.sendSystemMessage(Component.translatable("viktoria.lang.available", String.join(", ", DialogueManager.languages())));
        String used = chain.stream().filter(code -> DialogueManager.sets().containsKey(code)).findFirst().orElse(LocaleUtil.DEFAULT);
        p.sendSystemMessage(Component.translatable("viktoria.lang.used", used, DialogueManager.lineCounts().getOrDefault(used, 0)));
        return 1;
    }

    // ------------------------------------------------------------------ admin

    private static LiteralArgumentBuilder<CommandSourceStack> adminCommand() {
        return Commands.literal("admin").requires(s -> s.hasPermission(VikaConfig.reloadPermissionLevel.getInt()))
                .then(Commands.literal("list").executes(c -> {
                    VikaWorldData data = VikaWorldData.get(c.getSource().getServer());
                    if (data.all().isEmpty()) {
                        c.getSource().sendSuccess(() -> Component.translatable("viktoria.admin.none"), false);
                    }
                    for (VikaWorldData.Record r : data.all()) {
                        String state = r.dead ? "grave" : r.respawnAt >= 0 ? "respawning" : r.dismissed ? "dismissed" : "active";
                        c.getSource().sendSuccess(() -> Component.literal(r.ownerName + " : " + r.lauraName + " [" + state + "] "
                                + r.pos.getX() + " " + r.pos.getY() + " " + r.pos.getZ() + " " + r.dimension.location()), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("remove").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                    ServerPlayer target = EntityArgument.getPlayer(c, "player");
                    VikaManager.removeAll(c.getSource().getServer(), target);
                    c.getSource().sendSuccess(() -> Component.translatable("viktoria.admin.removed", target.getName()), true);
                    return 1;
                })))
                .then(Commands.literal("affection").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000)).executes(c -> {
                            VikaEntity laura = VikaManager.find(EntityArgument.getPlayer(c, "player"));
                            if (laura == null) {
                                c.getSource().sendFailure(Component.translatable("viktoria.not_found"));
                                return 0;
                            }
                            laura.setAffection(IntegerArgumentType.getInteger(c, "value"));
                            c.getSource().sendSuccess(() -> Component.literal("OK"), true);
                            return 1;
                        }))))
                .then(Commands.literal("need").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("need", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Needs.Need.values()).map(Needs.Need::key), b))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100)).executes(c -> {
                                    VikaEntity laura = VikaManager.find(EntityArgument.getPlayer(c, "player"));
                                    String name = StringArgumentType.getString(c, "need").toUpperCase(Locale.ROOT);
                                    Needs.Need need = Arrays.stream(Needs.Need.values()).filter(n -> n.name().equals(name)).findFirst().orElse(null);
                                    if (laura == null || need == null) {
                                        c.getSource().sendFailure(Component.translatable("viktoria.not_found"));
                                        return 0;
                                    }
                                    laura.brain().needs().set(need, IntegerArgumentType.getInteger(c, "value"));
                                    laura.brain().syncToEntity();
                                    c.getSource().sendSuccess(() -> Component.literal("OK"), true);
                                    return 1;
                                })))))
                .then(Commands.literal("desire").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("clear").executes(c -> {
                            VikaEntity laura = VikaManager.find(EntityArgument.getPlayer(c, "player"));
                            if (laura != null) {
                                laura.brain().clearDesire();
                            }
                            return laura == null ? 0 : 1;
                        }))
                        .then(Commands.literal("roll").executes(c -> {
                            VikaEntity laura = VikaManager.find(EntityArgument.getPlayer(c, "player"));
                            if (laura == null) {
                                return 0;
                            }
                            long now = laura.level().getGameTime();
                            laura.brain().forceDesire(Desire.roll(laura.getRandom(), now, VikaConfig.desireDeadlineMinutes.getInt() * 1200L));
                            return 1;
                        }))))
                .then(Commands.literal("emote").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("emote", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Emote.values()).map(Emote::animationName), b))
                                .executes(c -> {
                                    VikaEntity laura = VikaManager.find(EntityArgument.getPlayer(c, "player"));
                                    if (laura == null) {
                                        return 0;
                                    }
                                    laura.playEmote(Emote.byName(StringArgumentType.getString(c, "emote")));
                                    return 1;
                                }))));
    }
}
