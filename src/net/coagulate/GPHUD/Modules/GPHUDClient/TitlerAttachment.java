package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TitlerAttachment {
	
	public static final String[] ATTACHMENT_POINTS=
			{"None","Chest","Skull","Left Shoulder","Right Shoulder","Left Hand","Right Hand","Left Foot","Right Foot",
			 "Spine","Pelvis","Mouth","Chin","Left Ear","Right Ear","Left Eye","Right Eye","Nose","R Upper Arm",
			 "R Lower Arm","L Upper Arm","L Lower Arm","Right Hip","R Upper Leg","R Lower Leg","Left Hip","L Upper Leg",
			 "L Lower Leg","Stomach","Left Pec","Right Pec","","","","","","","","","Neck","Avatar Center",
			 "Left Ring Finger","Right Ring Finger","Tail Base","Tail Tip","Left Wing","Right Wing","Jaw",
			 "Alt Left Ear","Alt Right Ear","Alt Left Eye","Alt Right Eye","Tongue","Groin","Left Hind Foot",
			 "Right Hind Foot"};
	
	@Templater.Template(name="TITLERATTACHPOINT",
	                    description="Translation of titler attachment point from name to number, which is what the HUD needs")
	public static String translateAttachmentPoint(@Nonnull final State st,final String key) {
		if (st.getCharacterNullable()==null) {
			return "2";
		}
		final String attachmentPoint=st.getKV("GPHUDClient.TitlerAttachment").toString();
		int converted=2; // Default to skull if everything else fails
		if (!attachmentPoint.isBlank()) {
			for (int i=0;i<ATTACHMENT_POINTS.length;i++) {
				if (ATTACHMENT_POINTS[i].equalsIgnoreCase(attachmentPoint)) {
					converted=i;
				}
			}
		}
		return Integer.toString(converted);
	}
	
	@Command.Commands(description="Allows you to set the desired attachment point for your Titler",
	                  context=Command.Context.CHARACTER)
	public static Response setTitlerAttachPoint(@Nonnull final State state,
	                                            @Argument.Arguments(name="location",
	                                                                type=Argument.ArgumentType.CHOICE,
	                                                                description="Location for your Titler to attach",
	                                                                choiceMethod="net.coagulate.GPHUD.Modules.GPHUDClient.TitlerAttachment.getLocations")
	                                            @Nonnull final String location) {
		state.setKV(state.getCharacter(),"GPHUDClient.TitlerAttachment",location);
		//return resetTitler(state);
		return new OKResponse("Titler attachment point updated");
	}
	
	@Nonnull
	public static List<String> getLocations(final State st) {
		return new ArrayList<>(Arrays.asList(ATTACHMENT_POINTS).subList(1,ATTACHMENT_POINTS.length));
	}
	
	@Command.Commands(description="Re-requests you a titler",
	                  context=Command.Context.CHARACTER,
	                  permitUserWeb=false,
	                  permitExternal=false,
	                  permitScripting=false,
	                  permitObject=false)
	public static Response resetTitler(@Nonnull final State state) {
		final JSONObject json=new JSONObject();
		json.put("titler",state.getKV("GPHUDClient.TitlerAttachmentConverted").toString());
		JSONResponse.ownerSay(json,"Updating titler",state.getCharacter().getProtocol());
		return new JSONResponse(json);
	}
	
}
