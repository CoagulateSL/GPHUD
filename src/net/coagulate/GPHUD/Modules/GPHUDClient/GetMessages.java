package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.Message;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Read your messages.
 *
 * @author Iain Price <gphud@predestined.net
 */
public abstract class GetMessages {

	@Commands(context = Context.CHARACTER, permitScripting = false, description = "Get a message", permitConsole = false, permitUserWeb = false)
	public static Response getMessage(State st) throws SystemException, UserException {
		Message m = st.getCharacter().getMessage();
		if (m == null) { return new ErrorResponse("You have no outstanding messages."); }
		m.setActive();

		JSONObject j = new JSONObject(m.getJSON());
		String message = j.optString("message", "");
		if ("factioninvite".equalsIgnoreCase(message)) { return processFactionInvite(st, j); }
		throw new SystemException("Unable to find a message parser in GPHUDClient for message type '" + message + "'");
	}

	private static Response processFactionInvite(State st, JSONObject j) throws UserException, SystemException {
		Char from = Char.get(j.getInt("from"));
		CharacterGroup faction = CharacterGroup.get(j.getInt("to"));
		JSONObject template = Modules.getJSONTemplate(st, "gphudclient.acceptrejectmessage");
		template.put("arg0description", "You have been invited to join " + faction.getName() + " by " + from.getName());
		return new JSONResponse(template);
	}

	public static List<String> getAcceptReject(State st) throws UserException {
		List<String> options = new ArrayList<>();
		options.add("Accept");
		options.add("Reject");
		return options;
	}

	@Commands(context = Context.CHARACTER, description = "Accept/Reject a message", permitScripting = false, permitConsole = false, permitUserWeb = false)
	public static Response acceptRejectMessage(State st,
	                                           @Arguments(type = ArgumentType.CHOICE, description = "Accept or Reject the message", choiceMethod = "getAcceptReject")
			                                           String response) throws SystemException, UserException {
		Message m = st.getCharacter().getActiveMessage();
		if (m == null) { return new ErrorResponse("You have no active message."); }

		JSONObject j = new JSONObject(m.getJSON());
		String message = j.optString("message", "");
		if ("factioninvite".equalsIgnoreCase(message)) { return processFactionInviteResponse(st, m, j, response); }
		throw new SystemException("Unable to find a message RESPONSE parser in GPHUDClient for message type '" + message + "'");
	}

	private static Response processFactionInviteResponse(State st, Message m, JSONObject j, String response) throws UserException {
		boolean accepted;
		if ("Accept".equalsIgnoreCase(response)) { accepted = true; } else {
			if ("Reject".equalsIgnoreCase(response)) { accepted = false; } else {
				throw new UserException("Expected Accept or Reject response");
			}
		}
		CharacterGroup targetgroup = CharacterGroup.get(j.getInt("to"));
		m.delete();
		st.getCharacter().pushMessageCount();
		if (!accepted) {
			Audit.audit(st, Audit.OPERATOR.CHARACTER, null, Char.get(j.getInt("from")), "Invite Declined", targetgroup.getName(), null, null, "Declined invite to group " + targetgroup.getName());
			return new OKResponse("Invitation rejected");
		}
		if (targetgroup.getType() != null) {
			CharacterGroup currentgroup = st.getCharacter().getGroup(targetgroup.getType());
			if (currentgroup == targetgroup) {
				Audit.audit(st, Audit.OPERATOR.CHARACTER, null, Char.get(j.getInt("from")), "Invite Invalid", targetgroup.getName(), null, null, "Invite to group character is already in " + targetgroup.getName());
				return new OKResponse("Invitation invalid, you are already a member");
			}
			if (currentgroup != null && currentgroup.getOwner() == st.getCharacter()) {
				Audit.audit(st, Audit.OPERATOR.CHARACTER, null, Char.get(j.getInt("from")), "Invite Invalid", targetgroup.getName(), null, null, "Invite to group " + targetgroup.getName() + " but leader of conflicting group " + currentgroup.getName());
				return new OKResponse("Invitation invalid, you are leader of " + currentgroup.getName());
			}
			if (currentgroup != null) {
				try { currentgroup.removeMember(st.getCharacter()); } catch (UserException e) {
					Audit.audit(st, Audit.OPERATOR.CHARACTER, null, Char.get(j.getInt("from")), "Invite Invalid", targetgroup.getName(), null, null, "Invite to group " + targetgroup.getName() + " failed, leaving old group " + currentgroup.getName() + " errored - " + e.getMessage());
					return new ErrorResponse("Unable to leave existing group - " + e.getMessage());
				}
			}
		}
		try { targetgroup.addMember(st.getCharacter()); } catch (UserException e) {
			return new ErrorResponse("Unable to join - " + e.getMessage());
		}
		Audit.audit(st, Audit.OPERATOR.CHARACTER, null, Char.get(j.getInt("from")), "Invite Accepted", targetgroup.getName(), null, null, "Accepted group invite to " + targetgroup.getName());
		return new OKResponse("Invite accepted");
	}

}
