package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Inventory;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

public class InventoryModule extends ModuleAnnotation {
    public InventoryModule(final String name, final ModuleDefinition def) {
        super(name, def);
    }

    @Nonnull
    @Override
    public Map<String, KV> getKVDefinitions(@Nonnull final State st) {
        final Map<String, KV> kvSet = new TreeMap<>();
        for (final Attribute inventory : Inventory.getAll(st.getInstance())) {
            final MaxItemsKV maxItemsKV = new MaxItemsKV(inventory);
            kvSet.put(maxItemsKV.name(), maxItemsKV);
            final MaxQuantityKV maxQuantityKV = new MaxQuantityKV(inventory);
            kvSet.put(maxQuantityKV.name(), maxQuantityKV);
            final MaxWeightKV maxWeightKV = new MaxWeightKV(inventory);
            kvSet.put(maxWeightKV.name(), maxWeightKV);
            final AccessibleKV accessibleKV = new AccessibleKV(inventory);
            kvSet.put(accessibleKV.name(), accessibleKV);
            final DefaultAllowKV defaultAllowKV = new DefaultAllowKV(inventory);
            kvSet.put(defaultAllowKV.name(), defaultAllowKV);
        }
        return kvSet;
    }

    @Nullable
    @Override
    public KV getKVDefinition(@Nonnull final State st, @Nonnull final String reference) {
        final Map<String, KV> map = getKVDefinitions(st);
        for (final String mapKey : map.keySet()) {
            if (reference.equalsIgnoreCase(mapKey)) {
                return map.get(mapKey);
            }
        }
        return null;
    }
}
