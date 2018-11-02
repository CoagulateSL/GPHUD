package net.coagulate.GPHUD.Interfaces.Outputs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.coagulate.GPHUD.State;
/** Implements a row of elements in a table layout.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Row implements Renderable {
    List<Cell> row=new ArrayList<>();
    
    public Row() {}
    public Row(Cell c) { add(c); }
    public Row(String c) { add(c); }
    
    public Row add(Cell c) { row.add(c); return this; }
    public Row add(String s) { row.add(new Cell(new Text(s))); return this; }
    public Row add(Renderable r) { row.add(new Cell(r)); return this; }
    public boolean isHeader() { return false; }

    @Override
    public String asText(State st) {
        String s="";
        for (Cell c:row) {
            if (!s.isEmpty()) { s+=" : "; }
            s=s+c.asText(st);
        }
        return s;
    }

    @Override
    public String asHtml(State st,boolean rich) {
        String s="<tr";
        if (!bgcolor.isEmpty()) { s+=" bgcolor="+bgcolor; }
        if (!alignment.isEmpty()) { s+=" align="+alignment; }
        s+=">";
        for (Cell c:row) {
            c.header=isHeader();
            s=s+c.asHtml(st,rich);
        }
        return s+"</tr>";
    }

    @Override
    public Set<Renderable> getSubRenderables() {
        Set<Renderable> r=new HashSet<>();
        for (Cell c:row) { r.add(c); }
        return r;
    }

    public void add(Integer ownerid) {
        add(""+ownerid);
    }

    public void add(boolean online) {
        add(Boolean.toString(online));
    }
    String bgcolor="";
    public void setbgcolor(String setbgcolor) {
        bgcolor=setbgcolor;
    }
    String alignment="";
    public void align(String alignment) {
        this.alignment=alignment;
    }
    
}
