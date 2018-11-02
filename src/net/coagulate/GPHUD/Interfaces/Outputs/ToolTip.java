package net.coagulate.GPHUD.Interfaces.Outputs;

import java.util.HashSet;
import java.util.Set;
import net.coagulate.GPHUD.State;

/**
 *
 * @author Iain Price
 */
public class ToolTip implements Renderable {

    private final String element;
    private final Renderable tooltip;

    public ToolTip(String element,String tooltip) { this.element=element; this.tooltip=new Text(tooltip); }

    public ToolTip(String element, Renderable tooltip) { this.element=element; this.tooltip=tooltip; }
    
    @Override
    public String asText(State st) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String asHtml(State st, boolean rich) {
        return "<div class=\"tooltip\">"+element+"<span class=\"tooltiptext\">"+tooltip.asHtml(st, rich)+"</span></div>";
    }

    @Override
    public Set<Renderable> getSubRenderables() {
        return new HashSet<>();
    }
    
}
