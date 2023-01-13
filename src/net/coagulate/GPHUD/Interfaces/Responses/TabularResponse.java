package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation of a Response, the OK Response.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TabularResponse extends Table implements Response {
	@Nullable String title;
	
	public TabularResponse() {
	}
	
	public TabularResponse(@Nullable final String message) {
		title=message;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public JSONObject asJSON(final State st) {
		final JSONObject j=new JSONObject();
		JSONResponse.message(j,asText(st),st.protocol);
		return j;
	}
	
	@Nonnull
	@Override
	public String scriptResponse() {
		return "<A tablulated response>";
	}
	
	@Nonnull
	public String asText(final State st) {
		String s="";
		if (title!=null&&!title.isEmpty()) {
			s+=new TextHeader(title).asText(st);
		}
		s+=super.asText(st);
		return s;
	}
	
	@Nonnull
	@Override
	public String asHtml(final State st,final boolean rich) {
		String s="";
		if (title!=null&&!title.isEmpty()) {
			s+=new TextHeader(title).asHtml(st,rich);
		}
		s+=super.asHtml(st,rich);
		return s;
	}
}
