package net.coagulate.GPHUD.Modules.PermissionsGroups;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.PermissionsGroup;
import net.coagulate.GPHUD.Data.PermissionsGroupMembership;
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

import java.util.*;

/**
 * @author iain
 */
public abstract class Groups {
	public static final String RED="#ffdfdf";
	public static final String YELLOW="#ffffdf";
	public static final String GREEN="#dfffdf";

	@URLs(url = "/permissionsgroups/view/*")
	public static void view(State st, SafeMap values) throws UserException, SystemException {
		Form f = st.form;
		f.noForm();
		String[] split = st.getDebasedURL().split("/");
		String id = split[split.length - 1];
		PermissionsGroup pg = PermissionsGroup.get(Integer.parseInt(id));
		pg.validate(st);
		f.add(new TextHeader(pg.getName()));
		Set<String> permissions = pg.getPermissions(st);
		Table permtable = new Table();
		f.add(new TextSubHeader("Permissions"));
		if (!permissions.isEmpty()) { f.add(permtable); }
		permtable.add(new HeaderRow().add("Permission").add("Description"));
		for (String permission : permissions) {
			permtable.openRow();
			permtable.add(permission);
			try {
				Permission rawpermission = Modules.getPermission(st, permission);
				if (rawpermission != null) {
					permtable.add(rawpermission.description());
					permtable.setBGColor(rawpermission.getColor());
				} else {
					permtable.add("<font color=red><i>Permission no longer exists</i></font>");
				}
			} catch (UserException permissionerror) {
				permtable.add("Error: "+permissionerror.getLocalizedMessage()); permtable.setBGColor("#e0e0e0");
			}
			if (st.hasPermission("instance.managepermissions")) {
				Form dp = new Form();
				dp.setAction("../delpermission");
				dp.add(new Hidden("permissionsgroup", "" + pg.getName()));
				dp.add(new Hidden("Permission", permission));
				dp.add(new Hidden("okreturnurl", st.getFullURL()));
				dp.add(new Button("Delete Permission", true));
				permtable.add(dp);
			}
		}
		if (st.hasPermission("instance.managepermissions")) {
			f.add("<a href=\"/GPHUD/permissionsgroups/edit/"+pg.getId()+"\">Edit Permissions</a>");
		}
		f.add(new TextSubHeader("Members"));
		Set<PermissionsGroupMembership> members = pg.getMembers();
		Table membertable = new Table();
		membertable.add(new HeaderRow().add("Member").add("Can Invite").add("Can Kick"));
		if (!members.isEmpty()) { f.add(membertable); }
		boolean caneject = pg.canEject(st);
		for (PermissionsGroupMembership member : members) {
			membertable.openRow();
			membertable.add(member.avatar.getGPHUDLink());
			if (member.caninvite) { membertable.add(new Color("green", "Yes")); } else {
				membertable.add(new Color("red", "No"));
			}
			if (member.cankick) { membertable.add(new Color("green", "Yes")); } else {
				membertable.add(new Color("red", "No"));
			}
			if (st.hasPermission("instance.managepermissions")) {
				Form spf = new Form();
				spf.add(new Hidden("okreturnurl", st.getFullURL()));
				membertable.add(spf);
				spf.setAction("../setpermissions");
				spf.add(new Hidden("permissionsgroup", "" + pg.getName()));
				spf.add(new Hidden("Avatar", member.avatar.getName()));
				if (member.caninvite) { spf.add(new Hidden("CanInvite", "on")); }
				if (member.cankick) { spf.add(new Hidden("CanKick", "on")); }
				spf.add(new Button("Set Invite/Kick"));
			}
			if (caneject) {
				Form ef = new Form();
				ef.add(new Hidden("okreturnurl", st.getFullURL()));
				ef.setAction("../eject");
				ef.add(new Hidden("permissionsgroup", "" + pg.getName()));
				ef.add(new Hidden("Avatar", member.avatar.getName()));
				ef.add(new Button("Eject"));
				membertable.add(ef);
			}
		}
		if (pg.canInvite(st)) {
			Form invite = new Form();
			invite.add(new Hidden("okreturnurl", st.getFullURL()));
			invite.setAction("../invite");
			invite.add(new Hidden("permissionsgroup", "" + pg.getName()));
			invite.add(new Button("Add Member"));
			f.add(invite);
		}
		if (st.hasPermission("instance.managepermissions")) {
			Form df = new Form();
			df.setAction("../delete");
			df.add(new Hidden("permissionsgroup", "" + pg.getName()));
			df.add(new Button("DELETE GROUP"));
			f.add(new Separator());
			f.add(df);
		}
	}


	@URLs(url = "/permissionsgroups/create", requiresPermission = "Instance.ManagePermissions")
	@SideSubMenus(name = "Create", priority = 10)
	public static void createGroupPage(State st, SafeMap values) throws UserException, SystemException {
		if ("Submit".equals(values.get("Submit"))) { st.form.add(Modules.run(st, "permissionsgroups.create", values)); }
		Modules.getHtmlTemplate(st, "permissionsgroups.create");
	}

