package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Manipulation {

    private static Inventory preCheck(final State st, final Attribute inventory, final Char character, final boolean checkAdmin) {
        // Attribute must be an inventory
        if (!(inventory.getType() == Attribute.ATTRIBUTETYPE.INVENTORY)) {
            throw new UserInputStateException("Attribute " + inventory.getName() + " is of type " + inventory.getType() + " not INVENTORY");
        }
        // Attribute must belong to instance (!)
        if (!(inventory.getInstance() == st.getInstance())) {
            throw new SystemImplementationException("Attribute " + inventory + " is not from instance " + st.getInstanceString());
        }
        // check permission
        if (checkAdmin && !st.hasPermission("Characters.Set" + inventory.getName())) {
            throw new UserAccessDeniedException("You do not have permission Characters.Set" + inventory.getName() + " required to modify this characters inventory");
        }
        // check character is of right instance
        if (!(st.getInstance() == character.getInstance())) {
            throw new SystemImplementationException("Target character " + character + " is not from instance " + st.getInstanceString());
        }
        return new Inventory(character,inventory);
    }
    @URL.URLs(url = "/configuration/inventory/add")
    public static void addForm(@Nonnull final State st,
                               @Nonnull final SafeMap values) {
        Modules.simpleHtml(st, "Inventory.Add", values);
    }
    @Nonnull
    @Command.Commands(context = Command.Context.AVATAR,
                      description = "Add an item to an inventory")
    public static Response add(@Nonnull final State st,
                               @Nonnull @Argument.Arguments(type = Argument.ArgumentType.CHARACTER,
                                                            description = "Character to modify",
                                                            name = "character") final Char character,
                               @Nonnull @Argument.Arguments(type = Argument.ArgumentType.INVENTORY,
                                                            description = "Inventory to add to",
                                                            name = "inventory") final Attribute inventory,
                               @Nonnull @Argument.Arguments(description = "Item to add to inventory",
                                                            type = Argument.ArgumentType.ITEM,
                                                            name = "item") final Item item,
                               @Nullable @Argument.Arguments(name = "qty",
                                                             type = Argument.ArgumentType.INTEGER,
                                                             description = "Number of item to add (or remove, if negative)",
                                                             mandatory = false) Integer qty) {
        final Inventory inv = preCheck(st, inventory, character, true);
        if (qty == null) {
            qty = 1;
        }
        // guess we're ok then (!)
        final int oldValue = inv.count(item);
        try {
            final int total = inv.add(item, qty, false);
            Audit.audit(true, st, Audit.OPERATOR.AVATAR, null, character, "Add", inventory.getName(), "" + oldValue, "" + total, "Added " + qty + " " + item + " to inventory, totalling " + total);
            return new OKResponse("Added " + qty + " " + item + " to " + character + "'s " + inventory + ", changing total from " + oldValue + " to " + total);
        } catch (final UserInventoryException error) {
            return new ErrorResponse(error.getLocalizedMessage());
        }
    }

    @URL.URLs(url = "/configuration/inventory/set")
    public static void setForm(@Nonnull final State st,
                               @Nonnull final SafeMap values) {
        Modules.simpleHtml(st, "Inventory.Set", values);
    }

    @Nonnull
    @Command.Commands(context = Command.Context.AVATAR,
                      description = "Set the number of an item in an inventory")
    public static Response set(@Nonnull final State st,
                               @Nonnull @Argument.Arguments(type = Argument.ArgumentType.CHARACTER,
                                                            description = "Character to modify",
                                                            name = "character") final Char character,
                               @Nonnull @Argument.Arguments(type = Argument.ArgumentType.INVENTORY,
                                                            description = "Inventory to add to",
                                                            name = "inventory") final Attribute inventory,
                               @Nonnull @Argument.Arguments(description = "Item to add to inventory",
                                                            type = Argument.ArgumentType.ITEM,
                                                            name = "item") final Item item,
                               @Nullable @Argument.Arguments(name = "qty",
                                                             type = Argument.ArgumentType.INTEGER,
                                                             description = "Number of item to add (or remove, if negative)",
                                                             mandatory = false) Integer qty) {
        final Inventory inv = preCheck(st, inventory, character, true);
        if (qty == null) {
            qty = 1;
        }
        // guess we're ok then (!)
        final int oldValue = inv.count(item);
        final int total = inv.set(item, qty, false);
        Audit.audit(true, st, Audit.OPERATOR.AVATAR, null, character, "Set", inventory.getName(), "" + oldValue, "" + total, "Set " + total + " x " + item.getName());
        return new OKResponse("Set " + total + " " + item.getName() + " in " + character + "'s " + inventory.getName() + " (was " + oldValue + ")");
    }

    @Nonnull
    @Command.Commands(context = Command.Context.CHARACTER,
                      description = "Reports on the contents of an inventory")
    public static Response view(@Nonnull final State st,
                                @Nonnull @Argument.Arguments(type = Argument.ArgumentType.INVENTORY,
                                                             description = "Inventory to view",
                                                             name = "inventory") final Attribute inventory) {
        preCheck(st, inventory, st.getCharacter(), false);
        return new OKResponse(inventory.getName() + " contains: " + new Inventory(st.getCharacter(), inventory).textList());
    }
}
