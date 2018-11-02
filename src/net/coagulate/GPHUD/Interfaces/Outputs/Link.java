package net.coagulate.GPHUD.Interfaces.Outputs;

import java.util.HashSet;
import java.util.Set;
import net.coagulate.GPHUD.State;

/** Implements a header element.
 * Hmm, this is bad, the content should just be an element, not forced text.  
 * @author Iain Price <gphud@predestined.net>
 */
public class Link implements Renderable {
    Renderable content;
    String target;
    public Link(String label,String target) { content=new Text(label); this.target=target; }
    public Link(Renderable label,String target) { content=label; this.target=target; }

    @Override
    public String asText(State st) {
        return ">"+content.asText(st)+"<";
    }

    @Override
    public String asHtml(State st,boolean rich) {
        return "<a href=\""+target+"\">"+content.asHtml(st,rich)+"</a>";
    }

    @Override
    public Set<Renderable> getSubRenderables() {
        Set<Renderable> r=new HashSet<>();
        r.add(content);
        return r;
    }
}
