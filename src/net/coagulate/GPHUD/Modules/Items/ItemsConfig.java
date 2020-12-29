package net.coagulate.GPHUD.Modules.Items;

import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Item;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class ItemsConfig {

    @URL.URLs(url="/configuration/items")
    public static void configPage(@Nonnull final State st,
                                  @Nonnull final SafeMap values) {
        final Form f=st.form();
        f.add(new TextHeader("Defined Items"));
        f.noForm();
        f.add(Item.getSummaryPage(st.getInstance()));
        if (st.hasPermission("Items.Edit")) {
            f.add(new Form(st,true,"/GPHUD/configuration/items/edititem","Create New Item",
                    "tradable","true",
                    "destroyable","true",
                    "weight","0"));
        }
        //f.add(new Separator());
        //Modules.get(st,"Items").kvConfigPage(st);
    }

    @URL.URLs(url="/configuration/items/edititem",
              requiresPermission="Items.Edit")
    public static void addAttribute(@Nonnull final State st,
                                    @Nonnull final SafeMap values) {
        Modules.simpleHtml(st,"Items.Edit",values);
    }

    @Nonnull
    @Command.Commands(description="Create or modify an item (based on name)",
                      context= Command.Context.AVATAR,
                      requiresPermission="Items.Edit",
                      permitObject=false)
    public static Response edit(@Nonnull final State st,
                                @Nonnull @Arguments(name="name",
                                                    description="Name of item",
                                                    type= Argument.ArgumentType.TEXT_ONELINE,
                                                    max=128) final String name,
                                @Nonnull @Arguments(name="description",
                                                    description="Description of item",
                                                    type= Argument.ArgumentType.TEXT_ONELINE,
                                                    max=256) final String description,
                                @Nonnull @Arguments(name="weight",
                                                    description="Weight of item (0 for no weight)",
                                                    type= Argument.ArgumentType.INTEGER) final Integer weight,
                                @Nonnull @Arguments(name="tradable",
                                                    description="Players can trade item",
                                                    type= Argument.ArgumentType.BOOLEAN) final Boolean tradable,
                                @Nonnull @Arguments(name="destroyable",
                                                    description="Players can destroy item from inventory",
                                                    type= Argument.ArgumentType.BOOLEAN) final Boolean destroyable) {
        Item item=Item.findOrCreate(st,name);
        if (!item.description().equals(description)) {
            Audit.audit(false,st, Audit.OPERATOR.AVATAR,null,null,"Item",
                    "Description",item.description(),description,"Changed item '"+item.getName()+"' description");
            item.description(description);
        }
        if (!(item.weight()==weight)) {
            Audit.audit(false,st, Audit.OPERATOR.AVATAR,null,null,"Item",
                    "Weight",""+item.weight(),""+weight,"Changed item '"+item.getName()+"' weight");
            item.weight(weight);
        }
        if (!(item.tradable()==tradable)) {
            Audit.audit(false,st, Audit.OPERATOR.AVATAR,null,null,"Item",
                    "Tradable",""+item.tradable(),""+tradable,"Changed item '"+item.getName()+"' tradarable flag");
            item.tradable(tradable);
        }
        if (!(item.destroyable()==destroyable)) {
            Audit.audit(false,st, Audit.OPERATOR.AVATAR,null,null,"Item",
                    "Destroyable",""+item.destroyable(),""+destroyable,"Changed item '"+item.getName()+"' destoyable flag");
            item.destroyable(destroyable);
        }
        return new OKResponse("Item created/updated");
    }

    @URL.URLs(url="/configuration/items/*")
    public static void itemPage(@Nonnull final State st,
                                  @Nonnull final SafeMap values) {
        final Form f = st.form();
        String id=st.getDebasedURL().substring("/configuration/items/".length());
        Item item=Item.get(Integer.parseInt(id));
        if (item.getInstance()!=st.getInstance()) { throw new UserInputStateException("Item "+id+" is from a different instance"); }
        f.add(new TextHeader("Item: "+item.getName()));
        f.noForm();
    }
}
