package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Modules.KV;

/** Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AttributeKV extends KV {

    Attribute attribute;
    public AttributeKV(Attribute attribute) {
        this.attribute=attribute;
    }

    @Override
    public boolean isGenerated() {
        return true;
    }

    @Override
    public String name() {
        return attribute.getName();
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
        return "Character attribute "+attribute.getName();
    }

    @Override
    public String editpermission() {
        return "Characters.Set"+name();
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
        return false; // it's usually not the character KV that is templated, but that it its self templates into other templated KVs ...
    }
    
}
