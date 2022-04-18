package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Items.VerbActor;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class Interactions {

    @Command.Commands(description = "Opens a menu of an inventory to interact with",
                      permitScripting = false,
                      permitExternal = false,
                      context = Command.Context.CHARACTER,
                      permitObject = false,
                      permitUserWeb = false)
    public static Response interact(@Nonnull final State state,
                                    @Argument.Arguments(type = Argument.ArgumentType.INVENTORY,
                                                        description = "Inventory to interact with",
                                                        name = "inventory") @Nonnull final Attribute inventory) {
        final Inventory inv = new Inventory(state.getCharacter(), inventory);
        if (!inv.accessible(state)) {
            return new ErrorResponse("Inventory " + inventory.getName() + " is not accessible from this location!");
        }
        final JSONObject json = new JSONObject();
        json.put("inventory", inventory.getName());
        json.put("arg0name", "item");
        json.put("arg0description", "Pick an item to interact with");
        json.put("arg0type", "SELECT");
        int button = 0;
        for (final String item : inv.elements().keySet()) {
            json.put("arg0button" + button, item);
            button++;
        }
        json.put("args", 1);
        json.put("invoke", "Inventory.InteractItem");
        json.put("incommand", "runtemplate");
        return new JSONResponse(json);
    }

    @Command.Commands(description = "Interact with an item in an inventory",
                      permitScripting = false,
                      permitExternal = false,
                      context = Command.Context.CHARACTER,
                      permitObject = false,
                      permitUserWeb = false)
    public static Response interactItem(@Nonnull final State state,
                                        @Argument.Arguments(type = Argument.ArgumentType.INVENTORY,
                                                            description = "Inventory to interact with",
                                                            name = "inventory") @Nonnull final Attribute inventory,
                                        @Argument.Arguments(name = "item",
                                                            description = "Item to interact with",
                                                            type = Argument.ArgumentType.ITEM) @Nonnull final Item item) {
        final Inventory inv = new Inventory(state.getCharacter(), inventory);
        if (!inv.accessible(state)) {
            return new ErrorResponse("Inventory " + inventory.getName() + " is not accessible from this location!");
        }
        final int count = inv.count(item);
        if (count < 1) {
            return new ErrorResponse("Your inventory " + inventory.getName() + " does not contain any " + item.getName());
        }
        final JSONObject json = new JSONObject();
        json.put("inventory", inventory.getName());
        json.put("item", item.getName());
        json.put("arg0name", "verb");
        json.put("arg0description", "Pick an action\n" + item.getName() + "\n(Qty: " + count + ")");
        json.put("arg0type", "SELECT");
        int button = 0;
        if (item.destroyable()) {
            json.put("arg0button" + button, "Destroy");
            button++;
        }
        if (item.tradable()) {
            json.put("arg0button" + button, "Give To");
            button++;
        }
        int accessible = 0;
        for (final Attribute checking : Inventory.getAll(state)) {
            final Inventory checkInventory = new Inventory(state.getCharacter(), checking);
            if (checkInventory.accessible(state) && checkInventory.allows(state, item)) {
                accessible++;
            }
        }
        if (accessible > 1) {
            json.put("arg0button" + button, "Move To");
            button++;
        }
        for (final ItemVerb verb : ItemVerb.findAll(item)) {
            json.put("arg0button" + button, verb.getName());
            button++;
        }
        json.put("args", 1);
        json.put("invoke", "Inventory.InteractItemVerb");
        json.put("incommand", "runtemplate");
        return new JSONResponse(json);
    }

    @Command.Commands(description = "Interact with an item in an inventory",
                      permitScripting = false,
                      permitExternal = false,
                      context = Command.Context.CHARACTER,
                      permitObject = false,
                      permitUserWeb = false,
                      notes = "This is called by Inventory.InteractItem usually, you would start most user interactions from Inventory.Interact command")
    public static Response interactItemVerb(@Nonnull final State state,
                                            @Argument.Arguments(type = Argument.ArgumentType.INVENTORY,
                                                                description = "Inventory to interact with",
                                                                name = "inventory") @Nonnull final Attribute inventory,
                                            @Argument.Arguments(name = "item",
                                                                description = "Item to interact with",
                                                                type = Argument.ArgumentType.ITEM) @Nonnull final Item item,
                                            @Argument.Arguments(name = "verb",
                                                                description = "Interaction verb to use",
                                                                type = Argument.ArgumentType.TEXT_ONELINE,
                                                                max = 64) @Nonnull final String verb) {
        final Inventory inv = new Inventory(state.getCharacter(), inventory);
        if (!inv.accessible(state)) {
            return new ErrorResponse("Inventory " + inventory.getName() + " is not accessible from this location!");
        }
        final int count = inv.count(item);
        if (count < 1) {
            return new ErrorResponse("Your inventory " + inventory.getName() + " does not contain any " + item.getName());
        }
        final JSONObject json = new JSONObject();
        json.put("incommand", "runtemplate");
        json.put("inventory", inventory.getName());
        json.put("item", item.getName());
        if ("Destroy".equalsIgnoreCase(verb)) {
            json.put("invoke", "Inventory.DestroyItem");
            json.put("arg0name", "quantity");
            json.put("arg0description", "Ammount of " + item.getName() + " to destroy from " + inventory.getName() + "\n(You have " + count + ")\nUse -1 to destroy ALL");
            json.put("arg0type", "TEXTBOX");
            json.put("args", 1);
            return new JSONResponse(json);
        }
        if ("Give To".equalsIgnoreCase(verb)) {
            json.put("invoke", "Inventory.GiveItem");
            json.put("arg0name", "target");
            json.put("arg0description", "Who to give to?");
            json.put("arg0type", "SENSORCHAR");
            //json.put("arg0manual","fortesting");
            json.put("arg1name", "quantity");
            json.put("arg1description", "Ammount of " + item.getName() + " to give from " + inventory.getName() + "\n(You have " + count + ")\nUse -1 to GIVE ALL");
            json.put("arg1type", "TEXTBOX");
            json.put("args", 2);
            return new JSONResponse(json);
        }
        if ("Move To".equalsIgnoreCase(verb)) {
            json.put("invoke", "Inventory.MoveItem");
            json.put("arg0name", "target");
            json.put("arg0description", "Inventory to move to?");
            json.put("arg0type", "SELECT");
            int accessible = 0;
            for (final Attribute checking : Inventory.getAll(state)) {
                if (checking.getId() != inventory.getId()) {
                    final Inventory checkInventory = new Inventory(state.getCharacter(), checking);
                    if (checkInventory.accessible(state)) {
                        if (checkInventory.allows(state, item)) {
                            json.put("arg0button" + accessible, checking.getName());
                            accessible++;
                        }
                    }
                }
            }
            //json.put("arg0manual","fortesting");
            json.put("arg1name", "quantity");
            json.put("arg1description", "Ammount of " + item.getName() + " to move from " + inventory.getName() + "\n(You have " + count + ")\nUse -1 to MOVE ALL");
            json.put("arg1type", "TEXTBOX");
            json.put("args", 2);
            return new JSONResponse(json);
        }
        final ItemVerb itemVerb = ItemVerb.findNullable(item, verb);
        if (itemVerb==null) { return new ErrorResponse("Unknown interaction '" + verb + "' on item " + item.getName()); }
        return VerbActor.act(state,inventory,item,itemVerb);
    }

    @Command.Commands(description = "Destroys an item from an inventory",
                      permitScripting = false,
                      permitExternal = false,
                      context = Command.Context.CHARACTER,
                      permitObject = false,
                      permitUserWeb = false,
                      notes = "This is called by Inventory.InteractItem usually, you would start most user interactions from Inventory.Interact command")
    public static Response destroyItem(@Nonnull final State state,
                                       @Argument.Arguments(type = Argument.ArgumentType.INVENTORY,
                                                           description = "Inventory to interact with",
                                                           name = "inventory") @Nonnull final Attribute inventory,
                                       @Argument.Arguments(name = "item",
                                                           description = "Item to interact with",
                                                           type = Argument.ArgumentType.ITEM) @Nonnull final Item item,
                                       @Argument.Arguments(name="quantity",
                                                           type = Argument.ArgumentType.INTEGER,
                                                           description = "Number to destroy from inventory, -1 means ALL") int quantity) {
        final Inventory inv = new Inventory(state.getCharacter(), inventory);
        if (!inv.accessible(state)) {
            return new ErrorResponse("Inventory " + inventory.getName() + " is not accessible from this location!");
        }
        if (!item.destroyable()) {
            return new ErrorResponse("Item "+item.getName()+" can not be destroyed!");
        }
        final int count = inv.count(item);
        if (quantity==-1) { quantity=count; }
        if (quantity<1) { return new ErrorResponse("You must destroy at least one item to do anything"); }
        if (count < 1) {
            return new ErrorResponse("Your inventory " + inventory.getName() + " does not contain any " + item.getName());
        }
        if (quantity>count) {
            return new ErrorResponse("You can not destroy "+quantity+" x "+item.getName()+" from "+inventory.getName()+" because it only contains "+count);
        }
        try {
            final int newAmount = inv.add(item, -quantity, true);
            Audit.audit(false, state, Audit.OPERATOR.CHARACTER, null, null, "Destroy", item.getName(), String.valueOf(count), String.valueOf(newAmount), "Destroyed " + quantity + " " + item.getName() + " from " + inventory.getName());
            return new OKResponse("Destroyed "+quantity+" x "+item.getName()+" from "+inventory.getName()+", "+newAmount+" remain in this inventory");
        } catch (final UserInventoryException e) {
            return new ErrorResponse(e.getLocalizedMessage());
        }
    }

    @Command.Commands(description = "Gives an item from an inventory",
                      permitScripting = false,
                      permitExternal = false,
                      context = Command.Context.CHARACTER,
                      permitObject = false,
                      permitUserWeb = false,
                      notes = "This is called by Inventory.InteractItem usually, you would start most user interactions from Inventory.Interact command")
    public static Response giveItem(@Nonnull final State state,
                                    @Argument.Arguments(type = Argument.ArgumentType.INVENTORY,
                                                           description = "Inventory to interact with",
                                                           name = "inventory") @Nonnull final Attribute inventory,
                                    @Argument.Arguments(name = "item",
                                                           description = "Item to interact with",
                                                           type = Argument.ArgumentType.ITEM) @Nonnull final Item item,
                                    @Argument.Arguments(name="target",
                                                           description="Target to give items to",
                                                           type= Argument.ArgumentType.CHARACTER_NEAR) @Nonnull final Char target,
                                    @Argument.Arguments(name="quantity",
                                                           type = Argument.ArgumentType.INTEGER,
                                                           description = "Number to give from inventory, -1 means ALL") int quantity) {
        final Inventory inv = new Inventory(state.getCharacter(), inventory);
        final Inventory targetInv = new Inventory(target, inventory);
        if (!inv.accessible(state)) {
            return new ErrorResponse("Inventory " + inventory.getName() + " is not accessible from this location!");
        }
        if (!item.tradable()) {
            return new ErrorResponse("Item " + item.getName() + " can not be traded!");
        }
        final int count = inv.count(item);
        if (quantity == -1) {
            quantity = count;
        }
        if (quantity < 1) {
            return new ErrorResponse("You must give at least one item to do anything");
        }
        if (state.getCharacter() == target) {
            // give to self is a bad idea since it makes the audit logs report stupid incorrect counts
            return new ErrorResponse("You may not give items to yourself, silly!");
        }
        if (count < 1) {
            return new ErrorResponse("Your inventory " + inventory.getName() + " does not contain any " + item.getName());
        }
        if (quantity>count) {
            return new ErrorResponse("You can not give "+quantity+" x "+item.getName()+" from "+inventory.getName()+" because it only contains "+count);
        }
        try {
            final int targetOldAmount = targetInv.count(item);
            final int targetNewAmount = targetInv.add(item, quantity, true);
            final int newAmount = inv.add(item, -quantity, true);
            Audit.audit(false, state, null, target, state.getAvatar(), state.getCharacter(), "Recieve", item.getName(), String.valueOf(targetOldAmount), String.valueOf(targetNewAmount), "Received " + quantity + " " + item.getName() + " from " + state.getCharacter().getName() + "'s " + inventory.getName() + ", we had " + targetOldAmount + " and now have " + targetNewAmount);
            Audit.audit(false, state, Audit.OPERATOR.CHARACTER, null, target, "Give", item.getName(), String.valueOf(count), String.valueOf(newAmount), "Gave " + quantity + " " + item.getName() + " from " + inventory.getName() + " to " + target.getName());
            final JSONResponse push = new JSONResponse(new JSONObject());
            push.message("You received " + quantity + " x " + item.getName() + " from " + state.getCharacter().getName() + " to " + inventory.getName(), target.getProtocol());
            target.push(push);
            return new OKResponse("Gave " + quantity + " x " + item.getName() + " from " + inventory.getName() + " to " + target.getName() + ", " + newAmount + " remain in this inventory");
        } catch (final UserInventoryException e) {
            return new ErrorResponse(e.getLocalizedMessage());
        }
    }
    @Command.Commands(description = "Move an item from one inventory to another",
                      permitScripting = false,
                      permitExternal = false,
                      context = Command.Context.CHARACTER,
                      permitObject = false,
                      permitUserWeb = false,
                      notes = "This is called by Inventory.InteractItem usually, you would start most user interactions from Inventory.Interact command")
    public static Response moveItem(@Nonnull final State state,
                                    @Argument.Arguments(type = Argument.ArgumentType.INVENTORY,
                                                        description = "Inventory to move from",
                                                        name = "inventory") @Nonnull final Attribute inventory,
                                    @Argument.Arguments(name = "item",
                                                        description = "Item to move",
                                                        type = Argument.ArgumentType.ITEM) @Nonnull final Item item,
                                    @Argument.Arguments(name="target",
                                                        description="Target inventory to move items to",
                                                        type= Argument.ArgumentType.INVENTORY) @Nonnull final Attribute target,
                                    @Argument.Arguments(name="quantity",
                                                        type = Argument.ArgumentType.INTEGER,
                                                        description = "Number to move from inventory, -1 means ALL") int quantity) {
        final Inventory inv = new Inventory(state.getCharacter(), inventory);
        final Inventory targetInv = new Inventory(state.getCharacter(), target);
        if (!inv.accessible(state)) {
            return new ErrorResponse("Inventory " + inv.getName() + " is not accessible from this location!");
        }
        if (!targetInv.accessible(state)) {
            return new ErrorResponse("Inventory " + targetInv.getName() + " is not accessible from this location!");
        }
        final int count = inv.count(item);
        if (quantity == -1) {
            quantity = count;
        }
        if (quantity < 1) {
            return new ErrorResponse("You must move at least one item to do anything");
        }
        if (count < 1) {
            return new ErrorResponse("Your inventory " + inventory.getName() + " does not contain any " + item.getName());
        }
        if (quantity>count) {
            return new ErrorResponse("You can not move "+quantity+" x "+item.getName()+" from "+inventory.getName()+" because it only contains "+count);
        }
        if (!targetInv.allows(state,item)) {
            return new ErrorResponse("You can not move "+item.getName()+" to "+targetInv.getName()+" because it can not contain this item");
        }
        try {
            final int targetOldAmount = targetInv.count(item);
            final int targetNewAmount = targetInv.add(item, quantity, true);
            final int newAmount = inv.add(item, -quantity, false);
            Audit.audit(false, state, Audit.OPERATOR.CHARACTER, null, null, "Move", item.getName(), String.valueOf(targetOldAmount), String.valueOf(targetNewAmount), "Moved " + quantity + " " + item.getName() + " from " + inv.getName() + " to " + targetInv.getName());
            final JSONResponse push = new JSONResponse(new JSONObject());
            return new OKResponse("Moved " + quantity + " x " + item.getName() + " from " + inv.getName() + " to " + targetInv.getName() + ", " + newAmount + " remain in this inventory and " + targetNewAmount + " in the target inventory");
        } catch (final UserInventoryException e) {
            return new ErrorResponse(e.getLocalizedMessage());
        }
    }
}