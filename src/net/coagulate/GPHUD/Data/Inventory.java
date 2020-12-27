package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Inventory extends TableRow {
    @Nonnull private final Attribute inventory;

    @Nonnull public static Set<Inventory> getInventories(@Nonnull final Instance instance) {
        Set<Inventory> inventory=new HashSet<>();
        for (ResultsRow row:db().dq("select attributeid from attributes where instanceid=? and attributetype='INVENTORY'",instance.getId())) {
            inventory.add(Inventory.get(row.getInt("attributeid")));
        }
        return inventory;
    }

    @Nonnull
    @Override
    public String getName() {
        return inventory.getName();
    }

    public CharacterSet set(@Nonnull final Char character) {
        return new CharacterSet(character,inventory);
    }
    public Map<Item,Integer> getItems(@Nonnull final Char character) {
        Instance instance=character.getInstance();
        Map<Item,Integer> output=new HashMap<>();
        CharacterSet set=set(character);
        for (Map.Entry<String,Integer> element:set.elements().entrySet()) {
            Item item=Item.findNullable(instance,element.getKey());
            if (item!=null) { output.put(item,element.getValue()); }
        }
        return output;
    }
    public int countItems(@Nonnull final Char character) {
        return getItems(character).keySet().size();
    }
    public int countQuantity(@Nonnull final Char character) {
        return getItems(character).values().stream().reduce(0, Integer::sum);
    }
    public int countWeight(@Nonnull final Char character) {
        int weight=0;
        for (Map.Entry<Item,Integer> entry:getItems(character).entrySet()) {
           weight=weight+(entry.getKey().weight() * entry.getValue());
        }
        return weight;
    }

    public int maxItems() { return getInt("maxitems"); }
    public void maxItems(int newMax) { set("maxitems",newMax); }
    public int maxQuantity() { return getInt("maxquantity"); }
    public void maxQuantity(int newMax) { set("maxquantity",newMax); }
    public int maxWeight() { return getInt("maxweight"); }
    public void maxWeight(int newMax) { set("maxweight",newMax); }
    public boolean defaultAllow() { return getBool("defaultallow"); }
    public void defaultAllow(boolean newAllow) { set("defaultallow",newAllow); }

    @Nonnull
    @Override
    public String getIdColumn() {
        return "attributeid";
    }

    @Override
    public void validate(@Nonnull State st) {
    }

    @Nullable
    @Override
    public String getNameField() {
        return null;
    }

    @Nullable
    @Override
    public String getLinkTarget() {
        return null;
    }

    @Nullable
    @Override
    public String getKVTable() {
        return null;
    }

    @Nullable
    @Override
    public String getKVIdField() {
        return null;
    }

    @Override
    protected int getNameCacheTime() {
        return 0;
    }

    @Nonnull
    @Override
    public String getTableName() {
        return "inventoryconfiguration";
    }
    /**
     * Factory style constructor
     *
     * @param id the ID number we want to get
     *
     * @return An Instance representation
     */
    @Nonnull
    public static Inventory get(@Nonnull final Integer id) {
        return (Inventory) factoryPut("Inventory",id,new Inventory(id));
    }
    protected Inventory(final int id) {
        super(id);
        this.inventory=new Attribute(id);
        if (inventory.getType()!= Attribute.ATTRIBUTETYPE.INVENTORY) {
            throw new SystemImplementationException("Attribute "+inventory+" is not an INVENTORY attribute");
        }
    }

}
