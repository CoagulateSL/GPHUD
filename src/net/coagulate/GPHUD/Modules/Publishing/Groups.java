package net.coagulate.GPHUD.Modules.Publishing;

import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Groups extends Publishing {
	@URL.URLs(url="/publishing/allgroups")
	public static void allGroupsSample(State st, SafeMap values) {
		st.form.add(new TextHeader("All Groups And Members"));
		published(st,"allgroups/" + st.getInstance().getId());
	}

	@URL.URLs(url="/publishing/group/*")
	public static void oneGroupSample(State st, SafeMap values) {
		CharacterGroup group=CharacterGroup.get(getPartInt(st,1));
		st.form.add(new TextHeader("Group: "+group.getName()));
		published(st,"group/" + group.getId());
	}

	@URL.URLs(url="/publishing/grouptype/*")
	public static void groupTypeSample(State st, SafeMap values) {
		Instance instance=Instance.get(getPartInt(st,1));
		String type=getPart(st,2);
		st.form.add(new TextHeader("GroupType: "+type));
		published(st,"grouptype/" +instance.getId()+"/"+ type);
	}

	@URL.URLs(url="/published/grouptype/*",requiresAuthentication = false)
	public static void groupType(State st,SafeMap values) {
		Instance instance=Instance.get(getPartInt(st,1));
		String type=getPart(st,2);
		Set<String> output = new TreeSet<>();
		st.setInstance(instance);
		if (!st.getKV("Publishing.PublishGroups").boolValue()) { throw new UserException("Groups publishing is not enabled in "+instance+", please set Publishing.PublishGroups to TRUE"); }
		for (CharacterGroup group:instance.getGroupsForKeyword(type)) {
			output.add(formatGroup(group));
		}
		st.form.add("<table border=0>");
		for (String s:output) st.form.add(s);
		st.form.add("</table>");
		contentResizer(st);
	}

	@URL.URLs(url="/published/group/*",requiresAuthentication = false)
	public static void oneGroup(State st,SafeMap values) {
		CharacterGroup group=CharacterGroup.get(getPartInt(st,1));
		Instance instance=group.getInstance();
		st.setInstance(instance);
		if (!st.getKV("Publishing.PublishGroups").boolValue()) { throw new UserException("Groups publishing is not enabled in "+instance+", please set Publishing.PublishGroups to TRUE"); }
		st.form.add("<table border=0>");
		st.form.add(formatGroup(group));
		st.form.add("</table>");
		contentResizer(st);
	}

	@URL.URLs(url="/published/allgroups/*",requiresAuthentication = false)
	public static void allGroups(State st,SafeMap values) {
		Instance instance=Instance.get(getPartInt(st,1));
		st.setInstance(instance);
		TreeMap<String,String> grouprows=new TreeMap();
		if (!st.getKV("Publishing.PublishGroups").boolValue()) { throw new UserException("Groups publishing is not enabled in "+instance+", please set Publishing.PublishGroups to TRUE"); }
		st.form.add("<table border=0>");
		for (CharacterGroup group:instance.getCharacterGroups()) {
			String line=formatGroup(group);
			String type=group.getType();
			if (type==null) { type="ZZZZ"; }
			String sortby=type+group.getName();
			grouprows.put(sortby,line);
		}
		for (String k:grouprows.keySet()) { st.form.add(grouprows.get(k)); }
		st.form.add("</table>");
		contentResizer(st);
	}

	private static String formatGroup(CharacterGroup group) {
		String line="<tr><th colspan=2 align=left>"+group.getName()+(group.getType()!=null?" (<i>"+group.getType()+"</i>)":"")+"</th></tr>";
		TreeMap<String,String> charrows=new TreeMap();
		for (Char ch:group.getMembers()) {
			String userline="<tr><td>"+ch.getName()+"</td><td><i>"+ch.getOwner().getName()+"</i></td>";
			String sortby=ch.getName();
			if (group.getOwner()==ch) { sortby="  "+sortby; userline+="<td><b>Owner</b></td>"; }
			else {
				if (group.isAdmin(ch)) { sortby=" "+sortby; userline+="<td>Admin</td>"; }
			}
			userline+="</tr>";
			charrows.put(sortby,userline);
		}
		for (String k:charrows.keySet()) { line+=charrows.get(k); }
		return line;
	}

}
