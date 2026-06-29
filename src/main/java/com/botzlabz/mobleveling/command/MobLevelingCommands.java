package com.botzlabz.mobleveling.command;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.api.BotzMobLevelingAPI;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Read-only commands exposing {@link BotzMobLevelingAPI}.
 *
 * <ul>
 *   <li>{@code /botzmobleveling arealevel [pos] [print]} — the level a generic mob
 *       would spawn at (current position if {@code pos} omitted).</li>
 *   <li>{@code /botzmobleveling moblevel <entity> [print]} — a specific mob's total level.</li>
 * </ul>
 *
 * <p>Available to all players (permission level 0) since they only read data.
 * <b>Silent by default</b>: each command returns the value as its command result
 * (capturable via {@code execute store result score ...}) but prints nothing, so
 * a datapack can poll it every tick without chat spam. Append {@code print} to
 * also echo the value to chat for manual use.
 */
@EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class MobLevelingCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("botzmobleveling")
                .requires(src -> true) // read-only: everyone may use it
                .then(Commands.literal("arealevel")
                    .executes(ctx -> areaLevel(ctx, sourcePos(ctx), false))
                    .then(Commands.literal("print")
                        .executes(ctx -> areaLevel(ctx, sourcePos(ctx), true)))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> areaLevel(ctx, BlockPosArgument.getBlockPos(ctx, "pos"), false))
                        .then(Commands.literal("print")
                            .executes(ctx -> areaLevel(ctx, BlockPosArgument.getBlockPos(ctx, "pos"), true)))))
                .then(Commands.literal("moblevel")
                    .then(Commands.argument("entity", EntityArgument.entity())
                        .executes(ctx -> mobLevel(ctx, false))
                        .then(Commands.literal("print")
                            .executes(ctx -> mobLevel(ctx, true)))))
        );
    }

    // -------------------------------------------------------------------------

    private static BlockPos sourcePos(CommandContext<CommandSourceStack> ctx) {
        return BlockPos.containing(ctx.getSource().getPosition());
    }

    private static int areaLevel(CommandContext<CommandSourceStack> ctx, BlockPos pos, boolean print) {
        ServerLevel level = ctx.getSource().getLevel();
        int areaLevel = BotzMobLevelingAPI.getAreaLevel(level, pos);
        if (print) {
            BlockPos p = pos;
            ctx.getSource().sendSuccess(() -> Component.literal(
                "Area level at " + p.getX() + ", " + p.getY() + ", " + p.getZ() + ": " + areaLevel), false);
        }
        return areaLevel;
    }

    private static int mobLevel(CommandContext<CommandSourceStack> ctx, boolean print) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(ctx, "entity");
        int level = (entity instanceof Mob mob) ? BotzMobLevelingAPI.getMobLevel(mob) : 0;
        if (print) {
            String name = entity.getName().getString();
            ctx.getSource().sendSuccess(() -> Component.literal(name + " level: " + level), false);
        }
        return level;
    }

    private MobLevelingCommands() {}
}
