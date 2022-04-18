package net.coagulate.GPHUD.Modules.PermissionsGroups;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.PermissionsGroup;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.Hidden;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.TabularResponse;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author iain
 */
public abstract class Groups {
	public static final String RED="#ffdfdf";
	public static final String YELLOW="#ffffdf";
	public static final String GREEN="#dfffdf";

	// ---------- STATICS ----------
	@URLs(url="/permissionsgroups/view/*")
	public static void view(@Nonnull final State st,
	                        final SafeMap values) {
		final Form f=st.form();
		f.noForm();
		final String[] split=st.getDebasedURL().split("/");
		final String id=split[split.length-1];
		final PermissionsGroup pg=PermissionsGroup.get(Integer.parseInt(id));
		pg.validate(st);
		f.add(new TextHeader(pg.getName()));
		final Set<String> permissions=pg.getPermissions(st);
		final Table permtable=new Table();
		f.add(new TextSubHeader("Permissions"));
		if (!permissions.isEmpty()) { f.add(permtable); }
		permtable.add(new HeaderRow().add("Permission").add("Description"));
		for (final String permission: permissions) {
			permtable.openRow();
			permtable.add(permission);
			try {
				final Permission rawpermission = Modules.getPermission(st, permission);
				if (rawpermission == null) {
					permtable.add("<font color=red><i>Permission no longer exists</i></font>");
				} else {
					permtable.add(rawpermission.description());
					permtable.setBGColor(rawpermission.getColor());
				}
			}
			catch (@Nonnull final UserException permissionerror) {
				permtable.add("Error: "+permissionerror.getLocalizedMessage());
				permtable.setBGColor("#e0e0e0");
			}
			if (st.hasPermission("instance.managepermissions")) {
				final Form dp=new Form();
				dp.setAction("../delpermission");
				dp.add(new Hidden("permissionsgroup", pg.getName()));
				dp.add(new Hidden("Permission", permission));
				dp.add(new Hidden("okreturnurl",st.getFullURL()));
				dp.add(new Button("Delete Permission",true));
				permtable.add(dp);
			}
		}
		if (st.hasPermission("instance.managepermissions")) {
			f.add("<a href=\"/GPHUD/permissionsgroups/edit/"+pg.getId()+"\">Edit Permissions</a>");
		}
		f.add(new TextSubHeader("Members"));
		final Set<PermissionsGroup.PermissionsGroupMembership> members=pg.getMembers();
		final Table membertable=new Table();
		membertable.add(new HeaderRow().add("Member").add("Can Invite").add("Can Kick"));
		if (!members.isEmpty()) { f.add(membertable); }
		final boolean caneject=pg.canEject(st);
		for (final PermissionsGroup.PermissionsGroupMembership member: members) {
			membertable.openRow();
			membertable.add(member.avatar.getGPHUDLink());
			if (member.caninvite) { membertable.add(new Color("green","Yes")); }
			else {
				membertable.add(new Color("red","No"));
			}
			if (member.cankick) { membertable.add(new Color("green","Yes")); }
			else {
				membertable.add(new Color("red","No"));
			}
			if (st.hasPermission("instance.managepermissions")) {
				final Form spf=new Form();
				spf.add(new Hidden("okreturnurl",st.getFullURL()));
				membertable.add(spf);
				spf.setAction("../setpermissions");
				spf.add(new Hidden("permissionsgroup", pg.getName()));
				spf.add(new Hidden("Avatar", member.avatar.getName()));
				if (member.caninvite) { spf.add(new Hidden("CanInvite","on")); }
				if (member.cankick) { spf.add(new Hidden("CanKick","on")); }
				spf.add(new Button("Set Invite/Kick"));
			}
			if (caneject) {
				final Form ef=new Form();
				ef.add(new Hidden("okreturnurl",st.getFullURL()));
				ef.setAction("../eject");
				ef.add(new Hidden("permissionsgroup", pg.getName()));
				ef.add(new Hidden("Avatar", member.avatar.getName()));
				ef.add(new Button("Eject"));
				membertable.add(ef);
			}
		}
		if (pg.canInvite(st)) {
			final Form invite=new Form();
			invite.add(new Hidden("okreturnurl",st.getFullURL()));
			invite.setAction("../invite");
			invite.add(new Hidden("permissionsgroup", pg.getName()));
			invite.add(new Button("Add Member"));
			f.add(invite);
		}
		if (st.hasPermission("instance.managepermissions")) {
			final Form df=new Form();
			df.setAction("../delete");
			df.add(new Hidden("permissionsgroup", pg.getName()));
			df.add(new Button("DELETE GROUP"));
			f.add(new Separator());
			f.add(df);
		}
	}


