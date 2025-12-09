package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * Send admin message.   Just a command stub.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Broadcast {
	
	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.ANY, description="Send admin message", requiresPermission="instance.SendAdminMessages")
	public static Response admin(@Nonnull final State st,
	                             @Arguments(name="sendmessage",
	                                        description="Message to broadcast",
	                                        type=ArgumentType.TEXT_ONELINE,
	                                        max=200) final String sendmessage) {
		String message="(From ";
		String avfrom="";
		if (st.getAvatarNullable()!=null) {
			avfrom=st.getAvatarNullable().getName();
			message+=avfrom;
		}
		if (st.getCharacterNullable()!=null) {
			if (!st.getCharacter().getName().equals(avfrom)) {
				message+="/"+st.getCharacter().getName();
			}
		}
		message+=") : "+sendmessage;
		final int sent=st.getInstance().broadcastAdmins(st,message);
		return new OKResponse("Sent to "+sent+" admins");
	}
	
	@Nonnull
	@Commands(context=Context.ANY,description="Send message to all HUDs in a region", requiresPermission="Instance.Broadcast")
	public static Response regionBroadcast(@Nonnull final State st,
	                                       @Arguments(name="region",description="Region to broadcast too",type=ArgumentType.REGION) final
	                                       Region region,
	                                       @Arguments(name="sendmessage",description="Message to broadcast to the region",type=ArgumentType.TEXT_ONELINE,max=200) final String sendmessage) {
		if (region.broadcastMessage(st,sendmessage)) {
			return new OKResponse("Sent message to region "+region.getName());
		} else {
			return new ErrorResponse("Failed to message region "+region.getName());
		}
	}
	
	@Nonnull
	@Commands(context=Context.ANY,description="Send message to all HUDs in ALL regions", requiresPermission="Instance.Broadcast")
	public static Response broadcast(@Nonnull final State st,
	                                       @Arguments(name="sendmessage",description="Message to broadcast to the region",type=ArgumentType.TEXT_ONELINE,max=200) final String sendmessage) {
		return massSend(st,Region.getRegions(st,false),sendmessage);
	}
	
	@Nonnull
	@Commands(context=Context.ANY,description="Send message to all HUDs in ALL regions of ALL instances", requiresPermission="User.SuperAdmin")
	public static Response globalBroadcast(@Nonnull final State st,
	                                 @Arguments(name="sendmessage",description="Message to broadcast to the region",type=ArgumentType.TEXT_ONELINE,max=200) final String sendmessage) {
		if (!st.isSuperUser()) { throw new UserAccessDeniedException("Only the Super User may broadcast to all regions"); }
		Set<Region> allregions=new HashSet<>();
		for (Instance instance: Instance.getInstances()) {
			allregions.addAll(Region.getRegions(instance,false));
		}
		return massSend(st,allregions,sendmessage);
	}
	
	
	
	private static Response massSend(@Nonnull final State st,@Nonnull final Set<Region> regions,@Nonnull final String sendmessage) {
		StringBuilder passed=new StringBuilder();
		StringBuilder failed=new StringBuilder();
		for (Region region:regions) {
			if (region.broadcastMessage(st,sendmessage)) {
				if (!passed.isEmpty()) { passed.append(", "); }
				passed.append(region.getName());
			} else {
				if (!failed.isEmpty()) { passed.append(", "); }
				failed.append(region.getName());
			}
		}
		StringBuilder result=new StringBuilder();
		result.append("Broadcast ");
		if (!passed.isEmpty()) {
			result.append("successfully sent to [");
			result.append(passed);
			result.append("]");
			if (!failed.isEmpty()) { result.append(", and "); }
		}
		if (!failed.isEmpty()) {
			result.append("failed to send to [");
			result.append(failed);
			result.append("]");
		}
		if (passed.isEmpty()) {
			return new ErrorResponse(result.toString());
		} else {
			return new OKResponse(result.toString());
		}
	}
}
