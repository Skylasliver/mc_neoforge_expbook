package dev.skylasliver.expbook.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.skylasliver.expbook.menu.BookAdminMenu;
import dev.skylasliver.expbook.util.BookLedger;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Administrative commands for opening and inspecting the per-player book ledger. */
public final class ExperienceBookCommands {
    private ExperienceBookCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("expbook")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("list")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> suggestPlayers(context.getSource(), builder))
                    .executes(context -> openPlayerBooks(context.getSource(), StringArgumentType.getString(context, "player")))))
            );
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestPlayers(
            CommandSourceStack source, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        Set<String> names = new LinkedHashSet<>();
        source.getServer().getPlayerList().getPlayers().forEach(player -> names.add(player.getGameProfile().getName()));
        BookLedger.all(source.getLevel()).values().forEach(entry -> {
            if (entry.ownerName() != null && !entry.ownerName().isBlank()) names.add(entry.ownerName());

        });
        for (String name : names) if (name.toLowerCase(java.util.Locale.ROOT).startsWith(builder.getRemaining().toLowerCase(java.util.Locale.ROOT))) builder.suggest(name);
        return builder.buildFuture();
    }

    private static int openPlayerBooks(CommandSourceStack source, String input) {
        ServerPlayer admin;
        try { admin = source.getPlayerOrException(); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) { source.sendFailure(Component.literal("此命令必须由管理员玩家执行")); return 0; }
        UUID owner = resolveOwner(source, input);
        if (owner == null) { source.sendFailure(Component.literal("未找到玩家：" + input + "。请使用补全中的玩家名。")); return 0; }
        var ids = BookLedger.activeFor(admin.serverLevel(), owner).keySet().stream().toList();
        admin.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new BookAdminMenu(id, inventory, owner, ids), Component.literal("经验书 - " + input)), buffer -> {
            buffer.writeUUID(owner);
            buffer.writeVarInt(ids.size());
            ids.forEach(buffer::writeUUID);
        });
        return ids.size();
    }

    private static UUID resolveOwner(CommandSourceStack source, String input) {
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(input);
        if (online != null) return online.getUUID();
        return BookLedger.all(source.getLevel()).values().stream()
            .filter(entry -> entry.ownerName() != null && entry.ownerName().equalsIgnoreCase(input))
            .map(BookLedger.Entry::owner).findFirst().orElse(null);
    }

}
