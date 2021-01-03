package net.coagulate.GPHUD.Modules.Items;

import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
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
                      permitObject=false,
                      permitExternal = false,
                      permitScripting = false)
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
        f.add(new TextSubHeader("Properties"));
        f.noForm();
        Table propertiesTable=new Table();
        f.add(propertiesTable);
        propertiesTable.openRow().add(new Cell("Name")).add(item.getName());
        propertiesTable.openRow().add(new Cell("Description")).add(item.description());
        propertiesTable.openRow().add("Weight").add(item.weight());
        propertiesTable.openRow().add("Tradable").add(item.tradable());
        propertiesTable.openRow().add("Destroyable").add(item.destroyable());
        if (st.hasPermission("Items.Edit")) {
            f.add(new Form(st,true,"/GPHUD/configuration/items/edititem","Edit Item",
                    "name",item.getName(),
                    "tradable",item.tradable()?"true":"",
                    "destroyable",item.destroyable()?"true":"",
                    "weight",""+item.weight(),
                    "description",item.description()));
        }
        f.add(new Separator());
        f.add(new TextSubHeader("Allowed In Inventories"));
        Table inventories=new Table();
        f.add(inventories);
        for (Attribute inventory: Inventory.getAll(st.getInstance())) {
            inventories.openRow();
            inventories.add(inventory.getName());
            boolean allowed=Inventory.allows(st,inventory,item);
            if (allowed) { inventories.add("Permitted"); }
            else { inventories.add("Forbidden"); }
            if (st.hasPermission("Items.EditInventories")) {
                inventories.add(new Form(st,true,"/GPHUD/configuration/items/inventoryallow","Toggle",
                        "item",item.getName(),
                                "inventory",inventory.getName(),
                                "allowed",allowed?"":"true"));
            }
        }
        f.add(new Separator());
        f.add(new TextSubHeader("Available Actions"));
        if (!values.get("newaction").isBlank()) {
            String newAction=values.get("newaction");
            if (newAction.equalsIgnoreCase("Give To") ||
                newAction.equalsIgnoreCase("Destroy") ||
                newAction.equalsIgnoreCase("Move To")) {
                f.add(new TextError("Can not create new action "+newAction+", this verb is reserved"));
            } else {
                try { ItemVerb.create(item,newAction); }
                catch (UserException e) { f.add(new TextError(e.getLocalizedMessage())); }
            }
        }
        Table verbs=new Table();
        f.add(verbs);
        for (ItemVerb verb:ItemVerb.findAll(item)) {
            verbs.openRow();
            verbs.add(new Link(verb.getName(),"/GPHUD/configuration/items/verbs/"+verb.getId()));
            verbs.add(verb.description());
            verbs.add(VerbActor.decode(verb));
        }
        Form newAction=new Form();
        newAction.add(new TextInput("newaction")).add(new Button("Create"));
        f.add(newAction);
        f.add(new Separator());
    }

    @URL.URLs(url="/configuration/items/inventoryallow",
              requiresPermission="Items.EditInventories")
    public static void editInventoriesPage(@Nonnull final State st,
                                    @Nonnull final SafeMap values) {
        Modules.simpleHtml(st,"Items.EditInventories",values);
    }

    @Nonnull
    @Command.Commands(description="Alter an items allowed state in an inventory",
                      context= Command.Context.AVATAR,
                      requiresPermission="Items.EditInventories",
                      permitObject=false,
                      permitExternal = false,
                      permitScripting = false)
    public static Response editInventories(@Nonnull final State st,
                                @Nonnull @Arguments(name="item",
                                                    description="Item",
                                                    type= Argument.ArgumentType.ITEM) final Item item,
                                @Nonnull @Arguments(name="inventory",
                                                    description="Inventory",
                                                    type= Argument.ArgumentType.INVENTORY) final Attribute inventory,
                                @Nonnull @Arguments(name="allowed",
                                                    description="Item allowed",
                                                    type= Argument.ArgumentType.BOOLEAN) final Boolean allowed) {
        Inventory.allows(st,inventory,item,allowed);
        return new OKResponse("Item inventory allowed status updated");
    }

}
