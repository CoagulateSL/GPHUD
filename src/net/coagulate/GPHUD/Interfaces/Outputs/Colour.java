package net.coagulate.GPHUD.Interfaces.Outputs;

import java.util.HashSet;
import java.util.Set;
import net.coagulate.GPHUD.State;

/**
 *
 * @author iain
 */
public class Colour implements Renderable {
    Renderable content;
    String colour;
    public Colour(String colour,Renderable content) { this.content=content; this.colour=colour; }
    public Colour(String colour,String content) { this.content=new Text(content); this.colour=colour; }

    @Override
    public String asText(State st) {
        return content.asText(st);
    }

    @Override
    public String asHtml(State st, boolean rich) {
        return "<font color=\""+colour+"\">"+content.asHtml(st, rich)+"</font>";
    }

    @Override
    public Set<Renderable> getSubRenderables() {
        Set<Renderable> r=new HashSet<>(); r.add(content); return r;
    }
    
    
}
