package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Inventory;
import net.coagulate.GPHUD.Data.Item;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
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
        Inventory inv=new Inventory(state.getCharacter(),inventory);
        if (!inv.accessible(state)) { return new ErrorResponse("Inventory "+inventory.getName()+" is not accessible from this location!"); }
        JSONObject json=new JSONObject();
        json.put("inventory",inventory.getName());
        json.put("arg0name","item");
        json.put("arg0description","Pick an item to interact with");
        json.put("arg0type","SELECT");
        int button=0;
        for (String item:inv.elements().keySet()) {
            json.put("arg0button"+button,item);
            button++;
        }
        json.put("args",1);
        json.put("invoke","Inventory.InteractItem");
        json.put("incommand","runtemplate");
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
                                        @Argument.Arguments(name="item",
                                                            description = "Item to interact with",
                                                            type = Argument.ArgumentType.ITEM) @Nonnull final Item item) {
        Inventory inv=new Inventory(state.getCharacter(),inventory);
        if (!inv.accessible(state)) { return new ErrorResponse("Inventory "+inventory.getName()+" is not accessible from this location!"); }
        int count=inv.count(item);
        if (count<1) {
            return new ErrorResponse("Your inventory "+inventory.getName()+" does not contain any "+item.getName());
        }
        JSONObject json=new JSONObject();
        json.put("inventory",inventory.getName());
        json.put("item",item.getName());
        json.put("arg0name","verb");
        json.put("arg0description","Pick an action\n"+item.getName()+"\n(Qty: "+count+")");
        json.put("arg0type","SELECT");
        int button=0;
        if (item.destroyable()) {
            json.put("arg0button" + button, "Destroy");
            button++;
        }
        if (item.tradable()) {
            json.put("arg0button" + button, "Give To");
            button++;
        }
        //Verbs
        /*for (String item2:inv.elements().keySet()) {
            json.put("arg0button"+button,item2);
            button++;
        }*/
        json.put("args",1);
        json.put("invoke","Inventory.InteractItemVerb");
        json.put("incommand","runtemplate");
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
                                            @Argument.Arguments(name="item",
                                                                description = "Item to interact with",
                                                                type = Argument.ArgumentType.ITEM) @Nonnull final Item item,
                                            @Argument.Arguments(name="verb",
                                                                description = "Interaction verb to use",
                                                                type = Argument.ArgumentType.TEXT_ONELINE,
                                                                max=64) @Nonnull final String verb) {
        Inventory inv = new Inventory(state.getCharacter(), inventory);
        if (!inv.accessible(state)) {
            return new ErrorResponse("Inventory " + inventory.getName() + " is not accessible from this location!");
        }
        int count = inv.count(item);
        if (count < 1) {
            return new ErrorResponse("Your inventory " + inventory.getName() + " does not contain any " + item.getName());
        }
        JSONObject json = new JSONObject();
        json.put("incommand","runtemplate");
        json.put("inventory",inventory.getName());
        json.put("item",item.getName());
        if (verb.equalsIgnoreCase("Destroy")) {
            json.put("invoke","Inventory.DestroyItem");
            json.put("arg0name","quantity");
            json.put("arg0description","Ammount of "+item.getName()+" to destroy from "+inventory.getName()+"\n(You have "+count+")\nUse -1 to destroy ALL");
            json.put("arg0type","TEXTBOX");
            json.put("args",1);
            return new JSONResponse(json);
        }
        if (verb.equalsIgnoreCase("Give To")) {

        }
        return new ErrorResponse("Unknown interaction '"+verb+"' on item "+item.getName());
    }
}
