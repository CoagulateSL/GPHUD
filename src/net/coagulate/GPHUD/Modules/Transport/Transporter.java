package net.coagulate.GPHUD.Modules.Transport;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public abstract class Transporter {
	public abstract String transportName();
	public abstract List<String> getExportableElements();
	public JSONArray exportElements(final List<String> elements) {
		e
	}
	public abstract JSONObject exportElement(String element);
	public void importElements(final ImportReport report,final JSONArray elements,final boolean simulation) {
	
	}
	public abstract void importElement(ImportReport report,JSONObject element,boolean simulation);
}
