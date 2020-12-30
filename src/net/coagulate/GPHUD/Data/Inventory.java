package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Inventory extends CharacterSet {

    /** Create a reference to a particular set on a character.
     *
     * @param character The character owning the set
     * @param set The attribute defining the set
     */
    public Inventory(@Nonnull Char character,@Nonnull Attribute set) {
        super(character,set,true);
        if (set.getType()!= Attribute.ATTRIBUTETYPE.INVENTORY) {
            throw new SystemImplementationException("Accessing a INVENTORY of a non INVENTORY attribute type "+set.getName()+" is "+set.getType());
        }
        if (set.getInstance()!=character.getInstance()) {
            throw new SystemImplementationException("SetAttribute/Character instance mismatch");
        }
    }

    public static final Set<Attribute> getAll(@Nonnull final State state) { return getAll(state.getInstance()); }
    public static final Set<Attribute> getAll(@Nonnull final Instance instance) {
        Set<Attribute> attributes=Attribute.getAttributes(instance);
        Set<Attribute> sets=new HashSet<>();
        for (Attribute attribute:attributes) {
            if (attribute.getType()== Attribute.ATTRIBUTETYPE.INVENTORY) {
                sets.add(attribute);
            }
        }
        return sets;
    }
    public static boolean allows(@Nonnull State st,@Nonnull Attribute inventory,@Nonnull Item item) {
        for (ResultsRow row:db().dq("select permitted from iteminventories where itemid=? and inventoryid=?",item.getId(),inventory.getId())) {
            return row.getBool();
        }
        return st.getKV("Inventory."+inventory.getName()+"DefaultAllow").boolValue();
    }
    public static void allows(@Nonnull State st,@Nonnull Attribute inventory,@Nonnull Item item,boolean allow) {
        db().d("insert into iteminventories(itemid,inventoryid,permitted) values(?,?,?) on duplicate key update permitted=?",item.getId(),inventory.getId(),allow,allow);
    }

    @Nonnull
    public String getName() {
        return set.getName();
    }

    public Map<Item,Integer> getItems(@Nonnull final Char character) {
        Instance instance=character.getInstance();
        Map<Item,Integer> output=new HashMap<>();
        for (Map.Entry<String,Integer> element:elements().entrySet()) {
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

}
