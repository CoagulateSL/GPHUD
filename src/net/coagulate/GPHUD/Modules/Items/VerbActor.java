package net.coagulate.GPHUD.Modules.Items;

import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class VerbActor {
    public static String decode(final ItemVerb verb) {
        String response = "";
        final JSONObject payload = verb.payload();
        if (payload.has("consumesitem")) {
            response += "[Consumes Item] ";
        }
        if (payload.optString("action").equalsIgnoreCase("script")) {
            response += "[Script:" + payload.optString("script", "") + "] ";
        }
        if (payload.optString("action").equalsIgnoreCase("command")) {
            response += "[Command:" + payload.optString("command", "") + "] ";
        }
        return response;
    }

    public static Response act(@Nonnull final State state,
                               @Nonnull final Attribute attribute,
                               @Nonnull final Item item,
                               @Nonnull final ItemVerb itemVerb) {
        final Inventory inventory = new Inventory(state.getCharacter(), attribute);
        final JSONObject payload = itemVerb.payload();
        final int valueBefore = inventory.count(item);
        final int valueAfter;
        if (payload.has("consumesitem")) {
            valueAfter = inventory.add(item, -1, false);
        } else {
            valueAfter = valueBefore;
        }
        Audit.audit(state, Audit.OPERATOR.CHARACTER, null, null, "Use:" + itemVerb.getName(), item.getName(), "" + valueBefore, "" + valueAfter, "Character uses item from " + inventory.getName());
        final String action = payload.optString("action", "");
        if (action.equalsIgnoreCase("command")) {
            return Modules.getJSONTemplateResponse(state, payload.optString("command", ""));
        }
        if (action.equalsIgnoreCase("script")) {
            final GSVM vm = new GSVM(Script.find(state, payload.optString("script", "")));
            vm.introduce("ITEMNAME", new BCString(null, item.getName()));
            vm.introduce("ITEMVERB",new BCString(null,itemVerb.getName()));
            vm.introduce("INVENTORY",new BCString(null,inventory.getName()));
            return vm.execute(state);
        }
        return new OKResponse(itemVerb.getName()+" performed on "+item.getName());
    }
}