	@URLs(url="/permissionsgroups/create",
	      requiresPermission="Instance.ManagePermissions")
	@SideSubMenus(name="Create",
	              priority=10,
				  requiresPermission = "Instance.ManagePermissions")
	public static void createGroupPage(@Nonnull final State st,
	                                   @Nonnull final SafeMap values) {
		if ("Submit".equals(values.get("Submit"))) { st.form().add(Modules.run(st,"permissionsgroups.create",values)); }
		Modules.getHtmlTemplate(st,"permissionsgroups.create");
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Creates a new permissions group",
	          requiresPermission="Instance.ManagePermissions",
	          permitScripting=false,
	          permitExternal=false,
	          permitObject=false)
	public static Response create(@Nonnull final State st,
	                              @Arguments(name="name",description="Name of group to create",
	                                         type=ArgumentType.TEXT_CLEAN,
	                                         max=64) final String name) {
		try { PermissionsGroup.create(st,name); }
		catch (@Nonnull final UserException e) {
			return new ErrorResponse("Failed to create permissions group - "+e.getLocalizedMessage());
		}
		return new OKResponse("Permissions Group created successfully!");
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="List permissions groups present at this instance",
	          permitObject=false,
	          permitScripting=false)
	public static Response list(@Nonnull final State st) {
		final TabularResponse r=new TabularResponse("Permissions groups");
		final Set<PermissionsGroup> groups=PermissionsGroup.getPermissionsGroups(st);
		for (final PermissionsGroup p: groups) {
			r.openRow();
			r.add(p);
		}
		return r;
	}

	@URLs(url="/permissionsgroups/")
	public static void listForm(@Nonnull final State st,
	                            @Nonnull final SafeMap values) {
		st.form().add(Modules.run(st,"permissionsgroups.list",values));
	}

