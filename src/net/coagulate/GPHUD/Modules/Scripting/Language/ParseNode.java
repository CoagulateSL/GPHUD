package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSParser;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.Node;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.SimpleNode;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.Token;

import javax.annotation.Nonnull;

public class ParseNode extends net.coagulate.GPHUD.Modules.Scripting.Language.Generated.SimpleNode {
	public ParseNode(final int i) {
		super(i);
	}

	public ParseNode(final GSParser p,
	                 final int i) {
		super(p,i);
	}

	// ---------- INSTANCE ----------
	public int children() { return jjtGetNumChildren(); }

	@Nonnull
	public ParseNode child(final int i) { return ((ParseNode) jjtGetChild(i)); }

	@Nonnull
	public String tokens() {
		final StringBuilder s=new StringBuilder();
		Token t=jjtGetFirstToken();
		if (t!=null) {
			boolean last=false;
			while (!last) {
				if (s.length()>0) { s.append(" "); }
				s.append(t.image);
				if (t==jjtGetLastToken()) { last=true; }
				else { t=t.next; }
			}
		}
		return s.toString();
	}

	@Nonnull
	public String toHtml() {
		StringBuilder s=new StringBuilder();
		if (children!=null) {
			s=new StringBuilder("<ul>");
			for (final Node nnode: children) {
				final SimpleNode node=(SimpleNode) nnode;
				s.append("<li><b>");
				s.append(node.getClass().getName().replaceFirst("net.coagulate.GPHUD.Modules.Scripting.Language.Generated.",""));
				s.append(" : </b><i>");
				Token t=node.jjtGetFirstToken();
				if (t!=null) {
					boolean last=false;
					while (!last) {
						s.append(" ").append(t.image);
						if (t==node.jjtGetLastToken()) { last=true; }
						else { t=t.next; }
					}
				}
				s.append("</i>");
				s.append(((ParseNode) node).toHtml());
				s.append("</li>");
			}
			s.append("</ul>");
		}
		return s.toString();
	}

}
