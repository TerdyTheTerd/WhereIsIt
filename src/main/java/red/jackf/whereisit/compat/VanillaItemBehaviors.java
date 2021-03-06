package red.jackf.whereisit.compat;

import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import red.jackf.whereisit.FoundType;
import red.jackf.whereisit.Searcher;
import red.jackf.whereisit.WhereIsIt;
import red.jackf.whereisit.api.InventoryUtils;
import red.jackf.whereisit.api.WhereIsItEntrypoint;

public class VanillaItemBehaviors implements WhereIsItEntrypoint {

    @Override
    public void setupBehaviors(Searcher searcher) {

        // Behaviors for standard inventories - Chests, hoppers, anything that implements
        // {@link net.minecraft.inventory.Inventory}
        searcher.addWorldBehavior(
            blockState ->
                blockState.getBlock() instanceof BlockWithEntity,
            (searchingFor, searchingForNbt, state, pos, world) -> {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof Inventory) {
                    return InventoryUtils.invContains((Inventory) be, searchingFor, searchingForNbt, true);
                }
                return FoundType.NOT_FOUND;
            }
        );

        // Behaviors for miscellaneous inventories - Compostors, LinkedStorage chests
        // Anything that implements {@link net.minecraft.block.InventoryProvider}
        searcher.addWorldBehavior(
            blockState ->
                blockState.getBlock() instanceof InventoryProvider,
            (searchingFor, searchingForNbt, state, pos, world) -> {
                Inventory inv = ((InventoryProvider) state.getBlock()).getInventory(state, world, pos);
                if (inv == null) return FoundType.NOT_FOUND;
                return InventoryUtils.invContains(inv, searchingFor, searchingForNbt, true);
            }
        );

        // Lecterns
        searcher.addWorldBehavior(
            blockState -> blockState.getBlock() instanceof LecternBlock && blockState.get(LecternBlock.HAS_BOOK),
            ((searchingFor, searchingForNbt, state, pos, world) -> {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof LecternBlockEntity) {
                    return WhereIsIt.SEARCHER.searchItemStack(((LecternBlockEntity) be).getBook(), searchingFor, searchingForNbt, true);
                }
                return FoundType.NOT_FOUND;
            })
        );

        // Shulker Box in inventories
        searcher.addItemBehavior(
            (itemStack -> itemStack.getItem() instanceof BlockItem && ((BlockItem) (itemStack.getItem())).getBlock() instanceof ShulkerBoxBlock),
            ((stack, searchingFor, searchingForNbt) -> {
                CompoundTag tag = stack.getSubTag("BlockEntityTag");
                if (tag != null && tag.contains("Items", 9)) {
                    ListTag items = tag.getList("Items", 10);
                    for (int i = 0; i < items.size(); i++) {
                        ItemStack containedStack = ItemStack.fromTag(items.getCompound(i));
                        if (containedStack.getItem() == searchingFor && (searchingForNbt == null || searchingForNbt.equals(containedStack.getTag()))) {
                            return true;
                        }
                    }
                }
                return false;
            })
        );
    }

    @Override
    public int getPriority() {
        return -1000;
    }
}
