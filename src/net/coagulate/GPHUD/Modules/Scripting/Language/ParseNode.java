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

	public String toHtml() {
		String s="";
		if (children!=null) {
			s="<table border=1>";
			for (Node nnode : children) {
				SimpleNode node=(SimpleNode)nnode;
				s += "<tr><td><b>";
				s+= node.getClass().getName().replaceFirst("net.coagulate.GPHUD.Modules.Scripting.Language.Generated.","");
				s+="</b>";
				Token t=node.jjtGetFirstToken();
				if (t!=null) {
					boolean last=false;
					while (!last) {
						s+="<br>"+t.image;
						if (t==node.jjtGetLastToken()) { last=true; } else { t=t.next; }
					}
				}
				s+="</td><td>" + ((ParseNode) node).toHtml() + "</td></tr>";
			}
			s+="</table>";
		}
		return s;
	}

}
