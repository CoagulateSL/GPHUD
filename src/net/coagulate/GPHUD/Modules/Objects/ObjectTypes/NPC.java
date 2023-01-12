package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NPC extends ObjectType {
	protected NPC(final State st,@Nonnull final ObjType object) {
		super(st,object);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explainHtml() {
		final Char ch=getChar();
		return "An interactive NPC - "+(ch==null?"no character set":ch.toString());
	}
	
	@Override
	public void editForm(@Nonnull final State st) {
		final Table t=new Table();
		t.add("Character");
		final DropDownList charList=Char.getNPCList(st,"character");
		if (json.has("character")) {
			charList.setValue(String.valueOf(json.getInt("character")));
		}
		t.add(charList);
		t.openRow();
		t.add("OnClick Script");
		final DropDownList scriptList=Script.getList(st,"script");
		if (json.has("script")) {
			scriptList.setValue(String.valueOf(json.getInt("script")));
		}
		t.add(scriptList);
		t.openRow();
		editFormDistance(st,t);
		t.add(new Cell(new Button("Submit"),2));
		t.openRow();
		t.add(new Cell(
				"<b>WARNING:</b> you can only assign a character to ONE object, setting multiple objects to use the same character will cause all but one of them to be "+
				"shutdown by the server</b>",
				2));
		t.openRow();
		t.add(new Cell(
				"Your script will have a new variable, TARGET, which is the character interacting with the script.  CALLER will be the NPC character, and AVATAR will be the "+
				"objects owner and should be ignored",
				2));
		st.form().add(t);
	}
	
	@Override
	public void update(@Nonnull final State st) {
		boolean update=false;
		if (st.postMap().containsKey("character")) {
			final int charId=Integer.parseInt(st.postMap().get("character"));
			Char.get(charId).validate(st);
			if ((!json.has("character"))||charId!=json.getInt("character")) {
				update=true;
				json.put("character",charId);
			}
		}
		if (st.postMap().containsKey("script")) {
			final int scriptId=Integer.parseInt(st.postMap().get("script"));
			final Script script=Script.get(scriptId);
			script.validate(st);
			if ((!json.has("script"))||scriptId!=json.getInt("script")) {
				json.put("script",scriptId);
				update=true;
			}
		}
		update=updateDistance(st)||update;
		if (update) {
			object.setBehaviour(json);
		}
	}
	
	@Nonnull
	@Override
	public String explainText() {
		return explainHtml();
	}
	
	@Override
	public void payload(@Nonnull final State st,
	                    @Nonnull final JSONObject response,
	                    @Nonnull final Region region,
	                    @Nullable final String url) {
		super.payload(st,response,region,url);
		if (!json.has("character")) {
			return;
		}
		final int charId=json.getInt("character");
		final Char ch=Char.get(charId);
		ch.validate(st);
		ch.setRegion(region);
		if (url!=null) {
			ch.setURL(url);
		}
		final State newState=new State(ch);
		ch.initialConveyances(newState,response);
		if (json.has("maxdistance")) {
			response.put("maxdistance",json.get("maxdistance"));
		}
		response.remove("qb1texture");
		response.remove("qb2texture");
		response.remove("qb3texture");
		response.remove("qb4texture");
		response.remove("qb5texture");
		response.remove("qb6texture");
		response.remove("motd");
		response.remove("leveltext");
		response.remove("legacymenu");
		response.remove("hudtext");
		response.remove("namelessprefix");
		response.remove("opencmd");
		response.remove("rpchannel");
		response.remove("setlogo");
		response.remove("qbbalance");
		response.remove("uixmenus");
		response.remove("hudcolor");
		
	}
	
	@Nonnull
	@Override
	public MODE mode() {
		return MODE.CLICKABLE;
	}
	
	@Nonnull
	@Override
	public Response click(@Nonnull final State st,@Nonnull final Char clicker) {
		// do we have a character set
		if (!json.has("character")) {
			return new ErrorResponse("No character is associated with this NPC object");
		}
		final int charId=json.getInt("character");
		final Char ch=Char.get(charId);
		ch.validate(st);
		st.setCharacter(ch);
		if (!json.has("script")) {
			return new ErrorResponse("No script is associated with this NPC object");
		}
		final int scriptId=json.getInt("script");
		final Script script=Script.get(scriptId);
		script.validate(st);
		final GSVM vm=new GSVM(script);
		vm.introduce("TARGET",new BCCharacter(null,clicker));
		vm.introduce("TARGETUUID",new BCString(null,clicker.getPlayedBy().getUUID()));
		populateVmVariables(st,vm);
		final JSONObject jsonResponse=vm.execute(st).asJSON(st);
		//System.out.println(response.asJSON(st));
		ch.appendConveyance(st,jsonResponse);
		clicker.considerPushingConveyances();
		return new JSONResponse(jsonResponse);
	}
	
	// ----- Internal Instance -----
	@Nullable
	Char getChar() {
		final String charId=json.optString("character","");
		if (charId==null||charId.isEmpty()) {
			return null;
		}
		return Char.get(Integer.parseInt(charId));
	}
}
