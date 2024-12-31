package net.coagulate.GPHUD.Modules.Transport;

import java.util.ArrayList;
import java.util.List;

/** Just a simple class that accumulates log lines. */
public class ImportReport {
	private final List<String> errors       =new ArrayList<>();
	private final List<String> warnings     =new ArrayList<>();
	private final List<String> informational=new ArrayList<>();
	private final List<String> noops        =new ArrayList<>();
	
	public void error(final String error) {
		errors.add(error);
	}
	
	public void warn(final String warning) {
		warnings.add(warning);
	}
	
	public void info(final String information) {
		informational.add(information);
	}
	
	public void noop(final String noop) {
		noops.add(noop);
	}
	
	public List<String> errors() {
		return errors;
	}
	
	public List<String> warnings() {
		return warnings;
	}
	
	public List<String> informational() {
		return informational;
	}
	
	public List<String> noops() {
		return noops;
	}
}
