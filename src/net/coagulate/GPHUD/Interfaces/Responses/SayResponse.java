package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * A response formatted as a "sayas" response for SL.  Returns just the message in HTML.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class SayResponse implements Response {

	String reason;
	@Nullable
	String sayas;

	public SayResponse(final String r) { reason = r; }

	public SayResponse(final String r, @Nullable final String sayas) {
		reason = r;
		this.sayas = sayas;
	}

	public String getText() { return reason; }

	public void setText(final String text) { reason = text; }

	@Nonnull
	@Override
	public JSONObject asJSON(final State st) {
		final JSONObject json = new JSONObject();
		if (sayas != null) {
			json.put("sayas", sayas);
			json.put("say", "/me " + reason);
		} else { json.put("say", reason); }
		return json;
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		return (sayas==null?"":sayas+": ")+reason;
	}

	@Nonnull
	@Override
	public String asText(@Nonnull final State st) {
		if (st.getCharacterNullable() != null) {
			final Transmission t = new Transmission(st.getCharacter(), asJSON(st));
			t.start();
		}
		final String message;
		if (sayas != null) { message = "\"" + sayas + " " + reason + "\""; } else { message = "\"" + reason + "\""; }
		return message;
	}

	@Nonnull
	@Override
	public String asHtml(@Nonnull final State st, final boolean rich) {
		return new Paragraph(asText(st)).asHtml(st, rich);
	}


	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
