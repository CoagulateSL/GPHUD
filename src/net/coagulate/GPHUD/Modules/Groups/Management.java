package net.coagulate.GPHUD.Modules.Groups;

import net.coagulate.Core.Exceptions.UserException;
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
	// ---------- STATICS ----------
	@URLs(url="/groups")
	public static void manage(@Nonnull final State st,
	                          final SafeMap values) {
		manage(st,values,null);
	}

	public static void manage(@Nonnull final State st,
							  @SuppressWarnings("unused") final SafeMap values,
							  @Nullable final String typeFilter) {
		final Form f=st.form();
		f.noForm();
		final Table t=new Table();
		f.add(new TextHeader("Group Management"));
		if (typeFilter!=null) { f.add(new TextSubHeader("Filtered by type: "+typeFilter)); }
		final List<CharacterGroup> groups=st.getInstance().getCharacterGroups();
		f.add(t);
		final HeaderRow hr=new HeaderRow();
		t.add(hr);
		hr.add("Name");
		if (typeFilter==null) { hr.add("Type"); }
		hr.add("Owner");
		hr.add("Open");
		hr.add("Members");
		hr.add("KV Precedence");
		for (final CharacterGroup g: groups) {
			String keyword=g.getType();
			if (keyword==null) { keyword=""; }
			if (typeFilter==null || typeFilter.equalsIgnoreCase(keyword)) {
				t.openRow();
				t.add(new Link(g.getName(),"/GPHUD/groups/view/"+g.getId()));
				String owner="";
				if (g.getOwner()!=null) { owner=g.getOwner().asHtml(st,true); }
				if (typeFilter==null) { t.add(keyword); }
				t.add(owner);
				if (g.isOpen()) { t.add("Yes"); }
				else { t.add(""); }
				t.add(g.getMembers().size()+" members");
				t.add(g.getKVPrecedence());
			}
		}
		if (st.hasPermission("Groups.Create")) {
			final Form create=new Form();
			f.add(create);
			create.setAction("/GPHUD/groups/create");
			create.add(new Hidden("okreturnurl",st.getFullURL()));
			create.add(new Button("Create Group"));
		}
	}

	@URLs(url="/groups/type/*")
	public static void manageType(@Nonnull final State st,
	                              final SafeMap values) {
		final String[] comps=st.getDebasedURL().split("/");
		String type=comps[comps.length-1];
		if ("BLANK".equals(type)) { type=""; }
		manage(st,values,type);
	}


	@Nonnull
	public static List<String> groupTypes(@Nonnull final State st) {
		final List<String> ret=new ArrayList<>();
		ret.add("");
		ret.addAll(st.getCharacterGroupTypes());
		return ret;
	}

	@URLs(url="/groups/create",
	      requiresPermission="Groups.Create")
	public static void createForm(@Nonnull final State st,
	                              @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Groups.create",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Create a group",
	          requiresPermission="Groups.Create",
	          permitExternal=false,
	          permitObject=false,
	          permitScripting=false)
	public static Response create(@Nonnull final State st,
	                              @Arguments(type=ArgumentType.TEXT_CLEAN,
	                                         name="name",description="Name of the group",
	                                         max=128) final String name,
	                              @Arguments(type=ArgumentType.CHOICE,
	                                         name="type",description="Type of the group",
	                                         mandatory=false,
	                                         choiceMethod="net.coagulate.GPHUD.Modules.Groups.Management.groupTypes") final String type) {
		try { st.getInstance().createCharacterGroup(name,false,type); }
		catch (@Nonnull final UserException e) {
			return new ErrorResponse("Failed to create group: "+e.getMessage());
		}
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Create Group",type,null,name,"Created new group "+name+" of type "+type);
		return new OKResponse("Successfully created group");
	}

	@URLs(url="/groups/view/*")
	public static void viewGroup(@Nonnull final State st,
	                             final SafeMap values) {

		//System.out.println(st.uri);
		final String[] split=st.getDebasedURL().split("/");
		//System.out.println(split.length);
		final String id=split[split.length-1];
		final CharacterGroup group=CharacterGroup.get(Integer.parseInt(id));
		viewGroup(st,values,group);
	}

	public static void viewGroup(@Nonnull final State st,
	                             final SafeMap values,
	                             @Nonnull final CharacterGroup group) {
		final Form f=st.form();
		f.noForm();
		f.add(new TextHeader(group.getName()));
		final Table t=new Table();
		f.add(t);
		String owner="None";
		if (group.getOwner()!=null) {
			owner=group.getOwner().asHtml(st,true);
		}
		final Char groupOwner=group.getOwner();
		t.openRow().add("Owner").add(owner);
		if (st.hasPermission("Groups.SetOwner")) {
			final Form setOwner=new Form();
			setOwner.setAction("../setowner");
			setOwner.add(new Hidden("group",group.getName()));
			setOwner.add(new Hidden("okreturnurl",st.getFullURL()));
			setOwner.add(new Button("Set Owner",true));
			t.add(setOwner);
		}
		t.openRow().add("KV Precedence").add(group.getKVPrecedence());
		if (st.hasPermission("Groups.SetPrecedence")) {
			final Form setPrecedence=new Form();
			setPrecedence.setAction("../setprecedence");
			setPrecedence.add(new Hidden("group",group.getName()));
			setPrecedence.add(new Hidden("precedence",""+group.getKVPrecedence()));
			setPrecedence.add(new Hidden("okreturnurl",st.getFullURL()));
			setPrecedence.add(new Button("Set Precedence",true));
			t.add(setPrecedence);
		}
		final Set<Char> members=group.getMembers();
		t.openRow().add("Members").add(members.size()+" members");
		t.openRow().add("Open").add(group.isOpen()?"Yes":"No");
		if (st.hasPermission("Groups.SetOpen")) {
			t.add(new Form(st,true,"../setopen",group.isOpen()?"Close":"Open","group",group.getName(),"open",group.isOpen()?"1":""));
		}
		for (final Char c: members) {
			t.openRow().add("").add(c);
			if (st.hasPermission("Groups.SetGroup")) {
				final Form removeForm=new Form();
				removeForm.setAction("../remove");
				removeForm.add(new Hidden("group",group.getName()));
				removeForm.add(new Hidden("member",c.getName()));
				removeForm.add(new Hidden("okreturnurl","./view/"+group.getId()));
				removeForm.add(new Button("Kick Member",true));
				t.add(new Cell(removeForm));
			}
			if (group.isAdmin(c)) { t.add(new Cell(new Color("Red","Admin"))); }
			else { t.add(""); }
			if (c==groupOwner) { t.add(new Cell(new Color("Red","Leader"))); }
			else { t.add(""); }
			if (c==groupOwner || st.hasPermission("Groups.Create")) {
				t.add(new Form(st,true,"../setadmin","Toggle Admin","group",group.getName(),"character",c.getName(),"Admin",group.isAdmin(c)?"":"true"));
			}
		}
		if (st.hasPermission("Groups.SetGroup")) {
			t.openRow().add(new Cell(new Form(st,true,"../add","Add Member","group",group.getName()),2));
		}
		if (st.hasPermission("Groups.Delete")) {
			t.openRow().add(new Cell(new Form(st,false,"../delete","Delete Groups","group",group.getName()),2));
		}
		f.add(new TextSubHeader("KV influences"));
		GenericConfiguration.page(st,values,group,st);
	}

	@URLs(url="/groups/setowner",
	      requiresPermission="Groups.SetOwner")
	public static void setOwnerForm(@Nonnull final State st,
	                                @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Groups.SetOwner",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Set the leader of a group",
	          requiresPermission="Groups.SetOwner")
	public static Response setOwner(@Nonnull final State st,
	                                @Nonnull @Arguments(name="group",description="Group to change the leader of",
	                                                    type=ArgumentType.CHARACTERGROUP) final CharacterGroup group,
	                                @Nullable @Arguments(name="newowner",description="New leader, optionally",
	                                                     type=ArgumentType.CHARACTER,
	                                                     mandatory=false) final Char newOwner) {
		final Char oldOwner=group.getOwner();
		String oldOwnerName=null;
		if (oldOwner!=null) { oldOwnerName=oldOwner.getName(); }
		if (newOwner==oldOwner) {
			if (newOwner==null) { return new OKResponse("That group already has no leader"); }
			return new OKResponse("That character ("+newOwner.getName()+") is already the group leader");
		}
		if (newOwner==null) {
			group.setOwner(null);
			Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SetOwner",group.getName(),oldOwnerName,null,"Group leader removed");
			// following is always true otherwise newowner(from this if) and oldowner(from the following) are null which is handled by "that group already has no leader"
			//noinspection ConstantConditions
			if (oldOwner!=null) {
				oldOwner.hudMessage(("You are no longer the group leader for "+group.getName()));
			}
			return new OKResponse("Group leader removed.");
		}
		// or a member
		boolean inGroup=false;
		for (final Char c: group.getMembers()) {
			if (c == newOwner) {
				inGroup = true;
				break;
			}}
		if (!inGroup) {
			return new ErrorResponse("New leader "+newOwner.getName()+" must be in group "+group.getName());
		}
		if (oldOwner!=null) { oldOwner.hudMessage(("You are no longer the group leader for "+group.getName())); }
		newOwner.hudMessage("You are now the group leader for "+group.getName());
		group.setOwner(newOwner);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,newOwner,"SetOwner",group.getName(),oldOwnerName,newOwner.getName(),"Group leader changed");
		return new OKResponse("Group leader updated");
	}

	@URLs(url="/groups/setprecedence",
		  requiresPermission="Groups.SetPrecedence")
	public static void setPrecedenceForm(@Nonnull final State st,
									@Nonnull final SafeMap values) {
		Modules.simpleHtml(st, "Groups.SetPrecedence", values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
			  description="Set the KV precedence value of a character group",
			  requiresPermission="Groups.SetPrecedence")
	public static Response setPrecedence(@Nonnull final State st,
									@Nonnull @Arguments(name="group",description="Group to change the leader of",
														type=ArgumentType.CHARACTERGROUP) final CharacterGroup group,
									@Nonnull @Arguments(name="precedence",description="New precedence",
														 type=ArgumentType.INTEGER) Integer precedence) {
		int oldPrecedence=group.getKVPrecedence();
		if (oldPrecedence==precedence) { return new OKResponse("Precedence unchanged"); }
		group.setKVPrecedence(precedence);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SetPrecedence",group.getName(),""+oldPrecedence,""+precedence,"Changed KV Precedence from "+oldPrecedence+" to "+precedence);
		CharacterGroup.purgeCharacterGroupPrecedenceCaches();
		st.getInstance().pushConveyances();
		return new OKResponse("Precedence updated");
	}

	@URLs(url="/groups/add",
	      requiresPermission="Groups.SetGroup")
	public static void addForm(@Nonnull final State st,
	                           @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Groups.Add",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Add a member to this group",
	          requiresPermission="Groups.SetGroup")
	public static Response add(@Nonnull final State st,
	                           @Nonnull @Arguments(type=ArgumentType.CHARACTERGROUP,
	                                               name="group",description="Group to add character to") final CharacterGroup group,
	                           @Nonnull @Arguments(name="newmember",description="Character to add to the group",
	                                               type=ArgumentType.CHARACTER) final Char newMember) {
		return add(st,group,newMember,true);
	}
	@Nonnull
	@Commands(context=Context.AVATAR,
			  description="Add a member to this group.  Does not notify target of group change.",
			  requiresPermission="Groups.SetGroup")
	public static Response addSilently(@Nonnull final State st,
							   @Nonnull @Arguments(type=ArgumentType.CHARACTERGROUP,
												   name="group",description="Group to add character to") final CharacterGroup group,
							   @Nonnull @Arguments(name="newmember",description="Character to add to the group",
												   type=ArgumentType.CHARACTER) final Char newMember) {
		return add(st,group,newMember,false);
	}
	private static Response add(@Nonnull final State st,final CharacterGroup group,final Char newMember,boolean notify) {
		final Attribute attr=st.getAttribute(group);
		String groupType=null;
		if (attr!=null) {
			groupType=attr.getSubType();
		}
		CharacterGroup existingGroup=null;
		if (groupType!=null) { existingGroup=CharacterGroup.getGroup(newMember,groupType); }
		if (existingGroup!=null && existingGroup.getOwner()==newMember) {
			return new ErrorResponse("Refusing to move character "+newMember.getName()+", they are currently group leader of "+existingGroup.getName()+", you must manually "+"eject them from that position");
		}
		if (existingGroup!=null) { existingGroup.removeMember(newMember); }
		try { group.addMember(newMember); }
		catch (@Nonnull final UserException e) {
			return new ErrorResponse("Failed to add "+newMember.getName()+" to "+group.getName()+", they are probably in no group now! - "+e.getMessage());
		}
		String oldGroupName=null;
		if (existingGroup!=null) { oldGroupName=existingGroup.getName(); }
		String result=newMember.getName()+" is now in group "+group.getName();
		if (existingGroup!=null) {
			result=newMember.getName()+" was moved into group "+group.getName()+" (was formerly in "+existingGroup.getName()+")";
		}
		if (notify) { newMember.hudMessage("You have been added to the group "+group.getName()); }
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,newMember,"AddMember",group.getName(),oldGroupName,group.getName(),result);
		return new OKResponse(result);
	}

	@URLs(url="/groups/remove",
	      requiresPermission="Groups.SetGroup")
	public static void removeForm(@Nonnull final State st,
	                              @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Groups.Remove",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Remove a member from this group",
	          requiresPermission="Groups.SetGroup")
	public static Response remove(@Nonnull final State st,
	                              @Nonnull @Arguments(type=ArgumentType.CHARACTERGROUP,
	                                                  name="group",description="Group to remove character from") final CharacterGroup group,
	                              @Nonnull @Arguments(name="member",description="Character to remove from the group",
	                                                  type=ArgumentType.CHARACTER) final Char member) {
		return remove(st,group,member,true);
	}
	@Nonnull
	@Commands(context=Context.AVATAR,
			  description="Remove a member from this group without notifying them of the change",
			  requiresPermission="Groups.SetGroup")
	public static Response removeSilently(@Nonnull final State st,
								  @Nonnull @Arguments(type=ArgumentType.CHARACTERGROUP,
													  name="group",description="Group to remove character from") final CharacterGroup group,
								  @Nonnull @Arguments(name="member",description="Character to remove from the group",
													  type=ArgumentType.CHARACTER) final Char member) {
		return remove(st,group,member,false);
	}

	private static Response remove(@Nonnull final State st, final CharacterGroup group, final Char member, boolean notify) {
		if (group.getOwner()==member) {
			return new ErrorResponse("Will not remove "+member.getName()+" from "+group.getName()+", they are the group leader, you must demote them by replacing them or "+"leaving the group leaderless.");
		}
		try { group.removeMember(member); }
		catch (@Nonnull final UserException e) {
			return new ErrorResponse("Failed to remove member - "+e.getMessage());
		}
		if (notify) { member.hudMessage("You have been removed from group "+group.getName()); }
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,member,"RemoveMember",group.getName(),group.getName(),null,"Removed member from group");
		return new OKResponse(member.getName()+" was removed from group "+group.getName());
	}

	@URLs(url="/groups/delete",
	      requiresPermission="Groups.Delete")
	public static void deleteForm(@Nonnull final State st,
	                              @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Groups.Delete",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Delete a group",
	          requiresPermission="Groups.Delete",
	          permitObject=false,
	          permitScripting=false,
	          permitExternal=false)
	public static Response delete(@Nonnull final State st,
	                              @Nonnull @Arguments(name="group",description="Group to delete",
	                                                  type=ArgumentType.CHARACTERGROUP) final CharacterGroup group) {
		final String groupName=group.getName();
		group.delete();
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"DeleteGroup",groupName,groupName,null,"Group was deleted");
		return new OKResponse(groupName+" was deleted");
	}

	@URLs(url="/groups/setadmin")
	public static void setAdminForm(@Nonnull final State st,
	                                @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Groups.SetAdmin",values);
	}

	@Nonnull
	@Commands(context=Context.ANY,
	          description="Set the groups admin flag on a user")
	public static Response setAdmin(@Nonnull final State st,
	                                @Nonnull @Arguments(name="group",description="Group to set the character's admin flag on",
	                                                    type=ArgumentType.CHARACTERGROUP) final CharacterGroup group,
	                                @Nonnull @Arguments(name="character",description="Character to set the admin flag on",
	                                                    type=ArgumentType.CHARACTER) final Char character,
	                                @Arguments(name="admin",description="Admin flag to set on the character in this group",
	                                           type=ArgumentType.BOOLEAN) final boolean admin) {
		if (!group.hasMember(character)) {
			return new ErrorResponse(character.getName()+" is not a member of group "+group.getName());
		}
		// must be instance admin or group owner
		boolean ok=false;
		if (group.getOwner()==st.getCharacter()) { ok=true; }
		if (!ok && st.hasPermission("Groups.Create")) {ok=true; }
		if (!ok) {
			return new ErrorResponse("You must be group owner, or have Groups.Create permissions to set admin flags");
		}
		final boolean oldFlag=group.isAdmin(character);
		group.setAdmin(character,admin);
		if (admin) {
			character.hudMessage("You are now a group administrator for "+group.getName());
		}
		else {
			character.hudMessage("You are no longer a group administrator for "+group.getName());
		}
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,character,"SetAdmin",group.getName(),oldFlag+"",admin+"","Set admin flag");
		return new OKResponse("Successfully altered admin flag on "+character+" in "+group);
	}

	@URLs(url="/groups/setopen")
	public static void setOpenForm(@Nonnull final State st,
	                               @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Groups.SetOpen",values);
	}

	@Nonnull
	@Commands(context=Context.ANY,
	          description="Set the groups open flag",
	          requiresPermission="Groups.SetOpen")
	public static Response setOpen(@Nonnull final State st,
	                               @Nonnull @Arguments(name="group",description="Group to modify",
	                                                   type=ArgumentType.CHARACTERGROUP) final CharacterGroup group,
	                               @Arguments(name="open",description="Group open?",
	                                          type=ArgumentType.BOOLEAN) final boolean open) {
		group.validate(st);
		// must be instance admin or group owner
		final boolean oldFlag=group.isOpen();
		group.setOpen(open);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SetOpen",group.getName(),oldFlag+"",open+"","Set open flag");
		return new OKResponse("Successfully set open flag to "+open+" on group "+group.getName());
	}

}
