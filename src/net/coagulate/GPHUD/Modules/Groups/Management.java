package net.coagulate.GPHUD.Modules.Groups;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.Hidden;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Configuration.GenericConfiguration;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Management for groups.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Management {
	@URLs(url = "/groups")
	public static void manage(@Nonnull State st, SafeMap values) {
		manage(st, values, null);
	}

	public static void manage(@Nonnull State st, SafeMap values, @Nullable String typefilter) {
		Form f = st.form;
		f.noForm();
		Table t = new Table();
		f.add(new TextHeader("Group Management"));
		if (typefilter != null) { f.add(new TextSubHeader("Filtered by type: " + typefilter)); }
		Set<CharacterGroup> groups = st.getInstance().getCharacterGroups();
		f.add(t);
		HeaderRow hr = new HeaderRow();
		t.add(hr);
		hr.add("Name");
		if (typefilter == null) { hr.add("Type"); }
		hr.add("Owner");
		hr.add("Open");
		hr.add("Members");
		for (CharacterGroup g : groups) {
			String keyword = g.getType();
			if (keyword == null) { keyword = ""; }
			if (typefilter == null || typefilter.equalsIgnoreCase(keyword)) {
				t.openRow().add(new Link(g.getName(), "/GPHUD/groups/view/" + g.getId()));
				String owner = "";
				if (g.getOwner() != null) { owner = g.getOwner().asHtml(st, true); }
				if (typefilter == null) { t.add(keyword); }
				t.add(owner);
				if (g.isOpen()) { t.add("Yes"); } else { t.add(""); }
				t.add(g.getMembers().size() + " members");
			}
		}
		if (st.hasPermission("Groups.Create")) {
			Form create = new Form();
			f.add(create);
			create.setAction("/GPHUD/groups/create");
			create.add(new Hidden("okreturnurl", st.getFullURL()));
			create.add(new Button("Create Group"));
		}
	}

	@URLs(url = "/groups/type/*")
	public static void manageType(@Nonnull State st, SafeMap values) {
		String[] comps = st.getDebasedURL().split("/");
		String type = comps[comps.length - 1];
		if ("BLANK".equals(type)) { type = ""; }
		manage(st, values, type);
	}


	@Nonnull
	public static List<String> groupTypes(@Nonnull State st) {
		List<String> ret = new ArrayList<>();
		ret.add("");
		ret.addAll(st.getCharacterGroupTypes());
		return ret;
	}

	@URLs(url = "/groups/create", requiresPermission = "Groups.Create")
	public static void createForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Groups.create", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Create a group", requiresPermission = "Groups.Create")
	public static Response create(@Nonnull State st,
	                              @Arguments(type = ArgumentType.TEXT_ONELINE, description = "Name of the group", max = 128)
			                              String name,
	                              @Arguments(type = ArgumentType.CHOICE, description = "Type of the group", mandatory = false, choiceMethod = "groupTypes")
			                              String type) {
		try { st.getInstance().createCharacterGroup(name, false, type); } catch (UserException e) {
			return new ErrorResponse("Failed to create group: " + e.getMessage());
		}
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Create Group", type, null, name, "Created new group " + name + " of type " + type);
		return new OKResponse("Successfully created group");
	}

	@URLs(url = "/groups/view/*")
	public static void viewGroup(@Nonnull State st, SafeMap values) throws SystemException {

		//System.out.println(st.uri);
		String[] split = st.getDebasedURL().split("/");
		//System.out.println(split.length);
		String id = split[split.length - 1];
		CharacterGroup group = CharacterGroup.get(Integer.parseInt(id));
		viewGroup(st, values, group);
	}

	public static void viewGroup(@Nonnull State st, SafeMap values, @Nonnull CharacterGroup group) throws SystemException {
		Form f = st.form;
		f.noForm();
		f.add(new TextHeader(group.getName()));
		Table t = new Table();
		f.add(t);
		String owner = "None";
		if (group.getOwner() != null) {
			owner = group.getOwner().asHtml(st, true);
		}
		Char groupowner = group.getOwner();
		t.openRow().add("Owner").add(owner);
		if (st.hasPermission("Groups.SetOwner")) {
			Form setowner = new Form();
			setowner.setAction("../setowner");
			setowner.add(new Hidden("group", group.getName()));
			setowner.add(new Hidden("okreturnurl", st.getFullURL()));
			setowner.add(new Button("Set Owner", true));
			t.add(setowner);
		}
		Set<Char> members = group.getMembers();
		t.openRow().add("Members").add(members.size() + " members");
		t.openRow().add("Open").add(group.isOpen() ? "Yes" : "No");
		if (st.hasPermission("Groups.SetOpen")) {
			t.add(new Form(st, true, "../setopen", group.isOpen() ? "Close" : "Open", "group", group.getName(), "open", group.isOpen() ? "1" : ""));
		}
		for (Char c : members) {
			t.openRow().add("").add(c);
			if (st.hasPermission("Groups.SetGroup")) {
				Form removeform = new Form();
				removeform.setAction("../remove");
				removeform.add(new Hidden("group", group.getName()));
				removeform.add(new Hidden("member", c.getName()));
				removeform.add(new Hidden("okreturnurl", st.getFullURL()));
				removeform.add(new Button("Kick Member", true));
				t.add(new Cell(removeform));
			}
			if (group.isAdmin(c)) { t.add(new Cell(new Color("Red", "Admin"))); } else { t.add(""); }
			if (c == groupowner) { t.add(new Cell(new Color("Red", "Leader"))); } else { t.add(""); }
			if (c == groupowner || st.hasPermission("Groups.Create")) {
				t.add(new Form(st, true, "../setadmin", "Toggle Admin", "group", group.getName(), "character", c.getName(), "Admin", group.isAdmin(c) ? "" : "true"));
			}
		}
		if (st.hasPermission("Groups.SetGroup")) {
			t.openRow().add(new Cell(new Form(st, true, "../add", "Add Member", "group", group.getName()), 2));
		}
		if (st.hasPermission("Groups.Delete")) {
			t.openRow().add(new Cell(new Form(st, false, "../delete", "Delete Groups", "group", group.getName()), 2));
		}
		f.add(new TextSubHeader("KV influences"));
		GenericConfiguration.page(st, values, group, st);
	}

	@URLs(url = "/groups/setowner", requiresPermission = "Groups.SetOwner")
	public static void setOwnerForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Groups.SetOwner", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Set the leader of a group", requiresPermission = "Groups.SetOwner")
	public static Response setOwner(@Nonnull State st,
	                                @Nonnull @Arguments(description = "Group to change the leader of", type = ArgumentType.CHARACTERGROUP)
			                                CharacterGroup group,
	                                @Nonnull @Arguments(description = "New leader, optionally", type = ArgumentType.CHARACTER, mandatory = false)
			                                Char newowner) {
		Char oldowner = group.getOwner();
		String oldownername = null;
		if (oldowner != null) { oldownername = oldowner.getName(); }
		if (newowner == oldowner) {
			return new OKResponse("That character (" + newowner.getName() + ") is already the group leader");
		}
		if (newowner == null) {
			group.setOwner(null);
			Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "SetOwner", group.getName(), oldownername, null, "Group leader removed");
			if (oldowner != null) {
				oldowner.hudMessage(("You are no longer the group leader for " + group.getName()));
			}
			return new OKResponse("Group leader removed.");
		}
		// or a member
		boolean ingroup = false;
		for (Char c : group.getMembers()) { if (c == newowner) { ingroup = true; }}
		if (!ingroup) {
			return new ErrorResponse("New leader " + newowner.getName() + " must be in group " + group.getName());
		}
		if (oldowner != null) { oldowner.hudMessage(("You are no longer the group leader for " + group.getName())); }
		newowner.hudMessage("You are now the group leader for " + group.getName());
		group.setOwner(newowner);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, newowner, "SetOwner", group.getName(), oldownername, newowner.getName(), "Group leader changed");
		return new OKResponse("Group leader updated");
	}

	@URLs(url = "/groups/add", requiresPermission = "Groups.SetGroup")
	public static void addForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Groups.Add", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Add a member to this group", requiresPermission = "Groups.SetGroup")
	public static Response add(@Nonnull State st,
	                           @Nonnull @Arguments(type = ArgumentType.CHARACTERGROUP, description = "Group to add character to")
			                           CharacterGroup group,
	                           @Nonnull @Arguments(description = "Character to add to the group", type = ArgumentType.CHARACTER)
			                           Char newmember) throws UserException {
		boolean debug = false;
		Attribute attr = st.getAttribute(group);
		String grouptype = null;
		if (attr != null) {
			grouptype = attr.getSubType();
		}
		CharacterGroup existinggroup = null;
		if (grouptype != null) { existinggroup = newmember.getGroup(grouptype); }
		if (existinggroup != null && existinggroup.getOwner() == newmember) {
			return new ErrorResponse("Refusing to move character " + newmember.getName() + ", they are currently group leader of " + existinggroup.getName() + ", you must manually eject them from that position");
		}
		if (existinggroup != null) { existinggroup.removeMember(newmember); }
		try { group.addMember(newmember); } catch (UserException e) {
			return new ErrorResponse("Failed to add " + newmember.getName() + " to " + group.getName() + ", they are probably in no group now! - " + e.getMessage());
		}
		String oldgroupname = null;
		if (existinggroup != null) { oldgroupname = existinggroup.getName(); }
		String result = newmember.getName() + " is now in group " + group.getName();
		if (existinggroup != null) {
			result = newmember.getName() + " was moved into group " + group.getName() + " (was formerly in " + existinggroup.getName() + ")";
		}
		newmember.hudMessage("You have been added to the group " + group.getName());
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, newmember, "AddMember", group.getName(), oldgroupname, group.getName(), result);
		return new OKResponse(result);
	}

	@URLs(url = "/groups/remove", requiresPermission = "Groups.SetGroup")
	public static void removeForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Groups.Remove", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Remove a member from this group", requiresPermission = "Groups.SetGroup")
	public static Response remove(@Nonnull State st,
	                              @Nonnull @Arguments(type = ArgumentType.CHARACTERGROUP, description = "Group to remove character from")
			                              CharacterGroup group,
	                              @Nonnull @Arguments(description = "Character to remove from the group", type = ArgumentType.CHARACTER)
			                              Char member) {
		if (group.getOwner() == member) {
			return new ErrorResponse("Will not remove " + member.getName() + " from " + group.getName() + ", they are the group leader, you must demote them by replacing them or leaving the group leaderless.");
		}
		try { group.removeMember(member); } catch (UserException e) {
			return new ErrorResponse("Failed to remove member - " + e.getMessage());
		}
		member.hudMessage("You have been removed from group " + group.getName());
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, member, "RemoveMember", group.getName(), group.getName(), null, "Removed member from group");
		return new OKResponse(member.getName() + " was removed from group " + group.getName());
	}

	@URLs(url = "/groups/delete", requiresPermission = "Groups.Delete")
	public static void deleteForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Groups.Delete", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Delete a group", requiresPermission = "Groups.Delete")
	public static Response delete(@Nonnull State st,
	                              @Nonnull @Arguments(description = "Group to delete", type = ArgumentType.CHARACTERGROUP)
			                              CharacterGroup group) {
		String groupname = group.getName();
		group.delete();
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "DeleteGroup", groupname, groupname, null, "Group was deleted");
		return new OKResponse(groupname + " was deleted");
	}

	@URLs(url = "/groups/setadmin")
	public static void setAdminForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Groups.SetAdmin", values);
	}

	@Nonnull
	@Commands(context = Context.ANY, description = "Set the groups admin flag on a user")
	public static Response setAdmin(@Nonnull State st,
	                                @Nonnull @Arguments(description = "Group to set the character's admin flag on", type = ArgumentType.CHARACTERGROUP)
			                                CharacterGroup group,
	                                @Nonnull @Arguments(description = "Character to set the admin flag on", type = ArgumentType.CHARACTER)
			                                Char character,
	                                @Arguments(description = "Admin flag to set on the character in this group", type = ArgumentType.BOOLEAN)
			                                boolean admin) throws SystemException {
		if (!group.hasMember(character)) {
			return new ErrorResponse(character.getName() + " is not a member of group " + group.getName());
		}
		// must be instance admin or group owner
		boolean ok = false;
		if (group.getOwner() == st.getCharacter()) { ok = true; }
		if (!ok && st.hasPermission("Groups.Create")) {ok = true; }
		if (!ok) {
			return new ErrorResponse("You must be group owner, or have Groups.Create permissions to set admin flags");
		}
		boolean oldflag = group.isAdmin(character);
		group.setAdmin(character, admin);
		if (admin) {
			character.hudMessage("You are now a group administrator for " + group.getName());
		} else {
			character.hudMessage("You are no longer a group administrator for " + group.getName());
		}
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, character, "SetAdmin", group.getName(), oldflag + "", admin + "", "Set admin flag");
		return new OKResponse("Successfully altered admin flag on " + character + " in " + group);
	}

	@URLs(url = "/groups/setopen")
	public static void setOpenForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Groups.SetOpen", values);
	}

	@Nonnull
	@Commands(context = Context.ANY, description = "Set the groups open flag", requiresPermission = "Groups.SetOpen")
	public static Response setOpen(@Nonnull State st,
	                               @Nonnull @Arguments(description = "Group to modify", type = ArgumentType.CHARACTERGROUP)
			                               CharacterGroup group,
	                               @Arguments(description = "Group open?", type = ArgumentType.BOOLEAN)
			                               boolean open) throws SystemException {
		group.validate(st);
		// must be instance admin or group owner
		boolean oldflag = group.isOpen();
		group.setOpen(open);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "SetOpen", group.getName(), oldflag + "", open + "", "Set open flag");
		return new OKResponse("Successfully set open flag to " + open + " on group " + group.getName());
	}

}
