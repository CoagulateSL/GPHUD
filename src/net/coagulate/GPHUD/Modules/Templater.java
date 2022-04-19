package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;

/**
 * Does the templating and mathematical operations on templated values.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Templater {

	public static final Map<String,String> templates=new TreeMap<>();
	private static final Map<String,Method> methods=new TreeMap<>();

	// ---------- STATICS ----------

	/**
	 * Returns a list of templates and their descriptions.
	 *
	 * @param st The calling state
	 *
	 * @return A map of String to String, template name mapping to template description
	 */
	@Nonnull
	public static Map<String,String> getTemplates(final State st) {
		final Map<String,String> ret=new TreeMap<>(templates);
		for (final Module m: Modules.getModules()) {
			m.addTemplateDescriptions(st,ret);
		}
		return ret;
	}

	@Nonnull
	public static Map<String,Method> getMethods(final State st) {
		final Map<String,Method> ret=new TreeMap<>(methods);
		for (final Module m: Modules.getModules()) {
			m.addTemplateMethods(st,ret);
		}
		return ret;
	}

	public static void register(@Nonnull final Template template,
	                            final Method m) {
		add(template.name(),template.description(),m);
	}

	public static Method getMethod(final State st,
	                               final String name) { return getMethods(st).get(name); }

	@Nullable
	public static String template(@Nonnull final State st,
	                              @Nullable String string,
	                              final boolean evaluate,
	                              final boolean integer) {
		string=template(st,string);
		if (string == null) {
			return null;
		}
		if (string.isEmpty()) {
			return "";
		}
		try {
			if (evaluate && !integer) {
				return String.valueOf(eval(string));
			}
			//noinspection ConstantConditions
			if (evaluate && integer) {
				return String.valueOf((int) (eval(string)));
			}
		}
		catch (@Nonnull final Exception e) {
			SL.report("Expression failed for "+string,e,st);
			st.logger().log(Level.WARNING,"Failed to complete expression evaluation for '"+string+"' - we got error "+e.getMessage(),e);
			throw e;
		}
		return string;
	}

	@Nonnull
	public static String getValue(@Nonnull final State st,
	                              final String keyword,
	                              final boolean evaluate,
	                              final boolean integer) {
		if (evaluate && integer) {
			return String.valueOf((int) eval(getValue(st, keyword)));
		}
		if (evaluate) {
			return String.valueOf(eval(getValue(st, keyword)));
		}
		return getValue(st, keyword);
	}

	@Nonnull
	@Template(name="NAME",
	          description="Character Name")
	public static String getCharacterName(@Nonnull final State st,
	                                      final String key) {
		if (st.getCharacterNullable()==null) { return ""; }
		return st.getCharacter().getName();
	}

	@Template(name="AVATAR",
	          description="Avatar Name")
	public static String getAvatarName(@Nonnull final State st,
	                                   final String key) {
		if (st.getAvatarNullable()==null) { return ""; }
		return st.getAvatarNullable().getName();
	}

	@Nonnull
	@Template(name="NEWLINE",
	          description="Newline character")
	public static String newline(final State st,
	                             final String key) { return "\n"; }

	//https://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form
	// boann@stackoverflow.com
	public static double eval(@Nonnull final String str) {
		return new Object() {
			int pos=-1;
			int ch;

			// ----- Internal Instance -----
			void nextChar() {
				ch=(++pos<str.length())?str.charAt(pos):-1;
			}

			boolean eat(final int charToEat) {
				while (ch==' ') { nextChar(); }
				if (ch==charToEat) {
					nextChar();
					return true;
				}
				return false;
			}

			double parse() {
				nextChar();
				final double x=parseExpression();
				if (pos<str.length()) { throw new UserInputValidationParseException("Unexpected: "+(char) ch+" at position "+pos+" in '"+str+"'"); }
				return x;
			}

			// Grammar:
			// expression = term | expression `+` term | expression `-` term
			// term = factor | term `*` factor | term `/` factor
			// factor = `+` factor | `-` factor | `(` expression `)`
			//        | number | functionName factor | factor `^` factor

			double parseExpression() {
				double x=parseTerm();
				for (;;) {
					if (eat('+')) {
						x+=parseTerm(); // addition
					}
					else {
						if (eat('-')) {
							x-=parseTerm(); // subtraction
						}
						else { return x; }
					}
				}
			}

			double parseTerm() {
				double x=parseFactor();
				for (;;) {
					if (eat('*')) {
						x*=parseFactor(); // multiplication
					}
					else {
						if (eat('/')) {
							x/=parseFactor(); // division
						}
						else { return x; }
					}
				}
			}

			double parseFactor() {
				if (eat('+')) {
					return parseFactor(); // unary plus
				}
				if (eat('-')) {
					return -parseFactor(); // unary minus
				}

				double x;
				final int startPos=pos;
				if (eat('(')) { // parentheses
					x=parseExpression();
					eat(')');
				}
				else {
					if ((ch >= '0' && ch<='9') || ch=='.') { // numbers
						while ((ch >= '0' && ch<='9') || ch=='.') { nextChar(); }
						x=Double.parseDouble(str.substring(startPos,pos));
					}
					else {
						if (ch >= 'a' && ch<='z') { // functions
							while (ch >= 'a' && ch <= 'z') {
								nextChar();
							}
							final String func = str.substring(startPos, pos);
							x = parseFactor();
							x = switch (func) {
								case "sqrt" -> Math.sqrt(x);
								case "sin" -> Math.sin(Math.toRadians(x));
								case "cos" -> Math.cos(Math.toRadians(x));
								case "tan" -> Math.tan(Math.toRadians(x));
								default -> throw new UserInputValidationParseException("Unknown function: " + func);
							};
						}
						else {
							throw new UserInputValidationParseException("Unexpected: "+(char) ch+" at "+pos+" in '"+str+"'",true);
						}
					}
				}

				if (eat('^')) {
					x=Math.pow(x,parseFactor()); // exponentiation
				}

				return x;
			}
		}.parse();
	}

	// some standard templates

	// ----- Internal Statics -----
	private static void add(final String key,
	                        final String description,
	                        final Method method) {
		templates.put("--"+key+"--",description);
		methods.put("--"+key+"--",method);
	}

	@Nullable
	private static String template(@Nonnull final State st,
	                               @Nullable String string) {
		if (string==null) { return null; }
		for (final String subst: getTemplates(st).keySet()) {
			if (string.contains(subst)) {
				final String value;
				value=getValue(st,subst);
				string=string.replaceAll(subst,Matcher.quoteReplacement(value));
			}
		}
		return string;
	}

	@Nonnull
	private static String getValue(@Nonnull final State st,
	                               final String keyword) {
		if (Thread.currentThread().getStackTrace().length>150) { throw new UserConfigurationException("Recursion detected loading template "+keyword+" for "+st); }
		final Method m=getMethods(st).get(keyword);
		if (m!=null) {
			try {
				return (String) m.invoke(null,st,keyword);
			}
			catch (@Nonnull final IllegalAccessException|IllegalArgumentException ex) {
				SL.report("Templating exception",ex,st);
				st.logger().log(Level.SEVERE,"Exception running templater method",ex);
				throw new SystemImplementationException("Templater exceptioned",ex);
			}
			catch (@Nonnull final InvocationTargetException e) {
				if (UserException.class.isAssignableFrom(e.getCause().getClass())) { //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
					throw (UserException) e.getCause();
				}
				if (SystemException.class.isAssignableFrom(e.getCause().getClass())) { //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
					throw (SystemException) e.getCause();
				}
				throw new SystemImplementationException("Unable to invoke target", e);
			}
		}
		throw new SystemImplementationException("No template implementation for "+keyword);
	}


	/**
	 * Defined a method that returns a templateable element.
	 * Must be public static and return a String with a singular State argument
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	public @interface Template {
		// ---------- INSTANCE ----------
		@Nonnull String name();

		@Nonnull String description();
	}

}
