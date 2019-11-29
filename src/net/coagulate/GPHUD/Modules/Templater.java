package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.SL;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * Does the templating and mathematical operations on templated values.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Templater {

	private static final Map<String, String> templates = new TreeMap<>();
	private static final Map<String, Method> methods = new TreeMap<>();

	private static void add(String key, String description, Method method) {
		templates.put("--" + key + "--", description);
		methods.put("--" + key + "--", method);
	}

	public static Map<String, String> getTemplates(State st) {
		Map<String, String> ret = new TreeMap<>();
		ret.putAll(templates);
		for (Module m : Modules.getModules()) {
			m.addTemplateDescriptions(st, ret);
		}
		return ret;
	}

	public static Map<String, Method> getMethods(State st) {
		Map<String, Method> ret = new TreeMap<>();
		ret.putAll(methods);
		for (Module m : Modules.getModules()) {
			m.addTemplateMethods(st, ret);
		}
		return ret;
	}

	public static void register(Template template, Method m) {
		add(template.name(), template.description(), m);
	}

	public static Method getMethod(State st, String name) { return getMethods(st).get(name); }

	public static String template(State st, String string, boolean evaluate, boolean integer) {
		string = template(st, string);
		if (string == null) { return string; }
		if ("".equals(string)) { return ""; }
		try {
			if (evaluate && !integer) { return eval(string) + ""; }
			if (evaluate && integer) { return "" + ((int) (eval(string))); }
		} catch (Exception e) {
			SL.report("Expression failed for " + string, e, st);
			st.logger().log(WARNING, "Failed to complete expression evaluation for '" + string + "' - we got error " + e.getMessage(), e);
			throw e;
		}
		return string;
	}

	private static String template(State st, String string) throws UserException {
		if (string == null) { return string; }
		if (st == null) { throw new SystemException("Null session state is not permitted"); }
		boolean debug = false;
		for (String subst : getTemplates(st).keySet()) {
			if (string.contains(subst)) {
				if (debug) { System.out.println("Check: " + subst); }
				if (debug) { System.out.println("Pre: " + string); }
				String value = "ERROR";
				try { value = getValue(st, subst); } catch (UserException e) { value = "Error: " + e.getMessage(); }
				if (debug) { System.out.println("REPLACE: " + subst + " with " + value); }
				string = string.replaceAll(subst, Matcher.quoteReplacement(value));
				if (debug) { System.out.println("Post: " + string); }
			}
		}
		return string;
	}

	public static String getValue(State st, String keyword, boolean evaluate, boolean integer) {
		if (evaluate && integer) { return ((int) eval(getValue(st, keyword))) + ""; }
		if (evaluate) { return eval(getValue(st, keyword)) + ""; }
		return getValue(st, keyword);
	}

	private static String getValue(State st, String keyword) {
		Method m = getMethods(st).get(keyword);
		if (m != null) {
			try {
				return (String) m.invoke(null, st, keyword);
			} catch (IllegalAccessException | IllegalArgumentException ex) {
				SL.report("Templating exception", ex, st);
				st.logger().log(SEVERE, "Exception running templater method", ex);
				throw new SystemException("Templater exceptioned", ex);
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof UserException) { throw (UserException) e.getCause(); }
				if (e.getCause() instanceof SystemException) { throw (SystemException) e.getCause(); }
				throw new SystemException("Unable to invoke target", e);
			}
		}
		throw new SystemException("No template implementation for " + keyword);
	}

	@Template(name = "NAME", description = "Character Name")
	public static String getCharacterName(State st, String key) {
		if (st.getCharacterNullable() == null) { return ""; }
		return st.getCharacter().getName();
	}

	// some standard templates

	@Template(name = "AVATAR", description = "Avatar Name")
	public static String getAvatarName(State st, String key) {
		if (st.avatar() == null) { return ""; }
		return st.avatar().getName();
	}

	@Template(name = "NEWLINE", description = "Newline character")
	public static String newline(State st, String key) { return "\n"; }

	//https://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form
	// boann@stackoverflow.com
	public static double eval(final String str) throws UserException {
		return new Object() {
			int pos = -1, ch;

			void nextChar() {
				ch = (++pos < str.length()) ? str.charAt(pos) : -1;
			}

			boolean eat(int charToEat) {
				while (ch == ' ') nextChar();
				if (ch == charToEat) {
					nextChar();
					return true;
				}
				return false;
			}

			double parse() {
				nextChar();
				double x = parseExpression();
				if (pos < str.length())
					throw new UserException("Unexpected: " + (char) ch + " at position " + pos + " in '" + str + "'");
				return x;
			}

			// Grammar:
			// expression = term | expression `+` term | expression `-` term
			// term = factor | term `*` factor | term `/` factor
			// factor = `+` factor | `-` factor | `(` expression `)`
			//        | number | functionName factor | factor `^` factor

			double parseExpression() {
				double x = parseTerm();
				for (; ; ) {
					if (eat('+')) x += parseTerm(); // addition
					else if (eat('-')) x -= parseTerm(); // subtraction
					else return x;
				}
			}

			double parseTerm() {
				double x = parseFactor();
				for (; ; ) {
					if (eat('*')) x *= parseFactor(); // multiplication
					else if (eat('/')) x /= parseFactor(); // division
					else return x;
				}
			}

			double parseFactor() {
				if (eat('+')) return parseFactor(); // unary plus
				if (eat('-')) return -parseFactor(); // unary minus

				double x;
				int startPos = this.pos;
				if (eat('(')) { // parentheses
					x = parseExpression();
					eat(')');
				} else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
					while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
					x = Double.parseDouble(str.substring(startPos, this.pos));
				} else if (ch >= 'a' && ch <= 'z') { // functions
					while (ch >= 'a' && ch <= 'z') nextChar();
					String func = str.substring(startPos, this.pos);
					x = parseFactor();
					if ("sqrt".equals(func)) x = Math.sqrt(x);
					else if ("sin".equals(func)) x = Math.sin(Math.toRadians(x));
					else if ("cos".equals(func)) x = Math.cos(Math.toRadians(x));
					else if ("tan".equals(func)) x = Math.tan(Math.toRadians(x));
					else throw new UserException("Unknown function: " + func);
				} else {
					throw new UserException("Unexpected: " + (char) ch + " at " + pos + " in '" + str + "'");
				}

				if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

				return x;
			}
		}.parse();
	}


	/**
	 * Defined a method that returns a templateable element.
	 * Must be public static and return a String with a singular State argument
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	public @interface Template {
		String name();

		String description();
	}

}
