package net.coagulate.GPHUD.Interfaces.Outputs;

import java.util.HashSet;
import java.util.Set;
import net.coagulate.GPHUD.State;

/** Paragraph.
 * @author Iain Price <gphud@predestined.net>
 */
public class Paragraph implements Renderable {

    Renderable content;
    public Paragraph(String s) { content=new Text(s); }
    public Paragraph(Renderable r) { content=r; }

    public Paragraph() {
        content=new Text("");
    }

    @Override
    public String asText(State st) {
        return content.asText(st)+"\n";
    }

    @Override
    public String asHtml(State st,boolean rich) {
        return "<p>"+content.asHtml(st,rich)+"</p>";
    }

    @Override
    public Set<Renderable> getSubRenderables() {
        Set<Renderable> r=new HashSet<>();
        r.add(content);
        return r;
    }
}
