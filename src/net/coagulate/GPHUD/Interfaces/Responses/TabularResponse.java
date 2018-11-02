package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**  Default implementation of a Response, the OK Response.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TabularResponse extends Table implements Response {
    String title=null;
    public TabularResponse(){}
    public TabularResponse(String message) { title=message; }

    @Override
    public JSONObject asJSON(State st) {
        JSONObject j=new JSONObject();
        j.put("message",asText(st));
        return j;
    }
    
    public String asText(State st) {
        String s="";
        if (title!=null && !title.isEmpty()) { s+=new TextHeader(title).asText(st); }
        s+=super.asText(st);
        return s;
    }
    @Override
    public String asHtml(State st,boolean rich) {
        String s="";
        if (title!=null && !title.isEmpty()) { s+=new TextHeader(title).asHtml(st,rich); }
        s+=super.asHtml(st,rich);
        return s;
    }
}
