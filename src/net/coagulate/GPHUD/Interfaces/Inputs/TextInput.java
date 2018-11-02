package net.coagulate.GPHUD.Interfaces.Inputs;

import java.util.Set;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;

/** Implements a single line text input box.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TextInput extends Input {
    String name="";
    Integer width=null;
    public TextInput(String name) {this.name=name;} 
    public TextInput(String name,String value) {this.name=name;this.value=value;} 
    public TextInput(String name,int width) { this.name=name; this.width=width; }
    public TextInput(String name,String value,int width) {this.name=name;this.value=value;this.width=width;} 
    
    String getType() { return "text"; }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String asHtml(State st, boolean rich) {
        String s="<input type=\""+getType()+"\" name=\""+name+"\" value=\""+value+"\" ";
        if (width!=null) { s+="size="+width+" "; }
        //if (!(st.handler instanceof net.coagulate.GPHUD.Interfaces.HUD.Interface)) {
        s+="autofocus ";
        //}
        s+="/>";
        return s;
    }

    @Override
    public Set<Renderable> getSubRenderables() {
        return null;
    }
}
