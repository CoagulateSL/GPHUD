package net.coagulate.GPHUD.Modules.Transport;

import java.util.ArrayList;
import java.util.List;

public class ImportReport {
	private final List<String> errors  =new ArrayList<>();
	private final List<String> warnings=new ArrayList<>();
	private final List<String> informational=new ArrayList<>();
	
	public void error(final String error) { errors.add(error); }
	public void warn(final String warning) { warnings.add(warning); }
	public void info(final String information) { informational.add(information); }
}
