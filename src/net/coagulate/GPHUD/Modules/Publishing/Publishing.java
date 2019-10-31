package net.coagulate.GPHUD.Modules.Publishing;

import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.State;

/** Some generally useful methods for publishing */
public class Publishing {
	static int getPartInt(State st,int part) {
		return Integer.parseInt(getPart(st,part));
	}
	static String getPart(State st,int part) {
		String[] split=st.getDebasedNoQueryURL().split("/");
		if ((part+2)>=split.length) { throw new UserException("Missing identifier "+part); }
		return split[part+2];
	}

	static void contentResizer(State st) {st.form.add("<script type=\"text/javascript\" src=\"https://sl.coagulate.net/resources/iframeResizer.contentWindow.min.js\"></script>");}

	static void published(State st,String inline) {
		String id="GPHudFrame"+((int)((Math.random()*1000000.0)));
		Form f=st.form;
		example(f,"<style>\n" +
				"  iframe {\n" +
				"    width: 1px;\n" +
				"    height: 1px;\n" +
				"    min-width: 100%;\n" +
				"  }\n" +
				"</style>\n" +
				"<iframe frameBorder=0 src=\"https://sl.coagulate.net/GPHUD/published/"+inline+"\" id=\""+id+"\"></iframe>" +
				"<script type=\"text/javascript\" src=\"https://sl.coagulate.net/resources/iframeResizer.min.js\"></script>"+
				"<script>\n" +
				"  iFrameResize({ log: false }, '#"+id+"')\n" +
				"</script>");
	}
	private static void example(Form f,String s) {
		if (GPHUD.DEV) { s=s.replaceAll("sl\\.coagulate\\.net","sldev.coagulate.net"); }
		f.add("<p><b>Copy paste the following HTML into your page:</b><br><pre style=\"border: 1;\">"+s.replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\n","<br>")+"</pre></p>");
		f.add("<p>This will insert the content below (between the lines) into your document</p>");
		f.add("<hr>");
		f.add(s);
		f.add("<hr>");
	}

}