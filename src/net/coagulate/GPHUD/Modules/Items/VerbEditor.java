package net.coagulate.GPHUD.Modules.Items;

import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Item;
import net.coagulate.GPHUD.Data.ItemVerb;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.CheckBox;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class VerbEditor {
    @URL.URLs(url = "/configuration/items/verbs/*")
    public static void itemPage(@Nonnull final State st,
                                @Nonnull final SafeMap values) {
        boolean permitted=false;
        if (st.hasPermission("Items.EditVerbs")) { permitted=true; }
        final Form f = st.form();
        String id = st.getDebasedURL().substring("/configuration/items/verbs/".length());
        ItemVerb itemVerb = ItemVerb.get(Integer.parseInt(id));
        if (itemVerb.getInstance() != st.getInstance()) {
            throw new UserInputStateException("ItemVerb " + id + " is from a different instance");
        }
        if (permitted && !values.get("Submit").isBlank()) {
            JSONObject payload=new JSONObject();
            payload.put("action",values.get("actiontype"));
            payload.put("command",values.get("command"));
            payload.put("script",values.get("script"));
            if (!values.get("consumesitem").isBlank()) {
                payload.put("consumesitem","true");
            }
            itemVerb.payload(payload);
        }
        Item item=itemVerb.getItem();
        JSONObject payload=itemVerb.payload();
        f.add(new Link("&lt;- Back to Items list","/GPHUD/configuration/items"));
        f.add(new Text("<br>"));
        f.add(new Link("&lt;- Back to Item "+item.getName(),"/GPHUD/configuration/items/"+item.getId()));
        f.add(new Text("<br>"));
        f.add(new TextHeader("Item: " + item.getName()+"<br>Verb: "+itemVerb.getName()));
        //f.noForm();
        Table table=new Table();
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
        table.openRow().add("Consumes Item").add(new CheckBox("consumesitem").setValue(payload.optString("consumesitem","")));
        if (permitted) { table.openRow().add(new Cell(new Button("Submit"),2)); }
        else { table.openRow().add(new Cell("You do not have permission to edit this item action",2)); }
    }

    private static DropDownList selector(String value) {
        DropDownList ret=new DropDownList("actiontype");
        ret.add("script","Invoke Script");
        ret.add("command","Invoke Command");
        ret.setValue(value);
        ret.id("actiontype");
        ret.javascriptOnChange("update();");
        return ret;
    }
}
