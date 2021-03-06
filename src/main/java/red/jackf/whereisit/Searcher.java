package red.jackf.whereisit;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import red.jackf.whereisit.api.CustomItemBehavior;
import red.jackf.whereisit.api.CustomWorldBehavior;

import java.util.*;
import java.util.function.Predicate;

import static red.jackf.whereisit.WhereIsIt.log;

public class Searcher {
    private final List<ItemBehavior> itemBehaviors = new LinkedList<>();
    private final List<WorldBehavior> worldBehaviors = new LinkedList<>();

    public Map<BlockPos, FoundType> searchWorld(BlockPos basePos, ServerWorld world, Item toFind, CompoundTag toFindTag) {
        Map<BlockPos, FoundType> positions = new HashMap<>();
        final int radius = WhereIsIt.CONFIG.getSearchRadius();
        int checkedBECount = 0;

        int minChunkX = (-radius + basePos.getX()) >> 4;
        int maxChunkX = (radius + 1 + basePos.getX()) >> 4;
        int minChunkZ = (-radius + basePos.getZ()) >> 4;
        int maxChunkZ = (radius + 1 + basePos.getZ()) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {

                WorldChunk chunk = world.getChunk(chunkX, chunkZ);
                if (chunk == null) continue;

                checkedBECount += chunk.getBlockEntities().size();

                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    if (pos.isWithinDistance(basePos, radius)) {
                        BlockState state = chunk.getBlockState(pos);
                        try {
                            for (WorldBehavior behavior : worldBehaviors) {
                                if (behavior.getTest().test(state)) {
                                    FoundType result = behavior.getAction().containsItem(toFind, toFindTag, state, pos, world);
                                    if (result != FoundType.NOT_FOUND) {
                                        positions.put(pos.toImmutable(), result);
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            log("Error searching for item in " + state.getBlock() + ": " + ex.toString() + "|" + Arrays.toString(ex.getStackTrace()));
                        }
                    }
                }
            }
        }

        if (WhereIsIt.CONFIG.printSearchTime()) {
            WhereIsIt.log("Checked " + checkedBECount + " BlockEntities");
        }

        return positions;
    }

    public FoundType searchItemStack(ItemStack itemStack, Item toFind, CompoundTag nbtToFind, boolean deepSearch) {
        if (itemStack.getItem() == toFind && (nbtToFind == null || nbtToFind.equals(itemStack.getTag()))) {
            return FoundType.FOUND;
        } else if (!itemStack.isEmpty() && WhereIsIt.CONFIG.doDeepSearch() && deepSearch) {
            for (ItemBehavior behavior : itemBehaviors) {
                if (behavior.getTest().test(itemStack) && behavior.getAction().containsItem(itemStack, toFind, nbtToFind)) {
                    return FoundType.FOUND_DEEP;
                }
            }
        }
        return FoundType.NOT_FOUND;
    }

    public void addItemBehavior(Predicate<ItemStack> test, CustomItemBehavior action) {
        itemBehaviors.add(new ItemBehavior(test, action));
    }

    public void addWorldBehavior(Predicate<BlockState> test, CustomWorldBehavior action) {
        worldBehaviors.add(new WorldBehavior(test, action));
    }

    public static class ItemBehavior {
        private final Predicate<ItemStack> test;
        private final CustomItemBehavior action;

        public ItemBehavior(Predicate<ItemStack> test, CustomItemBehavior action) {
            this.test = test;
            this.action = action;
        }

        public CustomItemBehavior getAction() {
            return action;
        }

        public Predicate<ItemStack> getTest() {
            return test;
        }
    }

    public static class WorldBehavior {
        private final Predicate<BlockState> test;
        private final CustomWorldBehavior action;

        public WorldBehavior(Predicate<BlockState> test, CustomWorldBehavior action) {
            this.test = test;
            this.action = action;
        }

        public CustomWorldBehavior getAction() {
            return action;
        }

        public Predicate<BlockState> getTest() {
            return test;
        }
    }
}
