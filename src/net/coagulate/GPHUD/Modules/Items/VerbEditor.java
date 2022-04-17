package net.coagulate.GPHUD.Modules.Items;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Item;
import net.coagulate.GPHUD.Data.ItemVerb;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.CheckBox;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class VerbEditor {
    @URL.URLs(url = "/configuration/items/verbs/*")
    public static void itemPage(@Nonnull final State st,
                                @Nonnull final SafeMap values) {
        boolean permitted = false;
        if (st.hasPermission("Items.EditVerbs")) {
            permitted = true;
        }
        final Form f = st.form();
        final String id = st.getDebasedURL().substring("/configuration/items/verbs/".length());
        final ItemVerb itemVerb = ItemVerb.get(Integer.parseInt(id));
        if (itemVerb.getInstance() != st.getInstance()) {
            throw new UserInputStateException("ItemVerb " + id + " is from a different instance");
        }
        if (permitted && !values.get("Submit").isBlank()) {
            final JSONObject payload = new JSONObject();
            payload.put("action", values.get("actiontype"));
            payload.put("command", values.get("command"));
            payload.put("script", values.get("script"));
            if (!values.get("consumesitem").isBlank()) {
                payload.put("consumesitem", "true");
            }
            itemVerb.payload(payload);
        }
        final Item item = itemVerb.getItem();
        final JSONObject payload = itemVerb.payload();
        f.add(new Link("&lt;- Back to Items list", "/GPHUD/configuration/items"));
        f.add(new Text("<br>"));
        f.add(new Link("&lt;- Back to Item " + item.getName(), "/GPHUD/configuration/items/" + item.getId()));
        f.add(new Text("<br>"));
        f.add(new TextHeader("Item: " + item.getName() + "<br>Verb: " + itemVerb.getName()));
        //f.noForm();
        final Table table = new Table();
        f.add(table);
        table.border(false);
        table.add(new Cell("Description").th()).add(item.description());
        table.openRow().add(new Cell("Action Type").th());
        table.add(selector(itemVerb.payload().optString("action",payload.optString("action",""))));
        table.openRow("commandrow").add("Selected Command").add(DropDownList.getCommandsList(st,"command").setValue(payload.optString("command","")));
        table.openRow("scriptrow").add("Selected Script").add(DropDownList.getScriptsList(st,"script").setValue(payload.optString("script","")));
        f.add("<script> " +
                "function update() { var action=document.getElementById(\"actiontype\").value; " +
                "document.getElementById(\"scriptrow\").hidden=!(action==\"script\"); " +
                "document.getElementById(\"commandrow\").hidden=!(action==\"command\"); " +
                "} update(); </script>");
        table.openRow().add("Consumes Item").add(new CheckBox("consumesitem").setValue(payload.optString("consumesitem", "")));
        if (permitted) {
            table.openRow().add(new Cell(new Button("Submit"), 2));
        } else {
            table.openRow().add(new Cell("You do not have permission to edit this item action", 2));
        }
    }

    private static DropDownList selector(final String value) {
        final DropDownList ret = new DropDownList("actiontype");
        ret.add("script", "Invoke Script");
        ret.add("command", "Invoke Command");
        ret.setValue(value);
        ret.id("actiontype");
        ret.javascriptOnChange("update();");
        return ret;
    }


    @URL.URLs(url = "/configuration/items/deleteverb",
              requiresPermission="Items.DeleteVerb")
    public static void deleteForm(@Nonnull final State st,
                                    @Nonnull final SafeMap values) {
        Modules.simpleHtml(st,"Items.DeleteVerb",values);
    }

    @Nonnull
    @Command.Commands(description="Delete an item action",
                      context= Command.Context.AVATAR,
                      requiresPermission="Items.DeleteVerb",
                      permitObject=false,
                      permitExternal = false,
                      permitScripting = false)
    public static Response deleteVerb(@Nonnull final State st,
                                  @Nonnull @Argument.Arguments(name="item",
                                                               description="Item to delete action from",
                                                               type= Argument.ArgumentType.ITEM,
                                                               max=128) final Item item,
                                      @Nonnull @Argument.Arguments(name="verb",
                                                                   description="Action to delete",
                                                                   type= Argument.ArgumentType.TEXT_ONELINE,
                                                                   max=128) final String verb) {
        if (item.getInstance()!=st.getInstance()) { throw new SystemImplementationException("Item instance / state instance mismatch"); }
        final ItemVerb itemVerb = ItemVerb.findNullable(item, verb);
        if (itemVerb==null) { return new ErrorResponse("There is no verb named '"+verb+"' for item "+item.getName()); }
        itemVerb.delete();
        Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"Delete","Verb",verb,null,"Deleted action "+verb+" from item "+item.getName());
        return new OKResponse("Item "+item.getName()+" action "+verb+" deleted");
    }

}
