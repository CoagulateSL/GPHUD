package net.coagulate.GPHUD.Modules.Items;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Inventory;
import net.coagulate.GPHUD.Data.Item;
import net.coagulate.GPHUD.Data.ItemVerb;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class VerbActor {
    public static String decode(ItemVerb verb) {
        return "NOT IMPLEMENTED";
    }

    public static Response act(@Nonnull final State state,
                               @Nonnull final Attribute attribute,
                               @Nonnull final Item item,
                               @Nonnull final ItemVerb itemVerb) {
        Inventory inventory=new Inventory(state.getCharacter(),attribute);
        return new ErrorResponse("NOT IMPLEMENTED");
    }
}
