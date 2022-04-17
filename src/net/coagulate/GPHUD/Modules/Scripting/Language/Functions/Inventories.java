package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Inventory;
import net.coagulate.GPHUD.Data.Item;
import net.coagulate.GPHUD.Modules.Inventory.UserInventoryException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCList;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Map;

public class Inventories {
    @Nonnull
    @GSFunctions.GSFunction(description="Adds (or subtracts) items from an inventory",
                            returns="String - blank if everything went ok, otherwise a suitable error message (inventory full etc)",
                            parameters="Character character - Character to alter<br>"+
                                    "String inventory - Name of inventory to modify<br>"+
                                    "String item - Name of item to add/remove<br>"+
                                    "Integer amount - Ammount to alter quantity by (may be negative)",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.INVENTORY)
    public static BCString gsInventoryAdd(@Nonnull final State st,
                                    @Nonnull final GSVM vm,
                                    @Nonnull final BCCharacter character,
                                    @Nonnull final BCString inventoryName,
                                    @Nonnull final BCString itemName,
                                    @Nonnull final BCInteger amount) {
        final Inventory inventory = getInventory(st, character, inventoryName);
        try {
            final Item item = Item.find(st, itemName.getContent());
            final int oldValue = inventory.count(item);
            final int newValue = inventory.add(item, amount.getContent(), true);
            Audit.audit(true, st, Audit.OPERATOR.CHARACTER, null, character.getContent(), "gsInventoryAdd", inventory.getName(), "" + oldValue, "" + newValue, "Added " + amount.getContent() + " " + itemName.getContent() + " to inventory, changing total from " + oldValue + " to " + newValue);
            return new BCString(null, "");
        } catch (final UserInventoryException e) {
            return new BCString(null, e.getLocalizedMessage());
        }
    }

    @Nonnull
    @GSFunctions.GSFunction(description="Get the quantity of an item in an inventory, or zero if not present",
                            returns="Integer - Number of this item in inventory",
                            parameters="Character character - Character to interrogate<br>"+
                                    "String inventoryName - Name of inventory to count in<br>"+
                                    "String itemName - Name of item to count",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.INVENTORY)
    public static BCInteger gsInventoryGet(@Nonnull final State st,
                                     @Nonnull final GSVM vm,
                                     @Nonnull final BCCharacter character,
                                     @Nonnull final BCString inventoryName,
                                     @Nonnull final BCString itemName) {
        final Inventory inventory = getInventory(st, character, inventoryName);
        return new BCInteger(null,inventory.count(Item.find(st,itemName.getContent())));
    }

    @Nonnull
    @GSFunctions.GSFunction(description="Returns a count of the distinct items (regardless of quantity) in the inventory",
                            returns="Integer - Number of items in the inventory",
                            parameters="Character character - Character to count<br>"+
                                    "String inventoryName - Name of inventory to count",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.INVENTORY)
    public static BCInteger gsInventoryCount(@Nonnull final State st,
                                  @Nonnull final GSVM vm,
                                  @Nonnull final BCCharacter character,
                                  @Nonnull final BCString inventoryName) {
        return new BCInteger(null,getInventory(st,character,inventoryName).countItems());
    }

    @Nonnull
    @GSFunctions.GSFunction(description="Returns a count of the total number of items (counting quantities) in the inventory",
                            returns="Integer - Total of all quantities in the inventory",
                            parameters="Character character - Character to count<br>"+
                                    "String inventoryName - Name of inventory to count in",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.INVENTORY)
    public static BCInteger gsInventoryQuantity(@Nonnull final State st,
                                       @Nonnull final GSVM vm,
                                       @Nonnull final BCCharacter character,
                                       @Nonnull final BCString inventoryName) {
        return new BCInteger(null,getInventory(st,character,inventoryName).countQuantity());
    }

    @Nonnull
    @GSFunctions.GSFunction(description="Returns a count of the total weight in the inventory",
                            returns="Integer - Total of all weight in the inventory",
                            parameters="Character character - Character to count<br>"+
                                    "String inventoryName - Name of inventory to weigh",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.INVENTORY)
    public static BCInteger gsInventoryWeight(@Nonnull final State st,
                                              @Nonnull final GSVM vm,
                                              @Nonnull final BCCharacter character,
                                              @Nonnull final BCString inventoryName) {
        return new BCInteger(null,getInventory(st,character,inventoryName).countWeight());
    }

    private static Inventory getInventory(final State st, final BCCharacter character, final BCString inventoryName) {
        // find Attribute by name
        final Attribute attribute = Attribute.find(st.getInstance(), inventoryName.getContent());
        // Attribute must be a set
        if (attribute.getType() != Attribute.ATTRIBUTETYPE.INVENTORY) {
            throw new UserInputStateException("Attribute " + attribute.getName() + " is of type " + attribute.getType() + " not INVENTORY");
        }
        // Attribute must belong to instance (!)
        if (attribute.getInstance() != st.getInstance()) {
            throw new SystemImplementationException("Attribute " + attribute + " is not from instance " + st.getInstanceString());
        }
        // check character is of right instance
        if (st.getInstance() != character.getContent().getInstance()) {
            throw new SystemImplementationException("Target character " + character.getContent() + " is not from instance " + st.getInstanceString());
        }
        return new Inventory(character.getContent(), attribute);
    }

    @Nonnull
    @GSFunctions.GSFunction(description="Returns a list of all items in the inventory",
                            returns="List - A list of strings, consisting of the items in the inventory",
                            parameters="Character character - Character to query<br>"+
                                    "String inventory - Name of inventory to list",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.INVENTORY)
    public static BCList gsInventoryList(@Nonnull final State st,
                                   @Nonnull final GSVM vm,
                                   @Nonnull final BCCharacter character,
                                   @Nonnull final BCString inventory) {
        final Inventory source = getInventory(st, character, inventory);
        final BCList list = new BCList(null);
        for (final Map.Entry<String, Integer> element : source.elements().entrySet()) {
            list.append(new BCString(null, element.getKey()));
        }
        return list;
    }
}
