package net.coagulate.GPHUD.Modules.Transport.Transports;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.PermissionsGroup;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Modules.Transport.ImportReport;
import net.coagulate.GPHUD.Modules.Transport.Transporter;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PermissionsGroupTransport extends Transporter {
	@Override
	public String description() {
		return "Permissions Groups";
	}
	
	@Nonnull
	@Override
	public List<String> getExportableElements(@Nonnull final State st) {
		return PermissionsGroup.getPermissionsGroups(st).stream().map(TableRow::getName).toList();
	}
	
	@Override
	protected void exportElement(@Nonnull final State st,
	                             @Nonnull final String element,
	                             @Nonnull final JSONObject exportTo) {
		final PermissionsGroup pg=PermissionsGroup.find(element,st.getInstance());
		final JSONArray permissions=new JSONArray();
		for (final String permission: pg.getPermissions(st)) {
			permissions.put(permission);
		}
		exportTo.put("permissions",permissions);
		
		final JSONArray members=new JSONArray();
		for (final PermissionsGroup.PermissionsGroupMembership member: pg.getMembers()) {
			final JSONObject addingMember=new JSONObject();
			addingMember.put("uuid",member.avatar.getUUID());
			addingMember.put("caninvite",member.caninvite);
			addingMember.put("cankick",member.cankick);
			members.put(addingMember);
		}
		exportTo.put("members",members);
	}
	
	@Override
	protected void importElement(@Nonnull final State state,
	                             @Nonnull final ImportReport report,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject element,
	                             final boolean simulation) {
		final AtomicBoolean noop=new AtomicBoolean(true);
		existCheck(state,
		           simulation,
		           report,
		           PermissionsGroup.find(name,state.getInstance()),
		           name,
		           ()->PermissionsGroup.create(state,name));
		final PermissionsGroup pg=PermissionsGroup.find(name,state.getInstance());
		final JSONArray permissions=element.getJSONArray("permissions");
		permissions.iterator().forEachRemaining(x->{
			if (!pg.hasPermission(state,(String)x)) {
				noop.set(false);
				if (simulation) {
					report.info("PermissionsGroup - Would add permission "+x+" to "+name);
				} else {
					report.info("PermissionsGroup - Added permission "+x+" to "+name);
					pg.addPermission(state,(String)x);
					Audit.audit(state,
					            Audit.OPERATOR.AVATAR,
					            null,
					            null,
					            "Import Permissions Group Permission",
					            name,
					            null,
					            (String)x,
					            "Added permission to permissions group via import");
				}
			}
		});
		final JSONArray members=element.getJSONArray("members");
		members.iterator().forEachRemaining(x->{
			final JSONObject userData=(JSONObject)x;
			final User user=User.findUserKeyNullable(userData.getString("uuid"));
			if (user==null) {
				report.warn("PermissionsGroup - Could not restore user "+userData.getString("uuid")+
				            " to permissions group "+name+", UUID lookup failed");
			} else {
				if (!pg.hasMember(user)) {
					noop.set(false);
					report.info(
							"PermissionsGroup - Added User '"+user.getName()+"' ("+userData.getString("uuid")+") to "+
							name);
					if (!simulation) {
						pg.addMember(user);
					}
				}
				final boolean currentInvite=pg.canInvite(new State(state.getInstance()).setAvatar(user));
				final boolean currentKick=pg.canEject(new State(state.getInstance()).setAvatar(user));
				final boolean newInvite=userData.getBoolean("caninvite");
				final boolean newKick=userData.getBoolean("cankick");
				if (currentInvite!=newInvite||currentKick!=newKick) {
					noop.set(false);
					report.info(
							"PermissionsGroup - Update "+user.getName()+" to kick:"+(newKick?"true":"false")+" invite:"+
							(newInvite?"true":"false"));
					if (!simulation) {
						pg.setUserPermissions(user,newInvite,newKick);
						Audit.audit(state,
						            Audit.OPERATOR.AVATAR,
						            user,
						            null,
						            "Permissions Group Import",
						            "Kick/Invite",
						            "Kick:"+currentKick+" Invite:"+currentInvite,
						            "Kick:"+newKick+" Invite:"+newInvite,
						            "Updated kick/invite permissions for user via Permissions Group Import");
					}
				}
			}
		});
		if (noop.get()) {
			report.noop("PermissionsGroup - No changes to "+name);
		}
	}
}

