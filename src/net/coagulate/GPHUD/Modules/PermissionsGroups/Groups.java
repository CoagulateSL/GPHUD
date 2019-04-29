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
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import java.util.Set;

/**
 *
 * @author iain
 */
public abstract class Groups {
    
    @URLs(url="/permissionsgroups/view/*")
    public static void view(State st,SafeMap values) throws UserException, SystemException {
        Form f=st.form;
        f.noForm();
        String[] split=st.getDebasedURL().split("/");
        String id=split[split.length-1];
        PermissionsGroup pg=PermissionsGroup.get(Integer.parseInt(id));
        pg.validate(st);
        f.add(new TextHeader(pg.getName()));
        Set<String> permissions=pg.getPermissions(st);
        Table permtable=new Table(); f.add(new TextSubHeader("Permissions"));
        if (!permissions.isEmpty()) { f.add(permtable); }
        permtable.add(new HeaderRow().add("Permission").add("Description"));
        for (String permission:permissions) {
            permtable.openRow();
            permtable.add(permission);
            Permission rawpermission = Modules.getPermission(st, permission);
            if (rawpermission!=null) { permtable.add(rawpermission.description()); }
            else { permtable.add("<font color=red><i>Permission no longer exists</i></font>"); }
            if (st.isInstanceOwner() || st.isSuperUser()) {
                Form dp=new Form();
                dp.setAction("../delpermission");
                dp.add(new Hidden("permissionsgroup",""+pg.getName()));
                dp.add(new Hidden("Permission",permission));
                dp.add(new Hidden("okreturnurl",st.getFullURL()));
                dp.add(new Button("Delete Permission",true));
                permtable.add(dp);
            }   
        }
        if (st.isInstanceOwner() || st.isSuperUser()) {
            Form ap=new Form();
            f.add(ap);
            ap.setAction("../addpermission");
            ap.add(new Hidden("permissionsgroup",""+pg.getName()));
            ap.add(new Hidden("okreturnurl",st.getFullURL()));
            ap.add(new Button("Add Permission"));
        }
        f.add(new TextSubHeader("Members"));
        Set<PermissionsGroupMembership> members=pg.getMembers();
        Table membertable=new Table(); membertable.add(new HeaderRow().add("Member").add("Can Invite").add("Can Kick"));
        if (!members.isEmpty()) { f.add(membertable); }
        boolean caneject=pg.canEject(st);
        for (PermissionsGroupMembership member:members) {
            membertable.openRow();
            membertable.add(member.avatar.getGPHUDLink());
            if (member.caninvite) { membertable.add(new Color("green","Yes")); } else { membertable.add(new Color("red","No")); }
            if (member.cankick) { membertable.add(new Color("green","Yes")); } else { membertable.add(new Color("red","No")); }
            if (st.isInstanceOwner()) {
                Form spf=new Form();
                spf.add(new Hidden("okreturnurl",st.getFullURL()));
                membertable.add(spf);
                spf.setAction("../setpermissions");
                spf.add(new Hidden("permissionsgroup",""+pg.getName()));
                spf.add(new Hidden("Avatar",member.avatar.getName()));
                if (member.caninvite) { spf.add(new Hidden("CanInvite","on")); }
                if (member.cankick) { spf.add(new Hidden("CanKick","on")); }
                spf.add(new Button("Set Invite/Kick"));
            }
            if (caneject) { 
                Form ef=new Form();
                ef.add(new Hidden("okreturnurl",st.getFullURL()));
                ef.setAction("../eject");
                ef.add(new Hidden("permissionsgroup",""+pg.getName()));
                ef.add(new Hidden("Avatar",member.avatar.getName()));
                ef.add(new Button("Eject"));
                membertable.add(ef);
            }
        }
        if (pg.canInvite(st)) {
            Form invite=new Form();
            invite.add(new Hidden("okreturnurl",st.getFullURL()));
            invite.setAction("../invite");
            invite.add(new Hidden("permissionsgroup",""+pg.getName()));
            invite.add(new Button("Add Member"));
            f.add(invite);
        }
        if (st.isInstanceOwner()) {
            Form df=new Form();
            df.setAction("../delete");
            df.add(new Hidden("permissionsgroup",""+pg.getName()));
            df.add(new Button("DELETE GROUP"));
            f.add(new Separator());
            f.add(df);
        }
    }

    
    
    
    @URLs(url = "/permissionsgroups/create",requiresPermission = "instance.owner")
    @SideSubMenus(name = "Create",priority = 10)
    public static void createGroupPage(State st,SafeMap values) throws UserException, SystemException {
        if (values.get("Submit").equals("Submit")) { st.form.add(Modules.run(st,"permissionsgroups.create",values)); }
        Modules.getHtmlTemplate(st,"permissionsgroups.create");
    }

    @Commands(context = Context.AVATAR,description = "Creates a new permissions group",requiresPermission = "instance.owner")
    public static Response create(State st,
            @Arguments(description = "Name of group to create",type = ArgumentType.TEXT_CLEAN,max=64)
                    String name) throws UserException,SystemException {
        if (!st.isInstanceOwner()) { return new ErrorResponse("Insufficient permission to create a permissions group"); }
        try { st.getInstance().createPermissionsGroup(name); }
        catch (UserException e) {
            return new ErrorResponse("Failed to create permissions group - "+e.getLocalizedMessage());
        }
        Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Create", "PermissionsGroup", null, name, "Avatar created new permissions group");
        return new OKResponse("Permissions Group created successfully!");
    }
    
    @Commands(context = Context.AVATAR,description = "List permissions groups present at this instance")
    public static Response list(State st) {
        TabularResponse r=new TabularResponse("Permissions groups");
        Set<PermissionsGroup> groups=st.getInstance().getPermissionsGroups();
        for (PermissionsGroup p:groups) {
            r.openRow();
            r.add(p);
        }
        return r;
    }

    @URLs(url = "/permissionsgroups/")
    public static void listForm(State st,SafeMap values) throws UserException, SystemException {
        st.form.add(Modules.run(st, "permissionsgroups.list",values));
    }
    
    @URLs(url="/permissionsgroups/delete",requiresPermission = "instance.owner")
    @SideSubMenus(name="Delete",priority=11,requiresPermission = "instance.owner")
    public static void deleteForm(State st,SafeMap values) throws UserException,SystemException {
        if (values.get("Submit").equals("Submit")) { st.form.add(Modules.run(st,"permissionsgroups.delete",values)); }
        Modules.getHtmlTemplate(st, "permissionsgroups.delete");
    }
    
    @Commands(context = Context.AVATAR,requiresPermission = "instance.owner",description = "Deletes a permissions group")
    public static Response delete(State st,
            @Arguments(description = "Permissions group to delete",type = ArgumentType.PERMISSIONSGROUP) 
                PermissionsGroup permissionsgroup) throws UserException
    {
        if (!st.isInstanceOwner()) { throw new UserException("Insufficient permission to create a permissions group"); }        
        String success="NOP";
        permissionsgroup.validate(st);
        String name=permissionsgroup.getNameSafe();
        permissionsgroup.delete();
        Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "DELETE", "AvatarGroup", name, null, "Avatar deleted permissions group");
        return new OKResponse("Deleted permissions group "+name);
    }
}
