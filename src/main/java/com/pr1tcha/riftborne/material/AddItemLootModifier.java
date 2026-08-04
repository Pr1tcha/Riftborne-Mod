package com.pr1tcha.riftborne.material;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Adds one item to a specific vanilla loot table.
 *
 * <p>Used to seed predecessor equipment into the chests of old, dangerous places rather than
 * shipping structures of our own. The target table is matched explicitly so the modifier only ever
 * touches the table it names, leaving every other drop in the game alone.
 */
public class AddItemLootModifier extends LootModifier {
    public static final MapCodec<AddItemLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(inst.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item),
                    ResourceLocation.CODEC.fieldOf("target_table").forGetter(m -> m.targetTable)
            )).apply(inst, AddItemLootModifier::new));

    private final Item item;
    private final ResourceLocation targetTable;

    public AddItemLootModifier(LootItemCondition[] conditions, Item item, ResourceLocation targetTable) {
        super(conditions);
        this.item = item;
        this.targetTable = targetTable;
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ResourceLocation queried = context.getQueriedLootTableId();
        if (targetTable.equals(queried)) {
            loot.add(new ItemStack(item));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
