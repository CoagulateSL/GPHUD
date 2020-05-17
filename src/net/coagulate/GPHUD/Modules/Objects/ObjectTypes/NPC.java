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
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NPC extends ObjectType {
	protected NPC(final State st,
	              @Nonnull final ObjType object) {
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
		final DropDownList charlist=Char.getNPCList(st,"character");
		if (json.has("character")) { charlist.setValue(""+json.getInt("character")); }
		t.add(charlist);
		t.openRow();
		t.add("OnClick Script");
		final DropDownList scriptlist=Script.getList(st,"script");
		if (json.has("script")) { scriptlist.setValue(""+json.getInt("script")); }
		t.add(scriptlist);
		t.openRow();
		t.add(new Cell(new Button("Submit"),2));
		t.openRow();
		t.add(new Cell(
				"<b>WARNING:</b> you can only assign a character to ONE object, setting multiple objects to use the same character will cause all but one of them to be "+"shutdown by the server</b>",
				2
		));
		t.openRow();
		t.add(new Cell(
				"Your script will have a new variable, TARGET, which is the character interacting with the script.  CALLER will be the NPC character, and AVATAR will be the "+"objects owner and should be ignored",
				2
		));
		st.form().add(t);
	}

	@Override
	public void update(@Nonnull final State st) {
		boolean update=false;
		if (st.postmap().containsKey("character")) {
			final int charid=Integer.parseInt(st.postmap().get("character"));
			Char.get(charid).validate(st);
			if ((!json.has("character")) || charid!=json.getInt("character")) {
				update=true;
				json.put("character",charid);
			}
		}
		if (st.postmap().containsKey("script")) {
			final int scriptid=Integer.parseInt(st.postmap().get("script"));
			final Script script=Script.get(scriptid);
			script.validate(st);
			if ((!json.has("script")) || scriptid!=json.getInt("script")) {
				json.put("script",scriptid);
				update=true;
			}
		}
		if (update) { object.setBehaviour(json); }
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
		if (!json.has("character")) { return; }
		final int charid=json.getInt("character");
		final Char ch=Char.get(charid);
		ch.validate(st);
		ch.setRegion(region);
		if (url!=null) { ch.setURL(url); }
		final State newstate=new State(ch);
		ch.initialConveyances(newstate,response);
	}

	@Nonnull
	@Override
	public MODE mode() {
		return MODE.CLICKABLE;
	}

	@Nonnull
	@Override
	public Response click(@Nonnull final State st,
	                      @Nonnull final Char clicker) {
		// do we have a character set
		if (!json.has("character")) { return new ErrorResponse("No character is associated with this NPC object"); }
		final int charid=json.getInt("character");
		final Char ch=Char.get(charid);
		ch.validate(st);
		st.setCharacter(ch);
		if (!json.has("script")) { return new ErrorResponse("No script is associated with this NPC object"); }
		final int scriptid=json.getInt("script");
		final Script script=Script.get(scriptid);
		script.validate(st);
		final GSVM vm=new GSVM(script.getByteCode());
		vm.introduce("TARGET",new BCCharacter(null,clicker));
		final JSONObject jsonresponse=vm.execute(st).asJSON(st);
		//System.out.println(response.asJSON(st));
		ch.appendConveyance(st,jsonresponse);
		clicker.considerPushingConveyances();
		return new JSONResponse(jsonresponse);
	}

	// ----- Internal Instance -----
	@Nullable
	Char getChar() {
		final String chid=json.optString("character","");
		final String chname="";
		if (chid==null || chid.isEmpty()) { return null; }
		return Char.get(Integer.parseInt(chid));
	}
}
