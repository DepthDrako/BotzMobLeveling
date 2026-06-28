package com.botzlabz.mobleveling.command;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.api.BotzMobLevelingAPI;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code /botzmobleveling} query commands. These exist mainly so datapacks can read level
 * values: each command returns the value as its Brigadier result, so a pack can capture it
 * with {@code execute store result score ...} and drive a HUD (e.g. an actionbar readout)
 * even when no mobs are nearby.
 *
 * <p>Commands are <strong>silent by default</strong> — they return the value but print nothing,
 * so they can be polled every tick without chat spam (no {@code sendCommandFeedback} gamerule
 * change needed). Append {@code print} to echo the value to chat for manual inspection:
 * <pre>
 *   /botzmobleveling arealevel               (silent, returns result)
 *   /botzmobleveling arealevel print         (also prints to chat)
 *   /botzmobleveling arealevel ~ ~ ~ print   (at a position, prints)
 *   /botzmobleveling moblevel @e[...,limit=1] print
 * </pre>
 * Available to all players (read-only).
 */
@Mod.EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobLevelingCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("botzmobleveling")
                        .requires(source -> true)
                        .then(Commands.literal("arealevel")
                                .executes(ctx -> areaLevel(ctx, BlockPos.containing(ctx.getSource().getPosition()), false))
                                .then(Commands.literal("print")
                                        .executes(ctx -> areaLevel(ctx, BlockPos.containing(ctx.getSource().getPosition()), true)))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> areaLevel(ctx, BlockPosArgument.getBlockPos(ctx, "pos"), false))
                                        .then(Commands.literal("print")
                                                .executes(ctx -> areaLevel(ctx, BlockPosArgument.getBlockPos(ctx, "pos"), true)))))
                        .then(Commands.literal("moblevel")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> mobLevel(ctx, EntityArgument.getEntity(ctx, "target"), false))
                                        .then(Commands.literal("print")
                                                .executes(ctx -> mobLevel(ctx, EntityArgument.getEntity(ctx, "target"), true)))))
        );
    }

    private static int areaLevel(CommandContext<CommandSourceStack> ctx, BlockPos pos, boolean print) {
        CommandSourceStack source = ctx.getSource();
        int areaLevel = BotzMobLevelingAPI.getAreaLevel(source.getLevel(), pos);
        if (print) {
            source.sendSuccess(() -> Component.literal("Area level at " + pos.getX() + ", " + pos.getY()
                    + ", " + pos.getZ() + ": " + areaLevel), false);
        }
        return areaLevel;
    }

    private static int mobLevel(CommandContext<CommandSourceStack> ctx, Entity target, boolean print) {
        CommandSourceStack source = ctx.getSource();
        if (!(target instanceof Mob mob)) {
            source.sendFailure(Component.literal("That entity is not a leveled mob."));
            return 0;
        }
        int level = BotzMobLevelingAPI.getMobLevel(mob);
        if (print) {
            source.sendSuccess(() -> Component.literal(mob.getName().getString() + " level: " + level), false);
        }
        return level;
    }
}