	@URLs(url="/permissionsgroups/delete",
	      requiresPermission="Instance.ManagePermissions")
	@SideSubMenus(name="Delete",
	              priority=11,
	              requiresPermission="Instance.ManagePermissions")
	public static void deleteForm(@Nonnull final State st,
	                              @Nonnull final SafeMap values) {
		if ("Submit".equals(values.get("Submit"))) { st.form().add(Modules.run(st,"permissionsgroups.delete",values)); }
		Modules.getHtmlTemplate(st,"permissionsgroups.delete");
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          requiresPermission="Instance.ManagePermissions",
	          description="Deletes a permissions group",
	          permitExternal=false,
	          permitScripting=false,
	          permitObject=false)
	public static Response delete(@Nonnull final State st,
	                              @Nonnull @Arguments(name="permissionsgroup",description="Permissions group to delete",
	                                                  type=ArgumentType.PERMISSIONSGROUP) final PermissionsGroup permissionsgroup) {
		final String success="NOP";
		permissionsgroup.validate(st);
		final String name=permissionsgroup.getNameSafe();
		permissionsgroup.delete();
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"DELETE","AvatarGroup",name,null,"Avatar deleted permissions group");
		return new OKResponse("Deleted permissions group "+name);
	}

	@URLs(url="/permissionsgroups/edit/*",
	      requiresPermission="Instance.ManagePermissions")
	public static void editPermissions(@Nonnull final State st,
	                                   @Nonnull final SafeMap values) {
		final String[] split=st.getDebasedNoQueryURL().split("/");
		if (split.length!=4) {
			throw new UserInputValidationParseException("Incorrect number of query parameters ("+split.length+")");
		}
		final int groupid=Integer.parseInt(split[3]);
		final PermissionsGroup pg=PermissionsGroup.get(groupid);
		pg.validate(st);
		final Form f=st.form();
		f.add(new TextHeader("Edit permissions group "+pg.getName()));
		// this is all a bit tedious really :)
		final Map<Permission.POWER,Set<Permission>> permissions=new HashMap<>();
		permissions.put(Permission.POWER.LOW,new HashSet<>());
		permissions.put(Permission.POWER.MEDIUM,new HashSet<>());
		permissions.put(Permission.POWER.HIGH,new HashSet<>());
		permissions.put(Permission.POWER.UNKNOWN,new HashSet<>());
		for (final Module module: Modules.getModules()) {
			if (module.isEnabled(st)) {
				for (final Permission permission: module.getPermissions(st).values()) {
					if (permission.grantable()) {
						permissions.get(permission.power()).add(permission);
						if (values.containsKey("SET")) {
							stampPermission(st,pg,module,permission,values.get(permission.getModule(st).getName()+"."+permission.name()));
						}
					}
				}
			}
		}
		st.flushPermissionsGroupCache();
		st.flushPermissionsCache();
		String table="<table border=0><tr>";
		table+="<tr bgcolor="+GREEN+"><th colspan=99>LOW - permissions that edit stuff you can usually undo</th></tr>";
		table+=addPermissions(st,permissions.get(Permission.POWER.LOW),pg,GREEN);
		table+="<tr bgcolor="+YELLOW+"><th colspan=99>MEDIUM - permissions that edit stuff that has significant effect or can be hard to undo</th></tr>";
		table+=addPermissions(st,permissions.get(Permission.POWER.MEDIUM),pg,YELLOW);
		table+="<tr bgcolor="+RED+"><th colspan=99>HIGH - permissions that can change global things, prevent the instance working, or destroy data</th></tr>";
		table+=addPermissions(st,permissions.get(Permission.POWER.HIGH),pg,RED);
		table+="<tr><th colspan=99><input type=submit name=SET value=SET></th></tr>";
		table+="</table>";
		f.add(table);
	}

	// ----- Internal Statics -----
	private static void stampPermission(@Nonnull final State st,
	                                    @Nonnull final PermissionsGroup pg,
	                                    final Module module,
	                                    @Nonnull final Permission permission,
	                                    @Nonnull final String s) {
		final boolean set=!s.isEmpty();
		final String name=permission.getModule(st).getName()+"."+permission.name();
		if (pg.hasPermission(st,name)) {
			if (!set) {
				pg.removePermission(name);
				Audit.audit(true,
				            st,
				            Audit.OPERATOR.AVATAR,
				            null,
				            null,
				            "RemovePermission",
				            pg.getName(),
				            name,
				            null,
				            "Removed permission "+name+" from permissions group "+pg.getName()
				           );
			}
		}
		else {
			if (set) {
				pg.addPermission(st,name);
				Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"AddPermission",pg.getName(),null,name,"Add permission "+name+" to permissions group "+pg.getName());
			}
		}
	}

	@Nonnull
	private static String addPermissions(@Nonnull final State st,
	                                     @Nonnull final Set<Permission> permissions,
	                                     @Nonnull final PermissionsGroup pg,
	                                     final String col) {
		final StringBuilder r=new StringBuilder();
		final TreeMap<String,Permission> sorted=new TreeMap<>();
		for (final Permission p: permissions) {
			try {
                sorted.put(p.getModule(st).getName() + "." + p.name(), p);
            } catch (final NoDataException ignored) {
            } // badly cached attribute
		}
		for (final Permission p: sorted.values()) {
			final String fullname=p.getModule(st).getName()+"."+p.name();
			boolean exists=false;
			final Permission a=Modules.get(st,Modules.extractModule(fullname)).getPermission(st,Modules.extractReference(fullname));
			if (a!=null) { exists=true; }
			r.append("<tr bgcolor=")
			 .append(col)
			 .append("><td>")
			 .append(fullname)
			 .append("</td><td>")
			 .append(p.description())
			 .append("</td><td>")
			 .append("<input type=checkbox name=\"")
			 .append(fullname)
			 .append("\" value=\"")
			 .append(fullname)
			 .append("\" ")
			 .append(pg.hasPermission(st,fullname)?"checked":"")
			 .append(">")
			 .append("</td><td>")
			 .append(exists?"":"Doesn't Exist")
			 .append("</td></tr>");
		}
		return r.toString();
	}
}
