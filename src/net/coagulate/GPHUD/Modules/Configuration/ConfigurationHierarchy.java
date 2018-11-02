package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.Event;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.KVValue;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;

/** Renders the entirety of a hierarchical KV for editing.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConfigurationHierarchy extends Form {

    public ConfigurationHierarchy(State st,KV kv,State simulated) {
        if (st==null) { throw new SystemException("Null state?"); }
        if (simulated==null) { simulated=st; }
        if (kv==null) { throw new SystemException("KV null?"); }
        noForm();
        add(new TextHeader(kv.fullname()));
        add(kv.description()).br();
        br();
        String editperm=kv.editpermission();
        if (editperm!=null && !editperm.isEmpty()) { add("<b>Requires Permission:</b> "+editperm).br(); }
        String convey=kv.conveyas();
        if (convey!=null && !convey.isEmpty()) { add("<b>Conveyed as:</b> "+convey).br(); }
        add("<b>KV Type:</b> "+kv.type()).br();
        add("<b>Hierarchy Type:</b> "+kv.hierarchy()).br();
        add("<b>Hierarchy Scope:</b> "+kv.scope()).br();
        if (kv.isGenerated()) { add("<b>Generated</b>").br(); }
        if (kv.template()) { add("<b>Supports Templates</b>").br(); }
        br();
        
        Table h=new Table(); add(h);
        //h.border(true);
        h.openRow();
        h.add("SYSTEM");
        h.add("Default");
        h.add(kv.defaultvalue());
        Instance instance=simulated.getInstance();
        if (kv.appliesTo(instance)) { addKVRow(st,h,kv,instance,simulated); }
        for (Region r:instance.getRegions()) { if (kv.appliesTo(r)) { addKVRow(st,h,kv,r,simulated); } }
        for (Zone z:instance.getZones()) { if (kv.appliesTo(z)) { addKVRow(st,h,kv,z,simulated); } }
        for (Event e:instance.getEvents()) { if (kv.appliesTo(e)) { addKVRow(st,h,kv,e,simulated); } }
        for (CharacterGroup cg:instance.getCharacterGroups()) { if (kv.appliesTo(cg)) { addKVRow(st,h,kv,cg,simulated); } }
        if (simulated.getCharacterNullable()!=null) { if (kv.appliesTo(simulated.getCharacter())) { addKVRow(st,h,kv,simulated.getCharacter(),simulated); } }
        try {
            KVValue kvexample = simulated.getKV(kv.fullname());
            h.openRow();h.add("<i>Example</i>").add("<i>"+kvexample.path()+"</i>").add("<i>"+kvexample.value()+"</i>");
        }
        catch (UserException ue) {
            h.openRow();h.add("<b>ERROR</b>").add(ue.getLocalizedMessage()).add("<b>ERROR</b>");
        }
    }
    
    void addKVRow(State st,Table t,KV kv,TableRow dbo,State simulated) {
        if (dbo==null) { throw new SystemException("Add KV Row for Null DBO?"); }
        t.openRow();        
        if (dbo instanceof CharacterGroup) {
            t.add(dbo.getClass().getSimpleName()+" : "+((CharacterGroup)dbo).getTypeNotNull());
        }
        else { t.add(dbo.getClass().getSimpleName()); }
        t.add(dbo);
        String value=simulated.getRawKV(dbo,kv.fullname()); if (value==null) { value=""; }
        t.add(value);
        if (kv.editpermission().isEmpty() || st.hasPermission(kv.editpermission())) {
            String typefield="";
            String typename=dbo.getName();
            String targeturl="";
            if (dbo instanceof Instance) { targeturl="/"+Interface.base()+"/configuration/setinstancevalue"; typefield="instance"; }
            if (dbo instanceof Region) { targeturl="/"+Interface.base()+"/configuration/setregionvalue"; typefield="region"; }
            if (dbo instanceof Zone) { targeturl="/"+Interface.base()+"/configuration/setzonevalue"; typefield="zone"; }
            if (dbo instanceof Event) { targeturl="/"+Interface.base()+"/configuration/seteventvalue"; typefield="event"; }
            if (dbo instanceof CharacterGroup) { targeturl="/"+Interface.base()+"/configuration/setgroupvalue"; typefield="group"; }
            if (dbo instanceof Char) { targeturl="/"+Interface.base()+"/configuration/setcharvalue"; typefield="character"; }
            String kvvalue=simulated.getRawKV(dbo,kv.fullname());
            if (kvvalue==null) { kvvalue=""; }
            t.add(new Form(st, true, targeturl, "Edit",typefield,typename,"key",kv.fullname(),"value",kvvalue));
            if (dbo instanceof Char && dbo==st.getCharacterNullable()) {
                Attribute selfeditable=null;
                // vet against attributes
                for (Attribute attr:st.getAttributes()) {
                    if (attr.isKV() && kv.fullname().equalsIgnoreCase("Characters."+attr.getName())) {
                        if (attr.getSelfModify()) {
                            selfeditable=attr;
                        }
                    }
                }
                if (selfeditable!=null) {
                    t.add(new Form(st, true, "/"+Interface.base()+"/configuration/setself", "Self-Edit","attribute",selfeditable.getName(),"value",kvvalue));
                }
            }
        }
    }
}
