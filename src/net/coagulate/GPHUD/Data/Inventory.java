package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Modules.Inventory.UserInventoryException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Inventory extends CharacterSet {

    /**
     * Create a reference to a particular set on a character.
     *
     * @param character The character owning the set
     * @param set       The attribute defining the set
     */
    public Inventory(@Nonnull final Char character, @Nonnull final Attribute set) {
        super(character, set, true);
        if (set.getType() != Attribute.ATTRIBUTETYPE.INVENTORY) {
            throw new SystemImplementationException("Accessing a INVENTORY of a non INVENTORY attribute type " + set.getName() + " is " + set.getType());
        }
        if (set.getInstance() != character.getInstance()) {
            throw new SystemImplementationException("SetAttribute/Character instance mismatch");
        }
    }

    public static Set<Attribute> getAll(@Nonnull final State state) { return getAll(state.getInstance()); }
    public static Set<Attribute> getAll(@Nonnull final Instance instance) {
        final Set<Attribute> attributes = Attribute.getAttributes(instance);
        final Set<Attribute> sets = new HashSet<>();
        for (final Attribute attribute : attributes) {
            if (attribute.getType() == Attribute.ATTRIBUTETYPE.INVENTORY) {
                sets.add(attribute);
            }
        }
        return sets;
    }

    public static void deleteAll(final Instance instance, final String itemName) {
        for (final Attribute attribute : getAll(instance)) {
            db().d("delete from charactersets where attributeid=? and element=?", attribute.getId(), itemName);
        }
    }

    public boolean allows(@Nonnull final State st, @Nonnull final Item item) {
        for (final ResultsRow row : db().dq("select permitted from iteminventories where itemid=? and inventoryid=?", item.getId(), set.getId())) {
            return row.getBool();
        }
        return st.getKV("Inventory." + set.getName() + "DefaultAllow").boolValue();
    }

    public static boolean allows(@Nonnull final State st, @Nonnull final Attribute inventory, @Nonnull final Item item) {
        for (final ResultsRow row : db().dq("select permitted from iteminventories where itemid=? and inventoryid=?", item.getId(), inventory.getId())) {
            return row.getBool();
        }
        return st.getKV("Inventory." + inventory.getName() + "DefaultAllow").boolValue();
    }

    public static void allows(@Nonnull final State st, @Nonnull final Attribute inventory, @Nonnull final Item item, final boolean allow) {
        db().d("insert into iteminventories(itemid,inventoryid,permitted) values(?,?,?) on duplicate key update permitted=?", item.getId(), inventory.getId(), allow, allow);
    }

    @Nonnull
    public String getName() {
        return set.getName();
    }

    public Map<Item,Integer> getItems() {
        final Instance instance = character.getInstance();
        final Map<Item, Integer> output = new HashMap<>();
        for (final Map.Entry<String, Integer> element : elements().entrySet()) {
            final Item item = Item.findNullable(instance, element.getKey());
            if (item != null) {
                output.put(item, element.getValue());
            }
        }
        return output;
    }
    public int countItems() {
        return getItems().keySet().size();
    }
    public int countQuantity() {
        return getItems().values().stream().reduce(0, Integer::sum);
    }
    public int countWeight() {
        int weight = 0;
        for (final Map.Entry<Item, Integer> entry : getItems().entrySet()) {
            weight = weight + (entry.getKey().weight() * entry.getValue());
        }
        return weight;
    }

    public boolean accessible(final State state) {
        return state.getKV("Inventory." + getName() + "Accessible").boolValue();
    }

    public int count(final Item item) {
        return count(item.getName());
    }

    public int add(final Item item, final int quantity, final boolean checkAccessible) {
        // whole bunch of access checks to do
        final State state = new State(character);
        // Is the inventory accessible
        if (checkAccessible && !accessible(state)) {
            throw new UserInventoryException("Inventory " + getName() + " is not accessible from your current location");
        }
        // Is the item allowed in the inventory
        if (!allows(state, set, item)) {
            throw new UserInventoryException("Inventory " + getName() + " can not hold " + item.getName());
        }
        // check item/quantity/weight limits, but only if we're /adding/ items, removing items never checks this just in case
        if (quantity > 0) {
            final int maxItems = state.getKV("Inventory." + getName() + "MaxItems").intValue();
            final int maxQuantity = state.getKV("Inventory." + getName() + "MaxQuantity").intValue();
            final int maxWeight = state.getKV("Inventory." + getName() + "MaxWeight").intValue();
            // we only check if these limits aren't zero too
            if (maxItems > 0) {
                int numItems = countItems();
                if (count(item.getName()) == 0) {
                    numItems++;
                } // inventory does not contain this item
                if (numItems > maxItems) {
                    throw new UserInventoryException("Can not add " + item.getName() + " to " + getName() + ", it's full of items.");
                }
            }
            if (maxQuantity > 0) {
                final int newQuantity = countQuantity() + quantity;
                if (newQuantity > maxQuantity) {
                    throw new UserInventoryException("Can not add " + item.getName() + " to " + getName() + ", it would be completely full.");
                }
            }
            if (maxWeight > 0) {
                final int newWeight = countWeight() + (item.weight() * quantity);
                if (newWeight > maxWeight) {
                    throw new UserInventoryException("Can not add " + item.getName() + " to " + getName() + ", it would be too heavy.");
                }
            }
        }
        // guess we can add it then...
        return add(item.getName(), quantity);
    }

    public int set(final Item item, final Integer qty, final boolean checkAccessible) {
        final int delta = qty - count(item);
        return add(item, delta, checkAccessible);
    }
}
