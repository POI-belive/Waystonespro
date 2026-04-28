package net.blay09.mods.waystones.requirement;

import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.api.requirement.WarpRequirement;
import net.blay09.mods.waystones.config.WaystonesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * 玩家传送需求 - 处理玩家之间的传送成本
 */
public class PlayerTeleportRequirement implements WarpRequirement {

    private final ServerPlayer sourcePlayer;
    private final ServerPlayer targetPlayer;
    private final WaystonesConfig.PlayerCall config;

    public PlayerTeleportRequirement(ServerPlayer sourcePlayer, ServerPlayer targetPlayer) {
        this.sourcePlayer = sourcePlayer;
        this.targetPlayer = targetPlayer;
        this.config = WaystonesConfig.getActive().playerCall;
    }

    @Override
    public boolean canAfford(Player player) {
        final var requiredItem = config.getCostItem().orElse(null);

        // 检查是否需要消耗经验
        if (config.xpCost > 0 && !player.getAbilities().instabuild) {
            return player.experienceLevel >= config.xpCost;
        }

        // 检查是否需要消耗物品
        if (requiredItem != null) {
            final var heldItem = player.getMainHandItem();
            if (!heldItem.is(requiredItem)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void consume(Player player) {
        final var requiredItem = config.getCostItem().orElse(null);

        // 消耗经验
        if (config.xpCost > 0 && !player.getAbilities().instabuild) {
            player.giveExperienceLevels(-config.xpCost);
        }

        // 消耗物品（如果配置了）
        if (requiredItem != null && config.consumeItem) {
            final var heldItem = player.getMainHandItem();
            if (heldItem.is(requiredItem)) {
                heldItem.shrink(1);
            }
        }
    }

    @Override
    public void rollback(Player player) {
        // 回退经验消耗
        if (config.xpCost > 0) {
            player.giveExperienceLevels(config.xpCost);
        }

        // 回退物品消耗（需要特殊处理，因为已经在consume中减少了）
    }

    @Override
    public void appendHoverText(Player player, List<Component> tooltip) {
        final var requiredItem = config.getCostItem().orElse(null);

        // 添加经验成本提示
        if (config.xpCost > 0) {
            tooltip.add(Component.translatable("gui.waystones.player_selection.xp_cost", config.xpCost)
                    .withStyle(ChatFormatting.GREEN));
        }

        // 添加物品成本提示
        if (requiredItem != null) {
            tooltip.add(Component.translatable("gui.waystones.player_selection.item_cost",
                            requiredItem.getDefaultInstance().getHoverName())
                    .withStyle(ChatFormatting.GREEN));
        }

        // 添加冷却时间提示
        if (config.cooldownSeconds > 0) {
            int cooldownSeconds = config.cooldownSeconds;
            String cooldownText = formatCooldown(cooldownSeconds);
            tooltip.add(Component.translatable("gui.waystones.player_selection.cooldown", cooldownText)
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    /**
     * 格式化冷却时间显示
     */
    private String formatCooldown(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }

    @Override
    public boolean isEmpty() {
        return config.xpCost <= 0 && config.getCostItem().isEmpty();
    }
}
