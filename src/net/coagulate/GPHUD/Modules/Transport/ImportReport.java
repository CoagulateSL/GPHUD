package net.coagulate.GPHUD.Modules.Transport;

import java.util.ArrayList;
import java.util.List;

/** Just a simple class that accumulates log lines. */
public class ImportReport {
	private final List<String> errors       =new ArrayList<>();
	private final List<String> warnings     =new ArrayList<>();
	private final List<String> informational=new ArrayList<>();
	private final List<String> noops        =new ArrayList<>();
	
	/**
	 * Logs an error
	 *
	 * @param error Error message to store
	 */
	public void error(final String error) {
		errors.add(error);
	}
	
	/**
	 * Logs a warning
	 *
	 * @param warning Warning message to store
	 */
	public void warn(final String warning) {
		warnings.add(warning);
	}
	
	/**
	 * Logs an informational message
	 *
	 * @param information Informational message to store
	 */
	public void info(final String information) {
		informational.add(information);
	}
	
	/**
	 * Logs a no-operation message
	 *
	 * @param noop No operation log entry to store
	 */
	public void noop(final String noop) {
		noops.add(noop);
	}
	
	/**
	 * Returns all errors
	 *
	 * @return All stored errors
	 */
	public List<String> errors() {
		return errors;
	}
	
	/**
	 * Returns all warnings
	 *
	 * @return All stored warnings
	 */
	public List<String> warnings() {
		return warnings;
	}
	
	/**
	 * Returns all informationals
	 *
	 * @return All stored informationals
	 */
	public List<String> informational() {
		return informational;
	}
	
	/**
	 * Return all no operations
	 *
	 * @return All stored no-operation events.
	 */
	public List<String> noops() {
		return noops;
	}
}
