package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.TextError;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * A response that represents an error with the command.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ErrorResponse extends TextError implements Response {

    public ErrorResponse(final String content) {
        super(content);
    }

    // ---------- INSTANCE ----------
    @Nonnull
    @Override
    public JSONObject asJSON(final State st) {
        final JSONObject j = new JSONObject();
        j.put("error", asText(st));
        return j;
    }

	@Nonnull
	@Override
	public String scriptResponse() {
        return "Error: " + content;
	}

}
