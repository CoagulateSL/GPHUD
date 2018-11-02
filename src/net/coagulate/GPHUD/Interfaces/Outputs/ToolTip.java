package net.coagulate.GPHUD.Interfaces.Outputs;

import java.util.Set;
import net.coagulate.GPHUD.State;

/**
 *
 * @author Iain Price
 */
public class ToolTip implements Renderable {

    private final String element;
    private final String tooltip;

    public ToolTip(String element,String tooltip) { this.element=element; this.tooltip=tooltip; }
    @Override
    public String asText(State st) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String asHtml(State st, boolean rich) {
        return "<div class=\"tooltip\">"+element+"<span class=\"tooltiptext\">"+tooltip+"</span></div>";
    }

    @Override
    public Set<Renderable> getSubRenderables() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
