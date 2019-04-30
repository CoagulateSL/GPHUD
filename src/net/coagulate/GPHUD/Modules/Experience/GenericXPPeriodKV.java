package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Modules.KV;

/**
 *
 * @author Iain Price
 */
public class GenericXPPeriodKV extends KV {
    private String myname;
    public GenericXPPeriodKV(String name) { myname=name; }

    public boolean isGenerated() { return true; }
    public String fullname() { return "Experience."+myname; }
    public KVSCOPE scope() { return KVSCOPE.NONSPATIAL; }
    public KVTYPE type() { return KVTYPE.FLOAT; }
    public String description() { return "Cycle length, in days, for "+myname+" limit"; }
    public String editpermission() { return "Instance.Owner"; }
    public String defaultvalue() { return "6.75"; }
    public String conveyas() { return null; }
    public KVHIERARCHY hierarchy() { return KVHIERARCHY.CUMULATIVE; }
    public boolean template() { return true; }
    public String name() { return myname; }
}
