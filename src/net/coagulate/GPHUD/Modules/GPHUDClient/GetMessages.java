package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Read your messages.
 *
 * @author Iain Price <gphud@predestined.net
 */
public abstract class GetMessages {

	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.CHARACTER,
	          permitScripting=false,
	          description="Get a message",
	          permitConsole=false,
	          permitUserWeb=false)
	public static Response getMessage(@Nonnull final State st) {
		final Message m=st.getCharacter().getMessage();
		if (m==null) { return new ErrorResponse("You have no outstanding messages."); }
		m.setActive();

		final JSONObject j=new JSONObject(m.getJSON());
		final String message=j.optString("message","");
		if ("factioninvite".equalsIgnoreCase(message)) { return processFactionInvite(st,j); }
		throw new SystemImplementationException("Unable to find a message parser in GPHUDClient for message type '"+message+"'");
	}

	@Nonnull
	public static List<String> getAcceptReject(final State st) {
		final List<String> options=new ArrayList<>();
		options.add("Accept");
		options.add("Reject");
		return options;
	}

	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Accept/Reject a message",
	          permitScripting=false,
	          permitConsole=false,
	          permitUserWeb=false)
	public static Response acceptRejectMessage(@Nonnull final State st,
	                                           @Arguments(type=ArgumentType.CHOICE,
	                                                      description="Accept or Reject the message",
	                                                      choiceMethod="getAcceptReject") final String response) {
		final Message m=st.getCharacter().getActiveMessage();
		if (m==null) { return new ErrorResponse("You have no active message."); }

		final JSONObject j=new JSONObject(m.getJSON());
		final String message=j.optString("message","");
		if ("factioninvite".equalsIgnoreCase(message)) { return processFactionInviteResponse(st,m,j,response); }
		throw new SystemImplementationException("Unable to find a message RESPONSE parser in GPHUDClient for message type '"+message+"'");
	}

	// ----- Internal Statics -----
	@Nonnull
	private static Response processFactionInvite(@Nonnull final State st,
	                                             @Nonnull final JSONObject j) {
		final Char from=Char.get(j.getInt("from"));
		final CharacterGroup faction=CharacterGroup.get(j.getInt("to"));
		final JSONObject template=Modules.getJSONTemplate(st,"gphudclient.acceptrejectmessage");
		template.put("arg0description","You have been invited to join "+faction.getName()+" by "+from.getName());
		return new JSONResponse(template);
	}

	@Nonnull
	private static Response processFactionInviteResponse(@Nonnull final State st,
	                                                     @Nonnull final Message m,
	                                                     @Nonnull final JSONObject j,
	                                                     final String response) {
		final boolean accepted;
		if ("Accept".equalsIgnoreCase(response)) { accepted=true; }
		else {
			if ("Reject".equalsIgnoreCase(response)) { accepted=false; }
			else {
				throw new UserInputValidationParseException("Expected Accept or Reject response");
			}
		}
		final CharacterGroup targetgroup=CharacterGroup.get(j.getInt("to"));
		m.delete();
		st.getCharacter().pushMessageCount();
		if (!accepted) {
			Audit.audit(st,
			            Audit.OPERATOR.CHARACTER,
			            null,
			            Char.get(j.getInt("from")),
			            "Invite Declined",
			            targetgroup.getName(),
			            null,
			            null,
			            "Declined invite to group "+targetgroup.getName()
			           );
			return new OKResponse("Invitation rejected");
		}
		if (targetgroup.getType()!=null) {
			final CharacterGroup currentgroup=st.getCharacter().getGroup(targetgroup.getType());
			if (currentgroup==targetgroup) {
				Audit.audit(st,
				            Audit.OPERATOR.CHARACTER,
				            null,
				            Char.get(j.getInt("from")),
				            "Invite Invalid",
				            targetgroup.getName(),
				            null,
				            null,
				            "Invite to group character is already in "+targetgroup.getName()
				           );
				return new OKResponse("Invitation invalid, you are already a member");
			}
			if (currentgroup!=null && currentgroup.getOwner()==st.getCharacter()) {
				Audit.audit(st,
				            Audit.OPERATOR.CHARACTER,
				            null,
				            Char.get(j.getInt("from")),
				            "Invite Invalid",
				            targetgroup.getName(),
				            null,
				            null,
				            "Invite to group "+targetgroup.getName()+" but leader of conflicting group "+currentgroup.getName()
				           );
				return new OKResponse("Invitation invalid, you are leader of "+currentgroup.getName());
			}
			if (currentgroup!=null) {
				try { currentgroup.removeMember(st.getCharacter()); }
				catch (@Nonnull final UserException e) {
					Audit.audit(st,
					            Audit.OPERATOR.CHARACTER,
					            null,
					            Char.get(j.getInt("from")),
					            "Invite Invalid",
					            targetgroup.getName(),
					            null,
					            null,
					            "Invite to group "+targetgroup.getName()+" failed, leaving old group "+currentgroup.getName()+" errored - "+e.getMessage()
					           );
					return new ErrorResponse("Unable to leave existing group - "+e.getMessage());
				}
			}
		}
		try { targetgroup.addMember(st.getCharacter()); }
		catch (@Nonnull final UserException e) {
			return new ErrorResponse("Unable to join - "+e.getMessage());
		}
		Audit.audit(st,
		            Audit.OPERATOR.CHARACTER,
		            null,
		            Char.get(j.getInt("from")),
		            "Invite Accepted",
		            targetgroup.getName(),
		            null,
		            null,
		            "Accepted group invite to "+targetgroup.getName()
		           );
		return new OKResponse("Invite accepted");
	}

}
