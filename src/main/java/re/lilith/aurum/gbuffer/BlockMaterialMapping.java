package re.lilith.aurum.gbuffer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import re.lilith.aurum.shaderpack.materialmap.BlockEntry;
import re.lilith.aurum.shaderpack.materialmap.BlockRenderType;
import re.lilith.aurum.shaderpack.materialmap.NamespacedId;

import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class BlockMaterialMapping {
    public static Object2IntMap<BlockState> createBlockStateIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap) {
        Object2IntMap<BlockState> blockMatches = new Object2IntOpenHashMap<>();

        blockPropertiesMap.forEach((intId, entries) -> {
            for (BlockEntry entry : entries) {
                addBlock(entry, blockMatches, intId);
            }
        });

        return blockMatches;
    }

    public static Map<Block, RenderLayer> createBlockTypeMap(Map<NamespacedId, BlockRenderType> blockPropertiesMap) {
        Map<Block, RenderLayer> blockTypeIds = new Reference2ReferenceOpenHashMap<>();

        blockPropertiesMap.forEach((id, blockType) -> {
            Identifier resourceLocation = new Identifier(id.namespace(), id.name());

            Block block = Block.get(resourceLocation.toString());

            blockTypeIds.put(block, convertBlockToRenderType(blockType));
        });

        return blockTypeIds;
    }

    private static RenderLayer convertBlockToRenderType(BlockRenderType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case SOLID -> RenderLayer.SOLID;
            case CUTOUT -> RenderLayer.CUTOUT;
            case CUTOUT_MIPPED -> RenderLayer.CUTOUT_MIPPED;
            case TRANSLUCENT -> RenderLayer.TRANSLUCENT;
        };
    }

    private static void addBlock(BlockEntry entry, Object2IntMap<BlockState> idMap, int intId) {
        final NamespacedId id = entry.id();
        final Identifier resourceLocation = new Identifier(id.namespace(), id.name());

        final Block block = Block.get(resourceLocation.toString());

        if (block == null || block == Blocks.AIR) {
            return;
        }

        final Map<String, String> predicates = entry.propertyPredicates();

        for (BlockState state : block.getStateManager().getBlockStates()) {
            if (matches(state, predicates)) {
                idMap.put(state, intId);
            }
        }
    }

    private static boolean matches(BlockState state, Map<String, String> predicates) {
        if (predicates.isEmpty()) {
            return true;
        }

        int matched = 0;

        for (Map.Entry<Property, Comparable> property : state.getPropertyMap().entrySet()) {
            String expected = predicates.get(property.getKey().getName());

            if (expected == null) {
                continue;
            }

            if (!expected.equals(name(property.getKey(), property.getValue()))) {
                return false;
            }

            matched++;
        }

        // A filter that names a property the block does not have must not match.
        return matched == predicates.size();
    }

    @SuppressWarnings("unchecked")
    private static String name(Property property, Comparable value) {
        return property.name(value);
    }
}