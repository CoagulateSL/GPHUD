package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.GSParser;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.Node;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.SimpleNode;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.Token;

public class ParseNode extends net.coagulate.GPHUD.Modules.Scripting.Language.Generated.SimpleNode {
	public ParseNode(int i) {
		super(i);
	}

	public ParseNode(GSParser p, int i) {
		super(p, i);
	}

	public int children() { return jjtGetNumChildren(); }
	public ParseNode child(int i) { return ((ParseNode)jjtGetChild(i)); }

	public String tokens() {
		String s="";
		Token t=jjtGetFirstToken();
		if (t!=null) {
			boolean last=false;
			while (!last) {
				if (!s.isEmpty()) { s+=" "; }
				s+=t.image;
				if (t==jjtGetLastToken()) { last=true; } else { t=t.next; }
			}
		}
		return s;
	}

	public String toHtml() {
		String s="";
		if (children!=null) {
			s="<ul>";
			for (Node nnode : children) {
				SimpleNode node=(SimpleNode)nnode;
				s += "<li><b>";
				s+= node.getClass().getName().replaceFirst("net.coagulate.GPHUD.Modules.Scripting.Language.Generated.","");
				s+=" : </b><i>";
				Token t=node.jjtGetFirstToken();
				if (t!=null) {
					boolean last=false;
					while (!last) {
						s+=" "+t.image;
						if (t==node.jjtGetLastToken()) { last=true; } else { t=t.next; }
					}
				}
				s+="</i>";
				s+=((ParseNode) node).toHtml();
				s+="</li>";
			}
			s+="</ul>";
		}
		return s;
	}

}
