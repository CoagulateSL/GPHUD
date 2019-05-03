package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;

import java.util.Set;

/**
 * A submission button.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Button extends Input {

	String name;
	String value;
	boolean borderless = false;

	//public boolean pressed() { if (getValue()==null || getValue().isEmpty()) { return false; } return true; }
	public Button(String s) {
		name = s;
		value = s;
	}

	public Button(String n, String v) {
		name = n;
		value = v;
	}

	public Button(String s, boolean borderless) {
		name = s;
		value = s;
		this.borderless = borderless;
	}

	public Button(String n, String v, boolean borderless) {
		name = n;
		value = v;
		this.borderless = borderless;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String asHtml(State st, boolean rich) {
		String style = "";
		if (borderless) {
			style = "-webkit-appearance: textfield; border-top-width: 1px; border-right-width: 1px; border-left-width: 1px; border-bottom-width: 1px; padding-bottom: 0px; padding-top: 0px; padding-left: 0px; padding-right: 0px; margin-top: 0px; margin-bottom: 0px; margin-left: 0px; margin-right: 0px;";
		}
		return "<input type=submit name=\"" + name + "\" value=\"" + value + "\" style=\"" + style + "\"/>";
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}

}
