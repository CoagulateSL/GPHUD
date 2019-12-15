package net.coagulate.GPHUD.Interfaces.User;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.Hidden;
import net.coagulate.GPHUD.Interfaces.Inputs.Input;
import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Interfaces.Outputs.Text;
import net.coagulate.GPHUD.Interfaces.Responses.NullResponse;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implements a "Form"
 * Basically just a list of Elements
 *
 * @author iain
 */
public class Form implements Renderable {

	final List<Renderable> list = new ArrayList<>();
	@Nullable
	String action = null;
	/*
	public Map<String, String> getValueMap() {
		Map<String,String> parameters=new TreeMap<>();
		flattenInputs(getSubRenderables(),parameters);
		return parameters;
	}
	*/
    /*private void flattenInputs(Set<Renderable> subrenderables, Map<String,String> parameters) {
        for (Renderable r:subrenderables) {
            if (r instanceof Input) {
                Input i=(Input)r;
                parameters.put(i.getName(),i.getValue());
            }
            Set<Renderable> subsub=r.getSubRenderables();
            if (subsub!=null) {
                flattenInputs(subsub,parameters);
            }
        }
    }*/
	private boolean form = true;

	public Form() {
		add(new Hidden("okreturnurl", ""));
	}

	public Form(@Nonnull State st, boolean setreturnurl, String targeturl, String buttonname, @Nonnull String... inputs) throws SystemException {
		setAction(targeturl);
		if (setreturnurl) { add(new Hidden("okreturnurl", st.getDebasedURL())); }
		add(new Button(buttonname, true));
		if ((inputs.length % 2) == 1) {
			throw new SystemException("Input varargs must be even (key,value pairs), we got " + inputs.length);
		}
		for (int i = 0; i < inputs.length; i += 2) {
			add(new Hidden(inputs[i], inputs[i + 1]));
		}
	}

	@Nonnull
	public Form add(String s) { return add(new Text(s)); }

	@Nonnull
	public Form add(Renderable e) {
		list.add(e);
		return this;
	}

	public void p(Renderable paragraph) {
		add(new Paragraph(paragraph));
	}

	public void p(String text) { add(new Paragraph(text)); }

	@Nonnull
	@Override
	public String asText(State st) {
		StringBuilder response = new StringBuilder();
		for (Renderable r : list) { response.append(r.asText(st)); }
		return response.toString();
	}

	@Nullable
	public String getAction() { return action; }

	public void setAction(@Nullable String a) { action = a; }

	@Nonnull
	@Override
	public String asHtml(State st, boolean rich) {
		StringBuilder response = new StringBuilder();
		if (form) {
			response.append("<form method=post");
			if (action != null && !action.isEmpty()) { response.append(" action=\"").append(action).append("\""); }
			response.append(" style=\"border-top-width: 0px; border-right-width: 0px; border-left-width: 0px; border-bottom-width: 0px; padding-bottom: 0px; padding-top: 0px; padding-left: 0px; padding-right: 0px; margin-top: 0px; margin-bottom: 0px; margin-left: 0px; margin-right: 0px;\">\n");
		}
		for (Renderable r : list) {
			if (!(r instanceof NullResponse)) { response.append(r.asHtml(st, rich)).append("\n"); }  // else { response+="{NULLRESPONSE}"; }


		}
		if (form) {
			response.append("</form>\n");
		}
		return response.toString();
	}

	@Nonnull
	@Override
	public Set<Renderable> getSubRenderables() {
		return new HashSet<>(list);
	}

	public void readValue(String key, String value) {
		//System.out.println(key+"="+value);
		Input i = findInput(key);
		if (i == null) { return; }
		i.setValue(value);
	}

	/*public String getValue(String name)
	{
		Input i=findInput(name);
		if (i==null) { return ""; }
		String value=i.getValue();
		if (value==null) { return ""; }
		return value;
	}
	*/
	@Nullable
	public Input findInput(String name) {
		return scourRenderables(getSubRenderables(), name);
	}

	@Nullable
	private Input scourRenderables(@Nonnull Set<Renderable> subrenderables, String name) {
		for (Renderable r : subrenderables) {
			if (r instanceof Input) {
				Input i = (Input) r;
				// if match
				//System.out.println("Searching "+name+" against "+i.getName());
				if (i.getName().equalsIgnoreCase(name)) { return i; }
			}
			// no match, recurse
			Set<Renderable> subsub = r.getSubRenderables();
			if (subsub != null) {
				Input i = scourRenderables(subsub, name);
				if (i != null) { return i; }
			}
		}
		return null;
	}

	public void noForm() {
		form = false;
	}

	@Nonnull
	public Form br() {
		return add("<br>");
	}
}
