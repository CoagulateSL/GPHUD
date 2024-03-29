package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Interface;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.KVValue;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Renders the entirety of a hierarchical KV for editing.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConfigurationHierarchy extends Form {
	
	public ConfigurationHierarchy(@Nonnull final State st,
	                              @Nonnull final KV kv,
	                              @Nullable State simulated,
	                              @Nonnull final SafeMap parameters) {
		if (simulated==null) {
			simulated=st;
		}
		noForm();
		add(new TextHeader(kv.fullName()));
		add(kv.description()).br();
		br();
		final String editperm=kv.editPermission();
		if (!editperm.isEmpty()) {
			add("<b>Requires Permission:</b> "+editperm).br();
		}
		final String convey=kv.conveyAs();
		if (!convey.isEmpty()) {
			add("<b>Conveyed as:</b> "+convey).br();
		}
		add("<b>KV Type:</b> "+kv.type()).br();
		add("<b>Hierarchy Type:</b> "+kv.hierarchy()).br();
		add("<b>Hierarchy Scope:</b> "+kv.scope()).br();
		if (!kv.onUpdate().isEmpty()) {
			add("<b>On Update:</b> "+kv.onUpdate()).br();
		}
		if (kv.isGenerated()) {
			add("<b>Generated</b>").br();
		}
		if (kv.template()) {
			add("<b>Supports Templates</b>").br();
		}
		br();
		final String dboname=parameters.get("dbobject");
		int id=-1;
		if (parameters.containsKey("id")) {
			id=Integer.parseInt(parameters.get("id"));
		}
		if (!dboname.isEmpty()&&id>=0) {
			String value="";
			final String magickey="value-"+dboname+"-"+id;
			if (parameters.containsKey(magickey)) {
				value=parameters.get(magickey);
			}
			TableRow dbo=null;
			String type="unknown";
			if ("instancekvstore".equals(dboname)) {
				dbo=Instance.get(id);
				type="Instance";
			}
			if ("regionkvstore".equals(dboname)) {
				dbo=Region.get(id,true);
				type="Region";
			}
			if ("charactergroupkvstore".equals(dboname)) {
				dbo=CharacterGroup.get(id);
				type="CharacterGroup";
			}
			if ("characterkvstore".equals(dboname)) {
				dbo=Char.get(id);
				type="Character";
			}
			if ("eventskvstore".equals(dboname)) {
				dbo=Event.get(id);
				type="Event";
			}
			if ("zonekvstore".equals(dboname)) {
				dbo=Zone.get(id);
				type="Zone";
			}
			if ("effectskvstore".equals(dboname)) {
				dbo=Effect.get(id);
				type="Effect";
			}
			if (dbo==null) {
				throw new SystemImplementationException("Did not get a DBO from "+dboname);
			}
			if (st.hasPermission(kv.editPermission())) {
				final String oldvalue=st.getRawKV(dbo,kv.fullName());
				try {
					st.setKV(dbo,kv.fullName(),value);
					Audit.audit(true,
					            st,
					            Audit.OPERATOR.AVATAR,
					            null,
					            null,
					            "Set"+type+"KV",
					            kv.fullName(),
					            oldvalue,
					            value,
					            "Changed "+type+"/"+dbo.getNameSafe()+" configuration");
					add("<font color=green>OK: Value updated</font>");
					st.purgeCache(dbo);
				} catch (@Nonnull final UserException e) {
					add("<font color=red>ERROR: "+e.getLocalizedMessage()+"</font>");
				}
			} else {
				add("<font color=red>ERROR: You do not have edit permission "+kv.editPermission()+"</font>");
			}
			br();
		}
		
		final Table h=new Table();
		final DropDownList commandsDropDown=DropDownList.getCommandsList(st,"value-undefined",true);
		add(h);
		//h.border(true);
		h.openRow();
		h.add("SYSTEM");
		h.add("Default");
		h.add(kv.defaultValue());
		final Instance instance=simulated.getInstance();
		final Set<String> alledits=new HashSet<>();
		if (kv.appliesTo(instance)) {
			addKVRow(st,h,kv,instance,simulated,alledits,commandsDropDown);
		}
		for (final Region r: Region.getRegions(instance,false)) {
			if (kv.appliesTo(r)) {
				addKVRow(st,h,kv,r,simulated,alledits,commandsDropDown);
			}
		}
		for (final Zone z: Zone.getZones(instance)) {
			if (kv.appliesTo(z)) {
				addKVRow(st,h,kv,z,simulated,alledits,commandsDropDown);
			}
		}
		for (final Event e: Event.getAll(instance)) {
			if (kv.appliesTo(e)) {
				addKVRow(st,h,kv,e,simulated,alledits,commandsDropDown);
			}
		}
		for (final CharacterGroup cg: instance.getCharacterGroups()) {
			if (kv.appliesTo(cg)) {
				addKVRow(st,h,kv,cg,simulated,alledits,commandsDropDown);
			}
		}
		if (simulated.getCharacterNullable()!=null) {
			if (kv.appliesTo(simulated.getCharacter())) {
				addKVRow(st,h,kv,simulated.getCharacter(),simulated,alledits,commandsDropDown);
			}
		}
		for (final Effect e: Effect.getAll(st.getInstance())) {
			if (kv.appliesTo(e)) {
				addKVRow(st,h,kv,e,simulated,alledits,commandsDropDown);
			}
		}
		try {
			final KVValue kvexample=simulated.getKV(kv.fullName());
			h.openRow();
			h.add("<i>Example</i>").add("<i>"+kvexample.path()+"</i>").add("<i>"+kvexample.value()+"</i>");
		} catch (@Nonnull final UserException ue) {
			h.openRow();
			h.add("<b>ERROR</b>").add(ue.getLocalizedMessage()).add("<b>ERROR</b>");
		}
		add("<script>");
		add("function hideAllEdits() {");
		for (final String s: alledits) {
			add("document.getElementById('"+s+"').style.display='none';");
		}
		add("}");
		add("</script>");
	}
	
	// ----- Internal Instance -----
	void addKVRow(@Nonnull final State st,
	              @Nonnull final Table t,
	              @Nonnull final KV kv,
	              @Nonnull final TableRow dbo,
	              @Nonnull final State simulated,
	              @Nonnull final Set<String> alledits,
	              @Nonnull final DropDownList commandsDropDown) {
		t.openRow();
		if (dbo instanceof Region) {
			t.setBGColor("#ffe0e0");
		}
		if (dbo instanceof CharacterGroup) {
			t.add(dbo.getClass().getSimpleName()+" ("+((CharacterGroup)dbo).getKVPrecedence()+") : "+
			      ((CharacterGroup)dbo).getTypeNotNull());
		} else {
			t.add(dbo.getClass().getSimpleName());
		}
		t.add(dbo);
		String value=simulated.getRawKV(dbo,kv.fullName());
		if (value==null) {
			value="";
		}
		t.add(value);
		if (kv.editPermission().isEmpty()||st.hasPermission(kv.editPermission())) {
			String kvvalue=simulated.getRawKV(dbo,kv.fullName());
			if (kvvalue==null) {
				kvvalue="";
			}
			final String codename=dbo.getKVTable()+"-"+dbo.getId();
			alledits.add("edit-"+codename);
			String editor;
			//t.add(new Form(st, true, targeturl, "Edit",typefield,typename,"key",kv.fullname(),"value",kvvalue));
			switch (kv.type()) {
				case BOOLEAN -> {
					Boolean selected=null;
					if ("true".equalsIgnoreCase(value)) {
						selected=true;
					}
					if ("false".equalsIgnoreCase(value)) {
						selected=false;
					}
					editor="<input type=\"radio\" name=\"value-"+codename+"\" value=\"true\" "+
					       ((selected!=null&&selected)?"checked=checked":"")+">True";
					editor+="<input type=\"radio\" name=\"value-"+codename+"\" value=\"false\" "+
					        ((selected!=null&&!selected)?"checked=checked":"")+">False";
					editor+="<input type=\"radio\" name=\"value-"+codename+"\" value=\"\" "+
					        ((selected==null)?"checked=checked":"")+">Unset";
				}
				case INTEGER -> editor="<input size=10 type=\"text\" name=\"value-"+codename+"\" value=\""+value+"\">";
				case TEXT -> editor="<input size=80 type=\"text\" name=\"value-"+codename+"\" value=\""+value+"\">";
				case FLOAT -> editor="<input size=20 type=\"text\" name=\"value-"+codename+"\" value=\""+value+"\">";
				case UUID -> editor="<input size=36 type=\"text\" name=\"value-"+codename+"\" value=\""+value+"\">";
				case COMMAND -> {
					commandsDropDown.setValue(value);
					commandsDropDown.name("value-"+codename);
					editor=commandsDropDown.asHtml(st,true);
				}
				case COLOR -> editor="<input size=30 type=\"text\" name=\"value-"+codename+"\" value=\""+value+"\">";
				default -> throw new SystemImplementationException(
						"No editor for type "+kv.type()+" for object "+dbo.getKVTable()+"."+dbo.getNameSafe());
			}
			editor+="<input type=hidden name=dbobject value=\""+dbo.getKVTable()+"\">";
			editor+="<input type=hidden name=id value=\""+dbo.getId()+"\">";
			t.add("<button "+"onclick=\""+"hideAllEdits();"+"getElementById('editor-"+codename+
			      "').style.display='block';"+"\" "+"id=\"edit-"+codename+"\">"+"Edit"+"</button>"+
			
			      "<div style=\"display: none;\" id=\"editor-"+codename+"\">"+
			      "<form style=\"margin: 0px;\" method=post>"+editor+"&nbsp;&nbsp;&nbsp;<button "+"type"+
			      "=submit>Set</button>"+"</form>"+"</div>");
			if (dbo instanceof Char&&dbo==st.getCharacterNullable()) {
				Attribute selfeditable=null;
				// vet against attributes
				for (final Attribute attr: st.getAttributes()) {
					if (attr.isKV()&&kv.fullName().equalsIgnoreCase("Characters."+attr.getName())) {
						if (attr.getSelfModify()) {
							selfeditable=attr;
						}
					}
				}
				if (selfeditable!=null) {
					t.add(new Form(st,
					               true,
					               "/"+Interface.base()+"/configuration/setself",
					               "Self-Edit",
					               "attribute",
					               selfeditable.getName(),
					               "value",
					               kvvalue));
				}
			}
			if (dbo instanceof Region) {
				t.add("<i>Normally settings should be made at the Instance level not Region level.</i>");
			}
		}
	}
}
