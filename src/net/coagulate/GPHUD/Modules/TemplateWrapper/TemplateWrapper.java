package net.coagulate.GPHUD.Modules.TemplateWrapper;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import static java.util.logging.Level.SEVERE;

public class TemplateWrapper extends ModuleAnnotation {

	public TemplateWrapper(String name,
	                       ModuleDefinition def) {
		super(name,def);
	}

	private Map<String,String> externals=null;
	@Nonnull public Map<String,String> getExternalTemplates(@Nonnull final State st) {
		if (externals!=null) { return externals; }
		final boolean debug=false;
		Map<String,String> list=new TreeMap<>(Templater.templates);
		Map<String,String> listtrimmed=new TreeMap<>();
		for (Module m: Modules.getModules()) {
			if (!m.getName().equals("TemplateWrapper")) {
				m.addTemplateDescriptions(st,list);
				if (debug) { System.out.println("Probed module "+m+" size now "+list.size()); }
			}
		}
		for (Map.Entry<String,String> entry: list.entrySet()) {
			String element=entry.getKey();
			listtrimmed.put(trim(element),entry.getValue());
			if (debug) { System.out.println("Transferred "+element+" as "+trim(element)); }
		}
		externals=listtrimmed;
		return listtrimmed;
	}

	private static String trim(String s) { return s.substring(2,s.length()-2); }

	@Nonnull
	@Override
	public Map<String,KV> getKVDefinitions(@Nonnull final State st) {
		final Map<String,KV> kv=super.getKVDefinitions(st);
		for (final String template:getExternalTemplates(st).keySet()) {
			kv.put(template.toLowerCase()+"prefix",new TemplatePrefix(template));
			kv.put(template.toLowerCase()+"postfix",new TemplatePostfix(template));
		}
		return kv;
	}

	@Override
	public KV getKVDefinition(@Nonnull final State st,
	                          @Nonnull final String qualifiedname) {
		// avoid infinite loops as we look up definitions and try get our attributes to make more defintiions etc
		if (qualifiedname.toLowerCase().endsWith("prefix")) {
			return new TemplatePrefix(qualifiedname.substring(0,qualifiedname.length()-"prefix".length()));
		}
		if (qualifiedname.toLowerCase().endsWith("postfix")) {
			return new TemplatePostfix(qualifiedname.substring(0,qualifiedname.length()-"postfix".length()));
		}
		return super.getKVDefinition(st,qualifiedname);
	}

	@Override
	public void addTemplateDescriptions(@Nonnull final State st,
	                                    @Nonnull final Map<String,String> addto) {
		if (!st.hasModule("TemplateWrapper")) { return; }
		for (final String template:getExternalTemplates(st).keySet()) {
			addto.put("--WRAPPED:"+template.toUpperCase()+"--","Wrapped form of template "+template);
		}
	}

	@Override
	public void addTemplateMethods(@Nonnull final State st,
	                               @Nonnull final Map<String,Method> addto) {
		if (!st.hasModule("TemplateWrapper")) { return; }
		for (final String template:getExternalTemplates(st).keySet()) {
			try {
				addto.put("--WRAPPED:"+template.toUpperCase()+"--",getClass().getMethod("templateWrapper",State.class,String.class));
			}
			catch (@Nonnull final NoSuchMethodException|SecurityException ex) {
				SL.report("Templating referencing exception??",ex,st);
				st.logger().log(SEVERE,"Exception referencing own templating method??",ex);
			}
		}
	}

	@Nullable
	public static String templateWrapper(State st,
	                                       @Nonnull final String wrappedtemplate) {
		if (!st.hasModule("TemplateWrapper")) { return ""; }
		final boolean debug=false;
		String template=trim(wrappedtemplate);
		if (template.startsWith("WRAPPED:")) {
			template=template.substring(8);
			if (debug) { System.out.println(wrappedtemplate+" > "+template); }
			String ret=Templater.template(st,"--"+template+"--",false,false);
			if (debug) { System.out.println(wrappedtemplate+" > "+template+" > "+ret); }
			if (ret==null || ret.isEmpty()) {
				if (debug) { System.out.println("Returning empty ret"); }
				return ret;
			}
			String postret=st.getKV("TemplateWrapper."+template+"Prefix")+ret+st.getKV("TemplateWrapper."+template+"Postfix");
			if (debug) { System.out.println("Returning:"+postret); }
			return postret;
		}
		throw new SystemConsistencyException("Failed to resolve templateWrapper "+wrappedtemplate);
	}
}
