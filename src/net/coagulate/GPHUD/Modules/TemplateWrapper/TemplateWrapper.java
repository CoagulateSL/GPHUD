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

	public TemplateWrapper(final String name,
                           final ModuleDefinition annotation) {
        super(name, annotation);
    }

	// ---------- STATICS ----------
	@Nullable
	public static String templateWrapper(final State st,
	                                     @Nonnull final String wrappedtemplate) {
		if (!st.hasModule("TemplateWrapper")) { return ""; }
		String template=trim(wrappedtemplate);
		if (template.startsWith("WRAPPED:")) {
			template=template.substring(8);
			final String ret=Templater.template(st,"--"+template+"--",false,false);
			if (ret==null || ret.isEmpty()) {
				return ret;
			}
			return st.getKV("TemplateWrapper."+template+"Prefix")+ret+st.getKV("TemplateWrapper."+template+"Postfix");
		}
		throw new SystemConsistencyException("Failed to resolve templateWrapper "+wrappedtemplate);
	}

	// ----- Internal Statics -----
	private static String trim(final String s) { return s.substring(2,s.length()-2); }

	// ---------- INSTANCE ----------
	@Nonnull
	public Map<String,String> getExternalTemplates(@Nonnull final State st) {
		if (st.externals!=null) { return st.externals; }
		final Map<String,String> list=new TreeMap<>(Templater.templates);
		final Map<String,String> listtrimmed=new TreeMap<>();
		for (final Module m: Modules.getModules()) {
			if (!"TemplateWrapper".equals(m.getName())) {
				m.addTemplateDescriptions(st, list);
			}
		}
		for (final Map.Entry<String,String> entry: list.entrySet()) {
			final String element=entry.getKey();
			listtrimmed.put(trim(element),entry.getValue());
		}
		st.externals=listtrimmed;
		return listtrimmed;
	}

	@Nonnull
	@Override
	public Map<String,KV> getKVDefinitions(@Nonnull final State st) {
		final Map<String, KV> kv = new TreeMap<>(super.getKVDefinitions(st));
		for (final String template: getExternalTemplates(st).keySet()) {
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
                                        @Nonnull final Map<String, String> cumulativeMap) {
        if (!st.hasModule("TemplateWrapper")) {
            return;
        }
        for (final String template : getExternalTemplates(st).keySet()) {
            cumulativeMap.put("--WRAPPED:" + template.toUpperCase() + "--", "Wrapped form of template " + template);
        }
    }

    @Override
    public void addTemplateMethods(@Nonnull final State st,
                                   @Nonnull final Map<String, Method> cumulativeMap) {
        if (!st.hasModule("TemplateWrapper")) {
            return;
        }
        for (final String template : getExternalTemplates(st).keySet()) {
            try {
                cumulativeMap.put("--WRAPPED:" + template.toUpperCase() + "--", getClass().getMethod("templateWrapper", State.class, String.class));
            } catch (@Nonnull final NoSuchMethodException | SecurityException ex) {
                SL.report("Templating referencing exception??", ex, st);
                st.logger().log(SEVERE, "Exception referencing own templating method??", ex);
            }
        }
	}
}
