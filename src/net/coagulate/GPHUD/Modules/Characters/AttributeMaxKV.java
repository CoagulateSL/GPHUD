package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Modules.KV;

/** Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AttributeMaxKV extends KV {

    Attribute attribute;
    public AttributeMaxKV(Attribute attribute) {
        this.attribute=attribute;
    }

    @Override
    public boolean isGenerated() {
        return true;
    }

    @Override
    public String name() {
        return attribute.getName()+"MAX";
    }

    @Override
    public KVSCOPE scope() {
        return KVSCOPE.COMPLETE;
    }

    @Override
    public KVTYPE type() {
        return attribute.getKVType();
    }

    @Override
    public String description() {
        return "Character attribute "+attribute.getName()+" maximum allowed value";
    }

    @Override
    public String editpermission() {
        return "Characters.CreateAttribute";
    }

    @Override
    public String defaultvalue() {
        return attribute.getKVDefaultValue();
    }

    @Override
    public String conveyas() {
        return "";
    }

    @Override
    public KVHIERARCHY hierarchy() {
        return attribute.getKVHierarchy();
    }

    @Override
    public String fullname() {
        return "Characters."+name();
    }

    @Override
    public boolean template() {
        return true;
    }
    
}
