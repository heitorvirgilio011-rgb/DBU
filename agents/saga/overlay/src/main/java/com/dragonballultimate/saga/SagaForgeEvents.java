package com.dragonballultimate.saga;

import com.dragonballultimate.DragonBallUltimate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = DragonBallUltimate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SagaForgeEvents {
    private static final String ACTOR_TAG_PREFIX = "dbu_saga_";
    private static final String MESSAGE_PREFIX = "saga." + DragonBallUltimate.MOD_ID + ".message.";
    private static final double MAX_DISTANCE_SQUARED = 36.0;

    private SagaForgeEvents() {}

    @SubscribeEvent
    public static void onReloadListeners(AddReloadListenerEvent event) {
        SagaReload.register(event);
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || player instanceof FakePlayer || player.isSpectator() || !player.isAlive()
                || !(event.getTarget() instanceof Villager villager) || !villager.isAlive()
                || player.level() != villager.level() || player.distanceToSqr(villager) > MAX_DISTANCE_SQUARED) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null || !server.isSameThread()) {
            return;
        }

        String actor = actorId(villager);
        SagaCatalog catalog = SagaReload.catalog();
        if (actor == null || !catalog.knowsActor(actor)) {
            return;
        }

        SagaProgress.Result result = SagaSavedData.forServer(server).interact(player.getUUID(), actor, catalog);
        if (result.outcome() == SagaProgress.Outcome.IGNORED) {
            return;
        }

        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
        sendResult(player, result);
    }

    /** Reads a committed quest milestone on the server thread for ORACLE. */
    public static boolean hasMilestone(ServerPlayer player, String id) {
        MinecraftServer server = player.getServer();
        return server != null && server.isSameThread()
                && SagaSavedData.forServer(server).hasMilestone(player.getUUID(), id);
    }

    private static String actorId(Villager villager) {
        String actor = null;
        for (String tag : villager.getTags()) {
            if (!tag.startsWith(ACTOR_TAG_PREFIX)) {
                continue;
            }
            if (actor != null || tag.length() == ACTOR_TAG_PREFIX.length()) {
                return null;
            }
            actor = DragonBallUltimate.MOD_ID + ':' + tag.substring(ACTOR_TAG_PREFIX.length());
        }
        return actor;
    }

    private static void sendResult(ServerPlayer player, SagaProgress.Result result) {
        String key = MESSAGE_PREFIX + result.outcome().name().toLowerCase(Locale.ROOT);
        Component quest = translatedId(result.questId(), "", ".title");
        Component next = translatedId(result.nextActor(), "actor.", "");
        Component description = translatedId(result.questId(), "", ".description");
        int seals = result.receipt() == null ? 0 : result.receipt().seals();
        player.sendSystemMessage(Component.translatable(key, quest, next, result.reached(), result.total(), seals,
                description));
    }

    private static Component translatedId(String id, String prefix, String suffix) {
        if (id == null) {
            return Component.empty();
        }
        var location = new ResourceLocation(id);
        String key = "saga." + location.getNamespace() + '.' + prefix + location.getPath() + suffix;
        return Component.translatableWithFallback(key, location.toString());
    }
}
