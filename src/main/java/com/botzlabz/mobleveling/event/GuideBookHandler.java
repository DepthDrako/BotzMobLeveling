package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuideBookHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String NBT_PLAYER_GUIDE_GIVEN = "botzmobleveling_PlayerGuideGiven";
    private static final String NBT_DEVELOPER_GUIDE_GIVEN = "botzmobleveling_DeveloperGuideGiven";

    private static final ResourceLocation PATCHOULI_BOOK_ITEM_ID = ResourceLocation.parse("patchouli:guide_book");
    private static final ResourceLocation PLAYER_GUIDE_BOOK_ID = ResourceLocation.parse(BotzMobLeveling.MOD_ID + ":mob_leveling_player_guide");
    private static final ResourceLocation DEVELOPER_GUIDE_BOOK_ID = ResourceLocation.parse(BotzMobLeveling.MOD_ID + ":mob_leveling_developer_guide");

    private static boolean warnedAboutMissingPatchouli = false;

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        giveGuideIfNeeded(player,
                MobLevelingConfig.GIVE_PLAYER_GUIDE_ON_FIRST_JOIN.get(),
                NBT_PLAYER_GUIDE_GIVEN,
                PLAYER_GUIDE_BOOK_ID);

        giveGuideIfNeeded(player,
                MobLevelingConfig.GIVE_DEVELOPER_GUIDE_ON_FIRST_JOIN.get(),
                NBT_DEVELOPER_GUIDE_GIVEN,
                DEVELOPER_GUIDE_BOOK_ID);
    }

    private static void giveGuideIfNeeded(ServerPlayer player, boolean enabled, String nbtFlag, ResourceLocation bookId) {
        if (!enabled) {
            return;
        }

        CompoundTag persisted = getPersistedData(player);
        if (persisted.getBoolean(nbtFlag) && playerHasGuide(player, bookId)) {
            return;
        }

        Item patchouliGuideBook = ForgeRegistries.ITEMS.getValue(PATCHOULI_BOOK_ITEM_ID);
        if (patchouliGuideBook == null || patchouliGuideBook == Items.AIR) {
            if (!warnedAboutMissingPatchouli) {
                LOGGER.warn("[{}] Patchouli is not available; guide books cannot be given automatically.", BotzMobLeveling.MOD_ID);
                warnedAboutMissingPatchouli = true;
            }
            player.sendSystemMessage(Component.literal("[BotzMobLeveling] Patchouli is missing, so the guide book cannot be given."));
            return;
        }

        ItemStack guide = new ItemStack(patchouliGuideBook);
        guide.getOrCreateTag().putString("patchouli:book", bookId.toString());

        boolean inserted = player.getInventory().add(guide);
        if (!inserted) {
            player.drop(guide, false);
        }

        persisted.putBoolean(nbtFlag, true);
    }

    private static boolean playerHasGuide(ServerPlayer player, ResourceLocation bookId) {
        String expectedBookId = bookId.toString();

        for (ItemStack stack : player.getInventory().items) {
            if (isMatchingGuideStack(stack, expectedBookId)) {
                return true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (isMatchingGuideStack(stack, expectedBookId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isMatchingGuideStack(ItemStack stack, String expectedBookId) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (!PATCHOULI_BOOK_ITEM_ID.equals(itemId)) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && expectedBookId.equals(tag.getString("patchouli:book"));
    }

    private static CompoundTag getPersistedData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistentData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistentData.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
