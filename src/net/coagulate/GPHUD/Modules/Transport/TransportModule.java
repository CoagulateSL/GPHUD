package net.coagulate.GPHUD.Modules.Transport;

import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class TransportModule extends ModuleAnnotation {
	
	private static final Transporter[] transports={
	
	};  // an importantly ordered list, dependancies in the DB
	
	
	public TransportModule(final String name,final ModuleDefinition annotation) {
		super(name,annotation);
	}
	
	@URL.URLs(url="/transport")
	public static void transportPage(@Nonnull final State st,final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Transport Module"));
		f.add(new Paragraph(
				"The transport module provides a mechanism for importing or exporting data.  This can be used to copy CONFIGURATION data between instances or even different GPHUD servers."));
		f.add(new Paragraph("Note that currently the transport of character data is not supported"));
		f.add(new Link("Import Settings","/transport/import"));
		f.add(new Link("Export Settings","/transport/import"));
	}
	
	@SideSubMenu.SideSubMenus(name="Import", priority=1, requiresPermission="transport.*")
	@URL.URLs(url="/transport/import")
	public static void importPage(@Nonnull final State st,final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Transport Module, Import"));
		f.add(new Paragraph("This page can be used to Import saved settings."));
		f.p("Please note the import is MERGED into your current system ; new items are created, items with the same name will be overwritten, items not present in the import will not be deleted.");
		f.p("It is <b>STRONGLY</b> recommended you run the import in preview mode first and review any errors/warnings");
	}
	
	@SideSubMenu.SideSubMenus(name="Export", priority=2, requiresPermission="transport.*")
	@URL.URLs(url="/transport/export")
	public static void exportPage(@Nonnull final State st,final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Transport Module, Export"));
		f.p("Here you may download specific items or whole sections of your configuration which can then be imported later.");
	}
	
	/*
	 * Mark tables off below when added to import/export
	 * +-------------------------+
	 * | Tables_in_gphud         |
	 * +-------------------------+
	 * | adminnotes              |
	 * | aliases                 |
	 * | attributes              |
	 * | audit                   |
	 * | charactergroupkvstore   |
	 * | charactergroupmembers   |
	 * | charactergroups         |
	 * | characterkvstore        |
	 * | characterpools          |
	 * | characters              |
	 * | charactersets           |
	 * | cookies                 |
	 * | currencies              |
	 * | currencycoins           |
	 * | effects                 |
	 * | effectsapplications     |
	 * | effectskvstore          |
	 * | events                  |
	 * | eventskvstore           |
	 * | eventslocations         |
	 * | eventsschedule          |
	 * | eventvisits             |
	 * | instancedevelopers      |
	 * | instancekvstore         |
	 * | instances               |
	 * | iteminventories         |
	 * | items                   |
	 * | itemverbs               |
	 * | landmarks               |
	 * | menus                   |
	 * | messages                |
	 * | objects                 |
	 * | objecttypes             |
	 * | permissions             |
	 * | permissionsgroupmembers |
	 * | permissionsgroups       |
	 * | ping                    |
	 * | primarycharacters       |
	 * | regionkvstore           |
	 * | regions                 |
	 * | schemaversions          |
	 * | scriptruns              |
	 * | scripts                 |
	 * | visits                  |
	 * | zoneareas               |
	 * | zonekvstore             |
	 * | zones                   |
	 * +-------------------------+
	 */
}