	@Commands(context = Context.AVATAR, description = "Creates a new permissions group", requiresPermission = "Instance.ManagePermissions")
	public static Response create(State st,
	                              @Arguments(description = "Name of group to create", type = ArgumentType.TEXT_CLEAN, max = 64)
			                              String name) throws UserException, SystemException {
		try { st.getInstance().createPermissionsGroup(name); } catch (UserException e) {
			return new ErrorResponse("Failed to create permissions group - " + e.getLocalizedMessage());
		}
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Create", "PermissionsGroup", null, name, "Avatar created new permissions group");
		return new OKResponse("Permissions Group created successfully!");
	}

	@Commands(context = Context.AVATAR, description = "List permissions groups present at this instance")
	public static Response list(State st) {
		TabularResponse r = new TabularResponse("Permissions groups");
		Set<PermissionsGroup> groups = st.getInstance().getPermissionsGroups();
		for (PermissionsGroup p : groups) {
			r.openRow();
			r.add(p);
		}
		return r;
	}

	@URLs(url = "/permissionsgroups/")
	public static void listForm(State st, SafeMap values) throws UserException, SystemException {
		st.form.add(Modules.run(st, "permissionsgroups.list", values));
	}

	@URLs(url = "/permissionsgroups/delete", requiresPermission = "Instance.ManagePermissions")
	@SideSubMenus(name = "Delete", priority = 11, requiresPermission = "Instance.ManagePermissions")
	public static void deleteForm(State st, SafeMap values) throws UserException, SystemException {
		if ("Submit".equals(values.get("Submit"))) { st.form.add(Modules.run(st, "permissionsgroups.delete", values)); }
		Modules.getHtmlTemplate(st, "permissionsgroups.delete");
	}

	@Commands(context = Context.AVATAR, requiresPermission = "Instance.ManagePermissions", description = "Deletes a permissions group")
	public static Response delete(State st,
	                              @Arguments(description = "Permissions group to delete", type = ArgumentType.PERMISSIONSGROUP)
			                              PermissionsGroup permissionsgroup) throws UserException {
		String success = "NOP";
		permissionsgroup.validate(st);
		String name = permissionsgroup.getNameSafe();
		permissionsgroup.delete();
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "DELETE", "AvatarGroup", name, null, "Avatar deleted permissions group");
		return new OKResponse("Deleted permissions group " + name);
	}

	@URLs(url="/permissionsgroups/edit/*",requiresPermission = "Instance.ManagePermissions")
	public static void editPermissions(State st,SafeMap values) {
		String[] split =st.getDebasedNoQueryURL().split("/");
		if (split.length!=4) { throw new UserException("Incorrect number of query parameters ("+split.length+")"); }
		Integer groupid=Integer.parseInt(split[3]);
		PermissionsGroup pg=PermissionsGroup.get(groupid);
		pg.validate(st);
		Form f=st.form;
		f.add(new TextHeader("Edit permissions group "+pg.getName()));
		// this is all a bit tedious really :)
		Map<Permission.POWER,Set<Permission>> permissions=new HashMap<>();
		permissions.put(Permission.POWER.LOW,new HashSet<>());
		permissions.put(Permission.POWER.MEDIUM,new HashSet<>());
		permissions.put(Permission.POWER.HIGH,new HashSet<>());
		permissions.put(Permission.POWER.UNKNOWN,new HashSet<>());
		for (Module module:Modules.getModules()) {
			if (module.isEnabled(st)) {
				for (Permission permission:module.getPermissions(st).values()) {
					if (permission.grantable()) {
						permissions.get(permission.power()).add(permission);
						if (values.containsKey("SET")) {
							stampPermission(st,pg,module,permission,values.get(permission.getModule(st).getName()+"."+permission.name()));
						}
					}
				}
			}
		}
		st.flushPermissionsGroupCache(); st.flushPermissionsCache();
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

	private static void stampPermission(State st,PermissionsGroup pg,Module module, Permission permission, String s) {
		boolean set=!s.isEmpty();
		String name=permission.getModule(st).getName()+"."+permission.name();
		if (pg.hasPermission(st,name)) {
			if (!set) {
				pg.removePermission(name);
				Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"RemovePermission",pg.getName(),name,null,"Removed permission "+name+" from permissions group "+pg.getName());
			}
		} else {
			if (set) {
				pg.addPermission(st,name);
				Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"AddPermission",pg.getName(),null,name,"Add permission "+name+" to permissions group "+pg.getName());
			}
		}
	}

	private static String addPermissions(State st,Set<Permission> permissions, PermissionsGroup pg,String col) {
		StringBuilder r="";
		TreeMap<String,Permission> sorted=new TreeMap<>();
		for (Permission p:permissions) { sorted.put(p.getModule(st).getName()+"."+p.name(),p); }
		for (Permission p : sorted.values()) {
			String fullname=p.getModule(st).getName()+"."+p.name();
			boolean exists=false;
			Permission a = Modules.get(st, Modules.extractModule(fullname)).getPermission(st, Modules.extractReference(fullname));
			if (a!=null) { exists=true; }
			r.append("<tr bgcolor="+col+"><td>"+fullname+"</td><td>"+p.description()+"</td><td>"+
					"<input type=checkbox name=\""+fullname+"\" value=\""+fullname+"\" "+(pg.hasPermission(st,fullname)?"checked":"")+">"+
					"</td><td>"+(exists?"":"Doesn't Exist")+
					"</td></tr>");
		}
		return r.toString();
	}
}
