package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
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

	public ConfigurationHierarchy(@Nullable State st, @Nullable KV kv, @Nullable State simulated, @Nonnull SafeMap parameters) {
		if (st == null) { throw new SystemException("Null state?"); }
		if (simulated == null) { simulated = st; }
		if (kv == null) { throw new SystemException("KV null?"); }
		noForm();
		add(new TextHeader(kv.fullname()));
		add(kv.description()).br();
		br();
		String editperm = kv.editpermission();
		if (editperm != null && !editperm.isEmpty()) { add("<b>Requires Permission:</b> " + editperm).br(); }
		String convey = kv.conveyas();
		if (convey != null && !convey.isEmpty()) { add("<b>Conveyed as:</b> " + convey).br(); }
		add("<b>KV Type:</b> " + kv.type()).br();
		add("<b>Hierarchy Type:</b> " + kv.hierarchy()).br();
		add("<b>Hierarchy Scope:</b> " + kv.scope()).br();
		if (kv.isGenerated()) { add("<b>Generated</b>").br(); }
		if (kv.template()) { add("<b>Supports Templates</b>").br(); }
		br();
		String dboname = parameters.get("dbobject");
		int id = -1;
		if (parameters.containsKey("id")) { id = Integer.parseInt(parameters.get("id")); }
		if (dboname != null && !dboname.isEmpty() && id >= 0) {
			String value = "";
			String magickey = "value-" + dboname + "-" + id;
			if (parameters.containsKey(magickey)) { value = parameters.get(magickey); }
			TableRow dbo = null;
			String type = "unknown";
			if ("instancekvstore".equals(dboname)) {
				dbo = Instance.get(id);
				type = "Instance";
			}
			if ("regionkvstore".equals(dboname)) {
				dbo = Region.get(id,true);
				type = "Region";
			}
			if ("charactergroupkvstore".equals(dboname)) {
				dbo = CharacterGroup.get(id);
				type = "CharacterGroup";
			}
			if ("characterkvstore".equals(dboname)) {
				dbo = Char.get(id);
				type = "Character";
			}
			if ("eventskvstore".equals(dboname)) {
				dbo = Event.get(id);
				type = "Event";
			}
			if ("zonekvstore".equals(dboname)) {
				dbo = Zone.get(id);
				type = "Zone";
			}
			if (dbo == null) { throw new SystemException("Did not get a DBO from " + dboname); }
			if (st.hasPermission(kv.editpermission())) {
				String oldvalue = st.getRawKV(dbo, kv.fullname());
				try {
					st.setKV(dbo, kv.fullname(), value);
					Audit.audit(true, st, Audit.OPERATOR.AVATAR, null, null, "Set" + type + "KV", kv.fullname(), oldvalue, value, "Changed " + type + "/" + dbo.getNameSafe() + " configuration");
					add("<font color=green>OK: Value updated</font>");
					st.purgeCache(dbo);
				} catch (UserException e) {
					add("<font color=red>ERROR: " + e.getLocalizedMessage() + "</font>");
				}
			} else {
				add("<font color=red>ERROR: You do not have edit permission " + kv.editpermission() + "</font>");
			}
			br();
		}

		Table h = new Table();
		add(h);
		//h.border(true);
		h.openRow();
		h.add("SYSTEM");
		h.add("Default");
		h.add(kv.defaultvalue());
		Instance instance = simulated.getInstance();
		Set<String> alledits = new HashSet<>();
		if (kv.appliesTo(instance)) { addKVRow(st, h, kv, instance, simulated, alledits); }
		for (Region r : instance.getRegions(false)) { if (kv.appliesTo(r)) { addKVRow(st, h, kv, r, simulated, alledits); } }
		for (Zone z : instance.getZones()) { if (kv.appliesTo(z)) { addKVRow(st, h, kv, z, simulated, alledits); } }
		for (Event e : instance.getEvents()) { if (kv.appliesTo(e)) { addKVRow(st, h, kv, e, simulated, alledits); } }
		for (CharacterGroup cg : instance.getCharacterGroups()) {
			if (kv.appliesTo(cg)) { addKVRow(st, h, kv, cg, simulated, alledits); }
		}
		if (simulated.getCharacterNullable() != null) {
			if (kv.appliesTo(simulated.getCharacter())) {
				addKVRow(st, h, kv, simulated.getCharacter(), simulated, alledits);
			}
		}
		try {
			KVValue kvexample = simulated.getKV(kv.fullname());
			h.openRow();
			h.add("<i>Example</i>").add("<i>" + kvexample.path() + "</i>").add("<i>" + kvexample.value() + "</i>");
		} catch (UserException ue) {
			h.openRow();
			h.add("<b>ERROR</b>").add(ue.getLocalizedMessage()).add("<b>ERROR</b>");
		}
		add("<script>");
		add("function hideAllEdits() {");
		for (String s : alledits) {
			add("document.getElementById('" + s + "').style.display='none';");
		}
		add("}");
		add("</script>");
	}

	void addKVRow(@Nonnull State st, @Nonnull Table t, @Nonnull KV kv, @Nullable TableRow dbo, @Nonnull State simulated, @Nonnull Set<String> alledits) {
		if (dbo == null) { throw new SystemException("Add KV Row for Null DBO?"); }
		t.openRow();
		if (dbo instanceof CharacterGroup) {
			t.add(dbo.getClass().getSimpleName() + " : " + ((CharacterGroup) dbo).getTypeNotNull());
		} else { t.add(dbo.getClass().getSimpleName()); }
		t.add(dbo);
		String value = simulated.getRawKV(dbo, kv.fullname());
		if (value == null) { value = ""; }
		t.add(value);
		if (kv.editpermission().isEmpty() || st.hasPermission(kv.editpermission())) {
			String typefield = "";
			String typename = dbo.getName();
			String targeturl = "";
			if (dbo instanceof Instance) {
				targeturl = "/" + Interface.base() + "/configuration/setinstancevalue";
				typefield = "instance";
			}
			if (dbo instanceof Region) {
				targeturl = "/" + Interface.base() + "/configuration/setregionvalue";
				typefield = "region";
			}
			if (dbo instanceof Zone) {
				targeturl = "/" + Interface.base() + "/configuration/setzonevalue";
				typefield = "zone";
			}
			if (dbo instanceof Event) {
				targeturl = "/" + Interface.base() + "/configuration/seteventvalue";
				typefield = "event";
			}
			if (dbo instanceof CharacterGroup) {
				targeturl = "/" + Interface.base() + "/configuration/setgroupvalue";
				typefield = "group";
			}
			if (dbo instanceof Char) {
				targeturl = "/" + Interface.base() + "/configuration/setcharvalue";
				typefield = "character";
			}
			String kvvalue = simulated.getRawKV(dbo, kv.fullname());
			if (kvvalue == null) { kvvalue = ""; }
			String codename = dbo.getKVTable() + "-" + dbo.getId();
			alledits.add("edit-" + codename);
			String editor = null;
			//t.add(new Form(st, true, targeturl, "Edit",typefield,typename,"key",kv.fullname(),"value",kvvalue));
			switch (kv.type()) {
				case BOOLEAN:
					Boolean selected = null;
					if ("true".equalsIgnoreCase(value)) { selected = true; }
					if ("false".equalsIgnoreCase(value)) { selected = false; }
					editor = "<input type=\"radio\" name=\"value-" + codename + "\" value=\"true\" " + ((selected != null && selected) ? "checked=checked" : "") + ">True";
					editor += "<input type=\"radio\" name=\"value-" + codename + "\" value=\"false\" " + ((selected != null && !selected) ? "checked=checked" : "") + ">False";
					editor += "<input type=\"radio\" name=\"value-" + codename + "\" value=\"\" " + ((selected == null) ? "checked=checked" : "") + ">Unset";
					break;
				case INTEGER:
					if (value == null) { value = ""; }
					editor = "<input size=10 type=\"text\" name=\"value-" + codename + "\" value=\"" + value + "\">";
					break;
				case TEXT:
					if (value == null) { value = ""; }
					editor = "<input size=80 type=\"text\" name=\"value-" + codename + "\" value=\"" + value + "\">";
					break;
				case FLOAT:
					if (value == null) { value = ""; }
					editor = "<input size=20 type=\"text\" name=\"value-" + codename + "\" value=\"" + value + "\">";
					break;
				case UUID:
					if (value == null) { value = ""; }
					editor = "<input size=36 type=\"text\" name=\"value-" + codename + "\" value=\"" + value + "\">";
					break;
				case COMMAND:
					if (value == null) { value = ""; }
					DropDownList d = DropDownList.getCommandsList(st, "value-" + codename, true);
					d.setValue(value);
					editor = d.asHtml(st, true);
					break;
				case COLOR:
					if (value == null) { value = ""; }
					editor = "<input size=30 type=\"text\" name=\"value-" + codename + "\" value=\"" + value + "\">";
					break;
				default:
					throw new SystemException("No editor for type " + kv.type() + " for object " + dbo.getKVTable() + "." + dbo.getNameSafe());
			}
			editor += "<input type=hidden name=dbobject value=\"" + dbo.getKVTable() + "\">";
			editor += "<input type=hidden name=id value=\"" + dbo.getId() + "\">";
			t.add("<button " +
					"onclick=\"" +
					"hideAllEdits();" +
					"getElementById('editor-" + codename + "').style.display='block';" +
					"\" " +
					"id=\"edit-" + codename + "\">" +
					"Edit" +
					"</button>" +

					"<div style=\"display: none;\" id=\"editor-" + codename + "\">" +
					"<form style=\"margin: 0px;\" method=post>" +
					editor +
					"&nbsp;&nbsp;&nbsp;<button type=submit>Set</button>" +
					"</form>" +
					"</div>");
			if (dbo instanceof Char && dbo == st.getCharacterNullable()) {
				Attribute selfeditable = null;
				// vet against attributes
				for (Attribute attr : st.getAttributes()) {
					if (attr.isKV() && kv.fullname().equalsIgnoreCase("Characters." + attr.getName())) {
						if (attr.getSelfModify()) {
							selfeditable = attr;
						}
					}
				}
				if (selfeditable != null) {
					t.add(new Form(st, true, "/" + Interface.base() + "/configuration/setself", "Self-Edit", "attribute", selfeditable.getName(), "value", kvvalue));
				}
			}
		}
	}
}
