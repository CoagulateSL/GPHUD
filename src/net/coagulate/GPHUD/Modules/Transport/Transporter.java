package net.coagulate.GPHUD.Modules.Transport;

import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Abstract definition of a transport ; a module that imports/exports a group of data.
 * Transporters are not expected to track permissions.
 */
public abstract class Transporter {
	
	/**
	 * Describe this export module.
	 *
	 * @return A human parsable description of this export.
	 */
	public abstract String description();
	
	/**
	 * Returns a list of exportable elements
	 *
	 * @param st Caller's state
	 * @return A List of exportable elements
	 */
	@Nonnull
	public abstract List<String> getExportableElements(@Nonnull final State st);
	
	/**
	 * Starts an export of all passed elements.
	 *
	 * @param st       Export state
	 * @param elements List of strings of elements to export
	 * @return A JSON object of String(id)->JSONObject(data)
	 */
	@Nonnull
	public JSONObject exportElements(@Nonnull final State st,@Nonnull final List<String> elements) {
		st.checkPermission("Transport.Import"+transportName());
		final JSONObject objects=new JSONObject();
		for (final String element: elements) {
			final JSONObject object=new JSONObject();
			exportElement(st,element,object);
			objects.put(element,object);
		}
		return objects;
	}
	
	/**
	 * Get the name of this transport, used as an identifier in the export.
	 *
	 * @return A unique name for this transport.  Defaults to the class name.
	 */
	public String transportName() {
		return this.getClass().getSimpleName().replaceAll("Transport","");
	}
	
	/**
	 * Export a specific element.
	 *
	 * @param st       Exporters state
	 * @param element  Element to export
	 * @param exportTo Preprepared JSONObject to export data to
	 */
	protected abstract void exportElement(@Nonnull final State st,
	                                      @Nonnull final String element,
	                                      @Nonnull final JSONObject exportTo);
	
	/**
	 * Imports a bunch of stuff.
	 *
	 * @param report     An updatable data structure for reporting results into.
	 * @param elements   The JSON Object (as produced by exportElements()) to import
	 * @param simulation If true, do not actually import, just report what would happen.
	 * @see #exportElements(State,List)
	 */
	public void importElements(@Nonnull final State state,
	                           @Nonnull final ImportReport report,
	                           @Nonnull final JSONObject elements,
	                           final boolean simulation) {
		state.checkPermission("Transport.Import"+transportName());
		for (final String key: elements.keySet()) {
			importElement(state,report,key,elements.getJSONObject(key),simulation);
		}
	}
	
	/**
	 * Import an element ; inverse of exportElement
	 *
	 * @param report     Updatable report to write messages into
	 * @param name       Name of the element being imported
	 * @param element    The JSON Object of the element being imported
	 * @param simulation If true, don't actually import, just update the ImportReport
	 * @see #exportElement(State,String,JSONObject)
	 */
	protected abstract void importElement(@Nonnull final State state,
	                                      @Nonnull final ImportReport report,
	                                      @Nonnull final String name,
	                                      @Nonnull final JSONObject element,
	                                      final boolean simulation);
}
