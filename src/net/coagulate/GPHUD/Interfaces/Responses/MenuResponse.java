package net.coagulate.GPHUD.Interfaces.Responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import org.json.JSONObject;

/** Provide a series of elements in a Menu like layout...
 *
 * @author iain
 */
public class MenuResponse implements Response {

    private String header=null;
    private List<Renderable> menu=new ArrayList<>();
    
    public void add(Renderable r) { menu.add(r); }
    public MenuResponse() {}
    public MenuResponse(String title) { header=title; }
    
    @Override
    public JSONObject asJSON(State st) {
        throw new SystemException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String asText(State st) {
        throw new SystemException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String asHtml(State st, boolean rich) {
        String s="";
        if (header!=null && !header.isEmpty()) { s+=new TextHeader(header).asHtml(st, rich); }
        for (Renderable r:menu) {
            if (!s.isEmpty()) {
                if (rich) { s+="<br>"; } else { s+=" | "; }
            }
            s+=r.asHtml(st, rich);
        }
        return s;            
    }

    @Override
    public Set<Renderable> getSubRenderables() {
        throw new SystemException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
