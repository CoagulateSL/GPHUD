package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;

import java.util.List;

public class BCString extends ByteCodeDataType {
	private String content=""; public String getContent() { return content; }
	public BCString(ParseNode n) {super(n);};
	public BCString(ParseNode n,String content) {super(n);this.content=content; }
	public String explain() { return "String ("+content+")"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.String.get());
		addShort(bytes,content.length());
		for (char c:content.toCharArray()) {
			bytes.add((byte)c);
		}
	}
	@Override public String htmlDecode() { return "String</td><td>"+content; }

	@Override
	public void execute(GSVM vm) {
		vm.push(this);
	}

	@Override
	public ByteCodeDataType add(ByteCodeDataType var) {
		Class type=var.getClass();
		// <STRING> | <RESPONSE> | <INT> | <CHARACTER> | <AVATAR> | <GROUP> | "List"
		if (type.equals(BCList.class)) { // special case, return a list
			return new BCList(node(),this).add(var);
		}
		// for everything else
		return new BCString(node(),this.content+(var.toBCString()));
	}

	@Override
	public ByteCodeDataType subtract(ByteCodeDataType var) {
		// if the 2nd type is a number then we'll do number stuff :P
		if (var.getClass().equals(BCInteger.class)) {
			toBCInteger().subtract(var);
		}
		throw new GSInvalidExpressionException("Can not perform BCString - "+var.getClass().getSimpleName());
	}

	@Override
	public ByteCodeDataType multiply(ByteCodeDataType var) {
		// if the 2nd type is a number then we'll do number stuff :P
		if (var.getClass().equals(BCInteger.class)) {
			toBCInteger().subtract(var);
		}
		throw new GSInvalidExpressionException("Can not perform BCString * "+var.getClass().getSimpleName());
	}

	@Override
	public ByteCodeDataType divide(ByteCodeDataType var) {
		// if the 2nd type is a number then we'll do number stuff :P
		if (var.getClass().equals(BCInteger.class)) {
			toBCInteger().subtract(var);
		}
		throw new GSInvalidExpressionException("Can not perform BCString / "+var.getClass().getSimpleName());
	}

	@Override
	public ByteCodeDataType clone() {
		return new BCString(node(),content);
	}

}
