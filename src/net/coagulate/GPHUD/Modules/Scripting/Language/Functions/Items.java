package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.Item;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Items {
    @Nonnull
    @GSFunctions.GSFunction(description="Returns the description for an item",
                            returns="String - The item's description, or the empty string if not found",
                            parameters="String itemName - The name of the item to query",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.ITEMS)
    public static BCString gsItemDescription(@Nonnull final State st,
                                     @Nonnull final GSVM vm,
                                     @Nonnull final BCString itemName) {
        final Item item = Item.findNullable(st.getInstance(), itemName.getContent());
        return new BCString(null,item==null?"":item.description());
    }

    @Nonnull
    @GSFunctions.GSFunction(description="Returns the weight for an item",
                            returns="String - The item's weight, or -1 if not found",
                            parameters="String itemName - The name of the item to query",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.ITEMS)
    public static BCInteger gsItemWeight(@Nonnull final State st,
                                             @Nonnull final GSVM vm,
                                             @Nonnull final BCString itemName) {
        final Item item = Item.findNullable(st.getInstance(), itemName.getContent());
        return new BCInteger(null,item==null?-1:item.weight());
    }
}
