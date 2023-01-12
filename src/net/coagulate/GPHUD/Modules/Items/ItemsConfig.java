package net.coagulate.GPHUD.Modules.Items;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class ItemsConfig {
	
	@URL.URLs(url="/configuration/items", requiresPermission="Items.*")
	public static void configPage(@Nonnull final State st,@Nonnull final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Defined Items"));
		f.noForm();
		f.add(Item.getSummaryPage(st.getInstance()));
		if (st.hasPermission("Items.Edit")) {
			f.add(new Form(st,
			               true,
			               "/GPHUD/configuration/items/edititem",
			               "Create New Item",
			               "tradable",
			               "true",
			               "destroyable",
			               "true",
			               "weight",
			               "0"));
		}
		//f.add(new Separator());
		//Modules.get(st,"Items").kvConfigPage(st);
	}
	
	@URL.URLs(url="/configuration/items/edititem", requiresPermission="Items.Edit")
	public static void editItem(@Nonnull final State st,@Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Items.Edit",values);
	}
	
	@Nonnull
	@Command.Commands(description="Create or modify an item (based on name)",
	                  context=Command.Context.AVATAR,
	                  requiresPermission="Items.Edit",
	                  permitObject=false,
	                  permitExternal=false,
	                  permitScripting=false)
	public static Response edit(@Nonnull final State st,
	                            @Nonnull
	                            @Arguments(name="name",
	                                       description="Name of item",
	                                       type=Argument.ArgumentType.TEXT_ONELINE,
	                                       max=128) final String name,
	                            @Nonnull
	                            @Arguments(name="description",
	                                       description="Description of item",
	                                       type=Argument.ArgumentType.TEXT_ONELINE,
	                                       max=256) final String description,
	                            @Nonnull
	                            @Arguments(name="weight",
	                                       description="Weight of item (0 for no weight)",
	                                       type=Argument.ArgumentType.INTEGER) final Integer weight,
	                            @Nonnull
	                            @Arguments(name="tradable",
	                                       description="Players can trade item",
	                                       type=Argument.ArgumentType.BOOLEAN) final Boolean tradable,
	                            @Nonnull
	                            @Arguments(name="destroyable",
	                                       description="Players can destroy item from inventory",
	                                       type=Argument.ArgumentType.BOOLEAN) final Boolean destroyable) {
		final Item item=Item.findOrCreate(st,name);
		if (!item.description().equals(description)) {
			Audit.audit(false,
			            st,
			            Audit.OPERATOR.AVATAR,
			            null,
			            null,
			            "Item",
			            "Description",
			            item.description(),
			            description,
			            "Changed item '"+item.getName()+"' description");
			item.description(description);
		}
		if (item.weight()!=weight) {
			Audit.audit(false,
			            st,
			            Audit.OPERATOR.AVATAR,
			            null,
			            null,
			            "Item",
			            "Weight",
			            String.valueOf(item.weight()),
			            String.valueOf(weight),
			            "Changed item '"+item.getName()+"' weight");
			item.weight(weight);
		}
		if (item.tradable()!=tradable) {
			Audit.audit(false,
			            st,
			            Audit.OPERATOR.AVATAR,
			            null,
			            null,
			            "Item",
			            "Tradable",
			            String.valueOf(item.tradable()),
			            String.valueOf(tradable),
			            "Changed item '"+item.getName()+"' tradarable flag");
			item.tradable(tradable);
		}
		if (item.destroyable()!=destroyable) {
			Audit.audit(false,
			            st,
			            Audit.OPERATOR.AVATAR,
			            null,
			            null,
			            "Item",
			            "Destroyable",
			            String.valueOf(item.destroyable()),
			            String.valueOf(destroyable),
			            "Changed item '"+item.getName()+"' destoyable flag");
			item.destroyable(destroyable);
		}
		return new OKResponse("Item created/updated");
	}
	
	@URL.URLs(url="/configuration/items/*")
	public static void itemPage(@Nonnull final State st,@Nonnull final SafeMap values) {
		final Form f=st.form();
		final String id=st.getDebasedURL().substring("/configuration/items/".length());
		final Item item=Item.get(Integer.parseInt(id));
		if (item.getInstance()!=st.getInstance()) {
			throw new UserInputStateException("Item "+id+" is from a different instance");
		}
		f.add(new TextHeader("Item: "+item.getName()));
		f.add(new TextSubHeader("Properties"));
		f.noForm();
		final Table propertiesTable=new Table();
		f.add(propertiesTable);
		propertiesTable.openRow().add(new Cell("Name")).add(item.getName());
		propertiesTable.openRow().add(new Cell("Description")).add(item.description());
		propertiesTable.openRow().add("Weight").add(item.weight());
		propertiesTable.openRow().add("Tradable").add(item.tradable());
		propertiesTable.openRow().add("Destroyable").add(item.destroyable());
		if (st.hasPermission("Items.Edit")) {
			f.add(new Form(st,
			               true,
			               "/GPHUD/configuration/items/edititem",
			               "Edit Item",
			               "name",
			               item.getName(),
			               "tradable",
			               item.tradable()?"true":"",
			               "destroyable",
			               item.destroyable()?"true":"",
			               "weight",
			               String.valueOf(item.weight()),
			               "description",
			               item.description()));
		}
		if (st.hasPermission("Items.Delete")) {
			f.add(new Form(st,
			               true,
			               "/GPHUD/configuration/items/deleteitem",
			               "Delete Item",
			               "name",
			               item.getName(),
			               "okreturnurl",
			               "/GPHUD/configuration/items"));
		}
		f.add(new Separator());
		f.add(new TextSubHeader("Allowed In Inventories"));
		final Table inventories=new Table();
		f.add(inventories);
		for (final Attribute inventory: Inventory.getAll(st.getInstance())) {
			inventories.openRow();
			inventories.add(inventory.getName());
			final boolean allowed=Inventory.allows(st,inventory,item);
			if (allowed) {
				inventories.add("Permitted");
			} else {
				inventories.add("Forbidden");
			}
			if (st.hasPermission("Items.EditInventories")) {
				inventories.add(new Form(st,
				                         true,
				                         "/GPHUD/configuration/items/inventoryallow",
				                         "Toggle",
				                         "item",
				                         item.getName(),
				                         "inventory",
				                         inventory.getName(),
				                         "allowed",
				                         allowed?"":"true"));
			}
		}
		f.add(new Separator());
		f.add(new TextSubHeader("Available Actions"));
		if (!values.get("newaction").isBlank()) {
			final String newAction=values.get("newaction");
			if ("Give To".equalsIgnoreCase(newAction)||"Destroy".equalsIgnoreCase(newAction)||
			    "Move To".equalsIgnoreCase(newAction)) {
				f.add(new TextError("Can not create new action "+newAction+", this verb is reserved"));
			} else {
				try {
					ItemVerb.create(item,newAction);
				} catch (final UserException e) {
					f.add(new TextError(e.getLocalizedMessage()));
				}
			}
		}
		final Table verbs=new Table();
		f.add(verbs);
		for (final ItemVerb verb: ItemVerb.findAll(item)) {
			verbs.openRow();
			verbs.add(new Link(verb.getName(),"/GPHUD/configuration/items/verbs/"+verb.getId()));
			verbs.add(verb.description());
			verbs.add(VerbActor.decode(verb));
			verbs.add(new Form(st,
			                   true,
			                   "/GPHUD/configuration/items/deleteverb",
			                   "Delete",
			                   "item",
			                   item.getName(),
			                   "verb",
			                   verb.getName()));
		}
		final Form newAction=new Form();
		newAction.add(new TextInput("newaction")).add(new Button("Create"));
		f.add(newAction);
		f.add(new Separator());
	}
	
	@URL.URLs(url="/configuration/items/inventoryallow", requiresPermission="Items.EditInventories")
	public static void editInventoriesPage(@Nonnull final State st,@Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Items.EditInventories",values);
	}
	
	@Nonnull
	@Command.Commands(description="Alter an items allowed state in an inventory",
	                  context=Command.Context.AVATAR,
	                  requiresPermission="Items.EditInventories",
	                  permitObject=false,
	                  permitExternal=false,
	                  permitScripting=false)
	public static Response editInventories(@Nonnull final State st,
	                                       @Nonnull
	                                       @Arguments(name="item", description="Item", type=Argument.ArgumentType.ITEM)
	                                       final Item item,
	                                       @Nonnull
	                                       @Arguments(name="inventory",
	                                                  description="Inventory",
	                                                  type=Argument.ArgumentType.INVENTORY) final Attribute inventory,
	                                       @Nonnull
	                                       @Arguments(name="allowed",
	                                                  description="Item allowed",
	                                                  type=Argument.ArgumentType.BOOLEAN) final Boolean allowed) {
		Inventory.allows(st,inventory,item,allowed);
		return new OKResponse("Item inventory allowed status updated");
	}
	
	
	@URL.URLs(url="/configuration/items/deleteitem", requiresPermission="Items.Delete")
	public static void deleteForm(@Nonnull final State st,@Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Items.Delete",values);
	}
	
	@Nonnull
	@Command.Commands(description="Delete an item",
	                  context=Command.Context.AVATAR,
	                  requiresPermission="Items.Delete",
	                  permitObject=false,
	                  permitExternal=false,
	                  permitScripting=false)
	public static Response delete(@Nonnull final State st,
	                              @Nonnull
	                              @Arguments(name="name",
	                                         description="Item to delete",
	                                         type=Argument.ArgumentType.ITEM,
	                                         max=128) final Item name) {
		if (name.getInstance()!=st.getInstance()) {
			throw new SystemImplementationException("Item instance / state instance mismatch");
		}
		final String itemName=name.getName();
		name.delete();
		Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"Delete","Item",itemName,null,"Deleted item "+itemName);
		return new OKResponse("Item "+itemName+" deleted");
	}
	
}
