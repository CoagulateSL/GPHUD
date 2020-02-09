package net.coagulate.GPHUD.Modules.Publishing;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Groups extends Publishing {
	@URL.URLs(url="/publishing/allgroups")
	public static void allGroupsSample(@Nonnull final State st,
	                                   final SafeMap values) {
		st.form().add(new TextHeader("All Groups And Members"));
		published(st,"allgroups/"+st.getInstance().getId());
	}

	@URL.URLs(url="/publishing/group/*")
	public static void oneGroupSample(@Nonnull final State st,
	                                  final SafeMap values) {
		final CharacterGroup group=CharacterGroup.get(getPartInt(st,1));
		st.form().add(new TextHeader("Group: "+group.getName()));
		published(st,"group/"+group.getId());
	}

	@URL.URLs(url="/publishing/grouptype/*")
	public static void groupTypeSample(@Nonnull final State st,
	                                   final SafeMap values) {
		final Instance instance=Instance.get(getPartInt(st,1));
		final String type=getPart(st,2);
		st.form().add(new TextHeader("GroupType: "+type));
		published(st,"grouptype/"+instance.getId()+"/"+type);
	}

	@URL.URLs(url="/published/grouptype/*", requiresAuthentication=false)
	public static void groupType(@Nonnull final State st,
	                             final SafeMap values) {
		final Instance instance=Instance.get(getPartInt(st,1));
		final String type=getPart(st,2);
		final Set<String> output=new TreeSet<>();
		st.setInstance(instance);
		if (!st.getKV("Publishing.PublishGroups").boolValue()) {
			throw new UserConfigurationException("Groups publishing is not enabled in "+instance+", please set Publishing.PublishGroups to TRUE");
		}
		for (final CharacterGroup group: instance.getGroupsForKeyword(type)) {
			output.add(formatGroup(group));
		}
		st.form().add("<table border=0>");
		for (final String s: output) { st.form().add(s); }
		st.form().add("</table>");
		contentResizer(st);
	}

	@URL.URLs(url="/published/group/*", requiresAuthentication=false)
	public static void oneGroup(@Nonnull final State st,
	                            final SafeMap values) {
		try {
			final CharacterGroup group=CharacterGroup.get(getPartInt(st,1));
			group.validate(st);
			final Instance instance=group.getInstance();
			st.setInstance(instance);
			if (!st.getKV("Publishing.PublishGroups").boolValue()) {
				throw new UserConfigurationException("Groups publishing is not enabled in "+instance+", please set Publishing.PublishGroups to TRUE");
			}
			st.form().add("<table border=0>");
			st.form().add(formatGroup(group));
			st.form().add("</table>");
		} catch (final NoDataException e) {
			st.form().add("<i>The group being referenced does not exist</i>");
		}
		contentResizer(st);
	}

	@URL.URLs(url="/published/allgroups/*", requiresAuthentication=false)
	public static void allGroups(@Nonnull final State st,
	                             final SafeMap values) {
		final Instance instance=Instance.get(getPartInt(st,1));
		st.setInstance(instance);
		final TreeMap<String,String> grouprows=new TreeMap<>();
		if (!st.getKV("Publishing.PublishGroups").boolValue()) {
			throw new UserConfigurationException("Groups publishing is not enabled in "+instance+", please set Publishing.PublishGroups to TRUE");
		}
		st.form().add("<table border=0>");
		for (final CharacterGroup group: instance.getCharacterGroups()) {
			final String line=formatGroup(group);
			String type=group.getType();
			if (type==null) { type="ZZZZ"; }
			final String sortby=type+group.getName();
			grouprows.put(sortby,line);
		}
		for (final String s: grouprows.values()) { st.form().add(s); }
		st.form().add("</table>");
		contentResizer(st);
	}

	@Nonnull
	private static String formatGroup(@Nonnull final CharacterGroup group) {
		final StringBuilder line=new StringBuilder("<tr><th colspan=2 align=left>"+group.getName()+(group.getType()!=null?" (<i>"+group.getType()+"</i>)":"")+"</th></tr>");
		final TreeMap<String,String> charrows=new TreeMap<>();
		for (final Char ch: group.getMembers()) {
			String userline="<tr><td>"+ch.getName()+"</td><td><i>"+ch.getOwner().getName()+"</i></td>";
			String sortby=ch.getName();
			if (group.getOwner()==ch) {
				sortby="  "+sortby;
				userline+="<td><b>Owner</b></td>";
			}
			else {
				if (group.isAdmin(ch)) {
					sortby=" "+sortby;
					userline+="<td>Admin</td>";
				}
			}
			userline+="</tr>";
			charrows.put(sortby,userline);
		}
		for (final String s: charrows.values()) { line.append(s); }
		return line.toString();
	}

}
