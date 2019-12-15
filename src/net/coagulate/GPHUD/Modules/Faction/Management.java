package net.coagulate.GPHUD.Modules.Faction;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
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
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Management for factions.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Management {
	@URLs(url = "/factions")
	public static void manage(@Nonnull State st, SafeMap values) {
		Form f = st.form();
		f.noForm();
		Table t = new Table();
		f.add(new TextHeader("Faction Management"));
		Set<CharacterGroup> factions = st.getInstance().getGroupsForKeyword("Faction");
		f.add(t);
		t.add(new HeaderRow().add("Name").add("Leader").add("Members"));
		for (CharacterGroup g : factions) {
			t.openRow().add(new Link(g.getName(), "/GPHUD/factions/view/" + g.getId()));
			String owner = "None";
			if (g.getOwner() != null) { owner = g.getOwner().asHtml(st, true); }
			t.add(owner);
			t.add(g.getMembers().size() + " members");
		}
		if (st.hasPermission("Faction.Create")) {
			Form create = new Form();
			f.add(create);
			create.setAction("./factions/create");
			create.add(new Hidden("okreturnurl", st.getFullURL()));
			create.add(new Button("Create Faction"));
		}
	}

	@URLs(url = "/factions/create", requiresPermission = "Faction.Create")
	public static void createForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Faction.create", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Create a faction group", requiresPermission = "Faction.Create")
	public static Response create(@Nonnull State st,
	                              @Arguments(type = ArgumentType.TEXT_CLEAN, description = "Name of the faction", max = 128)
			                              String name) {
		try { st.getInstance().createCharacterGroup(name, false, "Faction"); } catch (UserException e) {
			return new ErrorResponse("Failed to create faction: " + e.getMessage());
		}
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Create Faction", name, null, name, "Created new faction " + name);
		return new OKResponse("Successfully created faction group");
	}

	@URLs(url = "/factions/view/*")
	public static void viewFaction(@Nonnull State st, SafeMap values) throws SystemException {

		//System.out.println(st.uri);
		String[] split = st.getDebasedURL().split("/");
		//System.out.println(split.length);
		String id = split[split.length - 1];
		CharacterGroup faction = CharacterGroup.get(Integer.parseInt(id));
		if ("Faction".equals(faction.getType())) {
			viewFaction(st, values, faction);
		} else {
			st.form().add(new TextError("This is not a faction group"));
		}
	}

	public static void viewFaction(@Nonnull State st, SafeMap values, @Nonnull CharacterGroup faction) throws SystemException {
		Form f = st.form();
		f.noForm();
		f.add(new TextHeader(faction.getName()));
		Table t = new Table();
		f.add(t);
		String owner = "None";
		if (faction.getOwner() != null) {
			owner = faction.getOwner().asHtml(st, true);
		}
		Char factionowner = faction.getOwner();
		t.openRow().add("Owner").add(owner);
		if (st.hasPermission("Faction.SetOwner")) {
			Form setowner = new Form();
			setowner.setAction("../setowner");
			setowner.add(new Hidden("faction", faction.getName()));
			setowner.add(new Hidden("okreturnurl", st.getFullURL()));
			setowner.add(new Button("Set Owner", true));
			t.add(setowner);
		}
		Set<Char> members = faction.getMembers();
		t.openRow().add("Members").add(members.size() + " members");
		for (Char c : members) {
			t.openRow().add("").add(c);
			if (st.hasPermission("Faction.SetFaction")) {
				Form removeform = new Form();
				removeform.setAction("../remove");
				removeform.add(new Hidden("faction", faction.getName()));
				removeform.add(new Hidden("member", c.getName()));
				removeform.add(new Hidden("okreturnurl", st.getFullURL()));
				removeform.add(new Button("Kick Member", true));
				t.add(new Cell(removeform));
			}
			if (faction.isAdmin(c)) { t.add(new Cell(new Color("Red", "Admin"))); } else { t.add(""); }
			if (c == factionowner) { t.add(new Cell(new Color("Red", "Leader"))); } else { t.add(""); }
			if (c == factionowner || st.hasPermission("Faction.Create")) {
				t.add(new Form(st, true, "../setadmin", "Toggle Admin", "faction", faction.getName(), "character", c.getName(), "Admin", faction.isAdmin(c) ? "" : "true"));
			}
		}
		if (st.hasPermission("Faction.SetFaction")) {
			t.openRow().add(new Cell(new Form(st, true, "../add", "Add Member", "faction", faction.getName()), 2));
		}
		if (st.hasPermission("Faction.Delete")) {
			t.openRow().add(new Cell(new Form(st, false, "../delete", "Delete Faction", "faction", faction.getName()), 2));
		}
	}

	@URLs(url = "/factions/setowner", requiresPermission = "Faction.SetOwner")
	public static void setOwnerForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Faction.SetOwner", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Set the leader of a faction", requiresPermission = "Faction.SetOwner")
	public static Response setOwner(@Nonnull State st,
	                                @Nonnull @Arguments(description = "Faction to change the leader of", type = ArgumentType.CHARACTERGROUP)
			                                CharacterGroup faction,
	                                @Nullable @Arguments(description = "New leader, optionally", type = ArgumentType.CHARACTER, mandatory = false)
			                                Char newowner) {
		// group must be a faction group
		if (!"Faction".equals(faction.getType())) {
			return new ErrorResponse(faction.getName() + " is not a faction.");
		}
		Char oldowner = faction.getOwner();
		String oldownername = null;
		if (oldowner != null) { oldownername = oldowner.getName(); }
		if (newowner == oldowner) {
			if (newowner==null) { return new OKResponse("There is already no faction leader for this faction"); }
			return new OKResponse("That character (" + newowner.getName() + ") is already the faction leader");
		}
		if (newowner == null) {
			faction.setOwner(null);
			Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "SetOwner", faction.getName(), oldownername, null, "Faction leader removed");
			if (oldowner != null) {
				oldowner.hudMessage(("You are no longer the faction leader for " + faction.getName()));
			}
			return new OKResponse("Faction leader removed.");
		}
		// or a member
		boolean ingroup = false;
		for (Char c : faction.getMembers()) { if (c == newowner) { ingroup = true; }}
		if (!ingroup) {
			return new ErrorResponse("New leader " + newowner.getName() + " must be in faction " + faction.getName());
		}
		if (oldowner != null) {
			oldowner.hudMessage(("You are no longer the faction leader for " + faction.getName()));
		}
		newowner.hudMessage("You are now the faction leader for " + faction.getName());
		faction.setOwner(newowner);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, newowner, "SetOwner", faction.getName(), oldownername, newowner.getName(), "Faction leader changed");
		return new OKResponse("Faction leader updated");
	}

	@URLs(url = "/factions/add", requiresPermission = "Faction.SetFaction")
	public static void addForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Faction.Add", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Add a member to this faction", requiresPermission = "Faction.SetOwner")
	public static Response add(@Nonnull State st,
	                           @Nonnull @Arguments(type = ArgumentType.CHARACTERGROUP, description = "Faction to add character to")
			                           CharacterGroup faction,
	                           @Nonnull @Arguments(description = "Character to add to the faction", type = ArgumentType.CHARACTER)
			                           Char newmember) throws UserException {
		if (!"Faction".equals(faction.getType())) {
			return new ErrorResponse(faction.getName() + " is not a faction.");
		}
		CharacterGroup existingfaction = newmember.getGroup("Faction");
		// refuse if they're the faction leader
		//System.out.println("EXISTINGFACTION "+existingfaction.getId());
		if (existingfaction != null && existingfaction.getOwner() == newmember) {
			return new ErrorResponse("Refusing to move character " + newmember.getName() + ", they are currently faction leader of " + existingfaction.getName() + ", you must manually eject them from that position");
		}
		String success = "";
		if (existingfaction != null) { existingfaction.removeMember(newmember); }
		if (!success.isEmpty()) {
			return new ErrorResponse("Failed to remove " + newmember.getName() + " from their existing faction " + faction.getName() + " : " + success);
		}
		try { faction.addMember(newmember); } catch (UserException e) {
			return new ErrorResponse("Failed to add " + newmember.getName() + " to " + faction.getName() + ", they are probably in no faction now! - " + e.getMessage());
		}
		String oldfactionname = null;
		if (existingfaction != null) { oldfactionname = existingfaction.getName(); }
		String result = newmember.getName() + " is now in faction " + faction.getName();
		if (existingfaction != null) {
			result = newmember.getName() + " was moved into faction " + faction.getName() + " (was formerly in " + existingfaction.getName() + ")";
		}
		newmember.hudMessage("You have been added to the faction " + faction.getName());
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, newmember, "AddMember", faction.getName(), oldfactionname, faction.getName(), result);
		return new OKResponse(result);
	}

	@URLs(url = "/factions/remove", requiresPermission = "Faction.SetFaction")
	public static void removeForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Faction.Remove", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Remove a member from this faction", requiresPermission = "Faction.SetOwner")
	public static Response remove(@Nonnull State st,
	                              @Nonnull @Arguments(type = ArgumentType.CHARACTERGROUP, description = "Faction to remove character from")
			                              CharacterGroup faction,
	                              @Nonnull @Arguments(description = "Character to remove from the faction", type = ArgumentType.CHARACTER)
			                              Char member) {
		if (!"Faction".equals(faction.getType())) {
			return new ErrorResponse(faction.getName() + " is not a faction.");
		}
		CharacterGroup existingfaction = member.getGroup("Faction");
		// refuse if they're not in this group (!)
		if (existingfaction != faction) {
			return new ErrorResponse("Can not remove " + member.getName() + " from " + faction.getName() + " because they are instead in " + existingfaction.getName());
		}
		// refuse if they're the faction leader
		if (faction.getOwner() == member) {
			return new ErrorResponse("Will not remove " + member.getName() + " from " + faction.getName() + ", they are the faction leader, you must demote them by replacing them or leaving the faction leaderless.");
		}
		try { existingfaction.removeMember(member); } catch (UserException e) {
			return new ErrorResponse("Failed to remove member - " + e.getMessage());
		}
		member.hudMessage("You have been removed from faction " + faction.getName() + " by (( " + st.getAvatarNullable().getName() + " ))");
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, member, "RemoveMember", faction.getName(), faction.getName(), null, "Removed member from faction group");
		return new OKResponse(member.getName() + " was removed from their faction " + faction.getName());
	}

	@URLs(url = "/factions/delete", requiresPermission = "Faction.Delete")
	public static void deleteForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Faction.Delete", values);
	}

	@Nonnull
	@Commands(context = Context.AVATAR, description = "Delete a faction", requiresPermission = "Faction.Delete")
	public static Response delete(@Nonnull State st,
	                              @Nonnull @Arguments(description = "Faction to delete", type = ArgumentType.CHARACTERGROUP)
			                              CharacterGroup faction) {
		if (!"Faction".equals(faction.getType())) {
			return new ErrorResponse(faction.getName() + " is not a faction.");
		}
		String factionname = faction.getName();
		faction.delete();
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "DeleteFaction", factionname, factionname, null, "Faction was deleted");
		return new OKResponse(factionname + " was deleted");
	}

	@URLs(url = "/factions/setadmin")
	public static void setAdminForm(State st, SafeMap values) throws UserException, SystemException {
		Modules.simpleHtml(st, "Faction.SetAdmin", values);
	}

	@Nonnull
	@Commands(context = Context.ANY, description = "Set the faction admin flag on a user")
	public static Response setAdmin(@Nonnull State st,
	                                @Nonnull @Arguments(description = "Faction to set the character's admin flag on", type = ArgumentType.CHARACTERGROUP)
			                                CharacterGroup faction,
	                                @Nonnull @Arguments(description = "Character to set the admin flag on", type = ArgumentType.CHARACTER)
			                                Char character,
	                                @Arguments(description = "Admin flag to set on the character in this faction", type = ArgumentType.BOOLEAN)
			                                boolean admin) throws SystemException {
		if (!"Faction".equals(faction.getType())) {
			return new ErrorResponse(faction.getName() + " is not a faction.");
		}
		if (!faction.hasMember(character)) {
			return new ErrorResponse(character.getName() + " is not a member of faction " + faction.getName());
		}
		// must be instance admin or faction owner
		boolean ok = false;
		if (faction.getOwner() == st.getCharacter()) { ok = true; }
		if (!ok && st.hasPermission("Faction.Create")) {ok = true; }
		if (!ok) {
			return new ErrorResponse("You must be faction owner, or have Faction.Create permissions to set admin flags");
		}
		boolean oldflag = faction.isAdmin(character);
		faction.setAdmin(character, admin);
		if (admin) {
			character.hudMessage("You are now a faction administrator for " + faction.getName());
		} else {
			character.hudMessage("You are no longer a faction administrator for " + faction.getName());
		}
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, character, "SetAdmin", faction.getName(), oldflag + "", admin + "", "Set admin flag");
		return new OKResponse("Successfully altered admin flag on " + character + " in " + faction);
	}


}
