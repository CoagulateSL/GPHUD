package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Inventory;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

public class InventoryModule extends ModuleAnnotation {
    public InventoryModule(String name, ModuleDefinition def) {
        super(name, def);
    }

    @Nonnull
    @Override
    public Map<String, KV> getKVDefinitions(@Nonnull State st) {
        Map<String,KV> kvSet=new TreeMap<>();
        for (Attribute inventory:Inventory.getInventories(st.getInstance())) {
            MaxItemsKV maxItemsKV=new MaxItemsKV(inventory);
            kvSet.put(maxItemsKV.name(),maxItemsKV);
            MaxQuantityKV maxQuantityKV=new MaxQuantityKV(inventory);
            kvSet.put(maxQuantityKV.name(),maxQuantityKV);
            MaxWeightKV maxWeightKV=new MaxWeightKV(inventory);
            kvSet.put(maxWeightKV.name(),maxWeightKV);
            AccessibleKV accessibleKV=new AccessibleKV(inventory);
            kvSet.put(accessibleKV.name(),accessibleKV);
            DefaultAllowKV defaultAllowKV=new DefaultAllowKV(inventory);
            kvSet.put(defaultAllowKV.name(),defaultAllowKV);
        }
        return kvSet;
    }

    @Nullable
    @Override
    public KV getKVDefinition(@Nonnull State st,@Nonnull String qualifiedName) {
        Map<String,KV> map=getKVDefinitions(st);
        String reference=Modules.extractReference(qualifiedName);
        if (reference==null) { return null; }
        for (String mapKey:map.keySet()) {
            if (reference.equalsIgnoreCase(mapKey)) { return map.get(mapKey); }
        }
        return null;
    }
}
