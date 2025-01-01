package net.coagulate.GPHUD.Modules.Transport;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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
	
	/**
	 * Generic value importer function, for sub classes utility.
	 *
	 * Effectively checks if something needs changing, updates the report, simulates or writes the value.
	 *
	 * @param state      Caller's state, used in audit log entry
	 * @param simulation Are we simulating this import
	 * @param report     Report to update
	 * @param targetName Name of the target we're updating
	 * @param targetAttr Attribute on the named target we're updating
	 * @param oldvalue   The old value
	 * @param newvalue   The proposed new value
	 * @param writeValue Functional primitive to actually write the newvalue if necessary
	 */
	protected void importValue(@Nonnull final State state,
	                           final boolean simulation,
	                           @Nonnull final ImportReport report,
	                           @Nonnull final String targetName,
	                           @Nonnull final String targetAttr,
	                           @Nullable final Object oldvalue,
	                           @Nullable final Object newvalue,
	                           @Nonnull final Consumer writeValue) {
		importValue(state,simulation,report,targetName,targetAttr,oldvalue,newvalue,()->writeValue.accept(newvalue));
	}
	
	/**
	 * Generic value importer function, for sub classes utility.
	 *
	 * Effectively checks if something needs changing, updates the report, simulates or writes the value.
	 *
	 * @param state      Caller's state, used in audit log entry
	 * @param simulation Are we simulating this import
	 * @param report     Report to update
	 * @param targetName Name of the target we're updating
	 * @param targetAttr Attribute on the named target we're updating
	 * @param oldvalue   The old value
	 * @param newvalue   The proposed new value
	 * @param writeValue Functional primitive to actually write the newvalue if necessary
	 */
	protected void importValue(@Nonnull final State state,
	                           final boolean simulation,
	                           @Nonnull final ImportReport report,
	                           @Nonnull final String targetName,
	                           @Nonnull final String targetAttr,
	                           @Nullable final Object oldvalue,
	                           @Nullable final Object newvalue,
	                           @Nonnull final Runnable writeValue) {
		importValue(state,simulation,report,targetName,targetAttr,oldvalue,newvalue,writeValue,Object::equals);
	}
	
	/**
	 * Generic value importer function, for sub classes utility.
	 *
	 * Effectively checks if something needs changing, updates the report, simulates or writes the value.
	 *
	 * @param state      Caller's state, used in audit log entry
	 * @param simulation Are we simulating this import
	 * @param report     Report to update
	 * @param targetName Name of the target we're updating
	 * @param targetAttr Attribute on the named target we're updating
	 * @param oldvalue   The old value
	 * @param newvalue   The proposed new value
	 * @param writeValue Functional primitive to actually write the newvalue if necessary
	 * @param comparator Functional interface for comparison
	 */
	protected void importValue(@Nonnull final State state,
	                           final boolean simulation,
	                           @Nonnull final ImportReport report,
	                           @Nonnull final String targetName,
	                           @Nonnull final String targetAttr,
	                           @Nullable final Object oldvalue,
	                           @Nullable final Object newvalue,
	                           @Nonnull final Runnable writeValue,
	                           @Nonnull final BiFunction<Object,Object,Boolean> comparator) {
		if (newvalue==null&&oldvalue==null) {
			report.noop(transportName()+" - value for '"+targetName+"' - '"+targetAttr+"' has not changed");
			return;
		}
		if (newvalue!=null&&comparator.apply(newvalue,oldvalue)) {
			report.noop(transportName()+" - value for '"+targetName+"' - '"+targetAttr+"' has not changed");
			return;
		}
		if (simulation) {
			report.info(
					transportName()+" - "+targetName+" would update "+targetAttr+" from '"+oldvalue+"' to '"+newvalue+
					"'");
			return;
		}
		report.info(transportName()+" - "+targetName+" updated "+targetAttr+" from '"+oldvalue+"' to '"+newvalue+"'");
		writeValue.run();
		Audit.audit(state,
		            Audit.OPERATOR.AVATAR,
		            null,
		            null,
		            "Import "+transportName(),
		            targetName+" - "+targetAttr,
		            oldvalue!=null?oldvalue.toString():null,
		            newvalue!=null?newvalue.toString():null,
		            "Updated "+targetAttr+" via attribute import");
	}
	
	/**
	 * Check if the passed object exists and if not create it.
	 *
	 * Just a utility method for subclasses
	 *
	 * @param state      Callers state
	 * @param simulation Are we simulating?
	 * @param report     Report to update
	 * @param check      Object to check - we only do anything if this ended up as null
	 */
	protected void existCheck(@Nonnull final State state,
	                          final boolean simulation,
	                          @Nonnull final ImportReport report,
	                          @Nullable final Object check,
	                          @Nonnull final String name,
	                          @Nonnull final Runnable create) {
		if (check!=null) {
			return;
		}
		report.info(transportName()+" - Create new "+name);
		if (simulation) {
			return;
		}
		create.run();
		Audit.audit(state,
		            Audit.OPERATOR.AVATAR,
		            null,
		            null,
		            "Import "+transportName(),
		            "Create "+transportName(),
		            null,
		            name,
		            "Created new "+transportName()+" via import");
	}
}
