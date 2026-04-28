package net.blay09.mods.waystones.item;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.world.BalmMenuProvider;
import net.blay09.mods.waystones.api.PlayerInfo;
import net.blay09.mods.waystones.menu.ModMenus;
import net.blay09.mods.waystones.menu.PlayerSelectionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 玩家传送物品 - 允许玩家传送到其他在线玩家
 */
public class PlayerCallItem extends Item {

    public static final UUID MOCK_OVERWORLD_NEAR_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID MOCK_OVERWORLD_FAR_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID MOCK_NETHER_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    public PlayerCallItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        final var itemStack = player.getItemInHand(hand);

        if (!world.isClientSide()) {
            // 播放音效
            world.playSound(null, player, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.1f, 2f);

            // 获取服务器在线玩家列表
            List<ServerPlayer> onlinePlayers = getPlayerList((ServerPlayer) player);

            // 打开玩家选择菜单
            Balm.networking().openMenu(player, new BalmMenuProvider<ModMenus.PlayerSelectionMenuData>() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.waystones.player_selection");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                    List<PlayerInfo> playerInfos = onlinePlayers.stream()
                            .map(p -> new PlayerInfo(p.getUUID(), p.getName().getString(), p.level().dimension(), p.blockPosition(), false))
                            .collect(Collectors.toList());
                    if (playerInfos.isEmpty() && player instanceof ServerPlayer serverPlayer) {
                        playerInfos = createMockPlayerInfos(serverPlayer);
                    }

                    return new PlayerSelectionMenu(ModMenus.playerSelection.value(), windowId, playerInfos)
                            .withWarpItem(itemStack)
                            .withHand(hand);
                }

                @Override
                public ModMenus.PlayerSelectionMenuData getScreenOpeningData(ServerPlayer serverPlayer) {
                    List<PlayerInfo> playerInfos = onlinePlayers.stream()
                            .map(p -> new PlayerInfo(p.getUUID(), p.getName().getString(), p.level().dimension(), p.blockPosition(), false))
                            .collect(Collectors.toList());

                    if (playerInfos.isEmpty()) {
                        playerInfos = createMockPlayerInfos(serverPlayer);
                    }

                    return new ModMenus.PlayerSelectionMenuData(playerInfos, itemStack);
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, ModMenus.PlayerSelectionMenuData> getScreenStreamCodec() {
                    return ModMenus.PlayerSelectionMenuData.STREAM_CODEC;
                }
            });
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 获取服务器在线玩家列表，排除当前玩家
     */
    private List<ServerPlayer> getPlayerList(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            return server.getPlayerList().getPlayers().stream()
                    .filter(otherPlayer -> !otherPlayer.getUUID().equals(player.getUUID()))
                    .toList();
        }
        return List.of();
    }

    public static List<PlayerInfo> createMockPlayerInfos(ServerPlayer currentPlayer) {
        BlockPos currentPos = currentPlayer.blockPosition();
        final var overworld = Level.OVERWORLD;
        final var nether = Level.NETHER;

        return List.of(
                new PlayerInfo(
                MOCK_OVERWORLD_NEAR_UUID,
                "测试玩家_Alex_近距离",
                overworld,
                new BlockPos(currentPos.getX() + 10, currentPos.getY(), currentPos.getZ() + 10),
                true
        ),
                new PlayerInfo(
                MOCK_OVERWORLD_FAR_UUID,
                "测试玩家_Steve_远距离",
                overworld,
                new BlockPos(48, Math.max(80, currentPos.getY()), -32),
                true
        ),
                new PlayerInfo(
                MOCK_NETHER_UUID,
                "测试玩家_Neo_下界",
                nether,
                new BlockPos(0, 80, 0),
                true
        ));
    }

    @Override
    public boolean isFoil(ItemStack itemStack) {
        return true;
    }
}
