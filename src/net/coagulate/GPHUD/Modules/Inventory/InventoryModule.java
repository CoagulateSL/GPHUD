package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Inventory;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.TreeMap;

public class InventoryModule extends ModuleAnnotation {
    public InventoryModule(String name, ModuleDefinition def) {
        super(name, def);
    }

    @Nonnull
    @Override
    public Map<String, KV> getKVDefinitions(State st) {
        Map<String,KV> kvSet=new TreeMap<>();
        for (Inventory inventory:Inventory.getInventories(st.getInstance())) {
            MaxItemsKV maxItemsKV=new MaxItemsKV(inventory);
            kvSet.put(maxItemsKV.name(),maxItemsKV);
            MaxQuantityKV maxQuantityKV=new MaxQuantityKV(inventory);
            kvSet.put(maxQuantityKV.name(),maxQuantityKV);
            MaxWeightKV maxWeightKV=new MaxWeightKV(inventory);
            kvSet.put(maxWeightKV.name(),maxWeightKV);
            AccessibleKV accessibleKV=new AccessibleKV(inventory);
            kvSet.put(accessibleKV.name(),accessibleKV);
        }
        return kvSet;
    }

    @Override
    public KV getKVDefinition(State st, String qualifiedname) {
        Map<String,KV> map=getKVDefinitions(st);
        String reference=Modules.extractReference(qualifiedname);
        if (reference==null) { return null; }
        for (String mapKey:map.keySet()) {
            if (reference.equalsIgnoreCase(mapKey)) { return map.get(mapKey); }
        }
        return null;
    }
}
