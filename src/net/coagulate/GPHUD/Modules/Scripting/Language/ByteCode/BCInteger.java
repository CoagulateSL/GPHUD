package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;

import java.util.List;

public class BCInteger extends ByteCodeDataType {
	private Integer content=0xffffffff;
	public BCInteger() {}
	public BCInteger(Integer content) { this.content=content; }

	public BCInteger(String tokens) { this.content=Integer.parseInt(tokens); }

	public String explain() { return "Integer("+content+") (push)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Integer.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content);
	}
	@Override public String htmlDecode() { return "Integer</td><td>"+content; }

	@Override
	public void execute(GSVM vm) {
		vm.push(this);
	}

	public int getContent() { return content; }

	@Override
	public ByteCodeDataType add(ByteCodeDataType var) {
		// if the other is a String, we'll just be doing that
		if (var.getClass().equals(BCString.class)) { return toBCString().add(var); }
		// if the other is a Float, we should cast down to it.  but that's not how we do things yet.
		return new BCInteger(toInteger()+var.toInteger());
	}

	@Override
	public ByteCodeDataType subtract(ByteCodeDataType var) {
		//check float, eventually
		return new BCInteger(toInteger()-var.toInteger());
	}

	@Override
	public ByteCodeDataType multiply(ByteCodeDataType var) {
		//check float, eventually
		return new BCInteger(toInteger()-var.toInteger());
	}

	@Override
	public ByteCodeDataType divide(ByteCodeDataType var) {
		//check float, eventually
		return new BCInteger(toInteger()-var.toInteger());
	}

	@Override
	public ByteCodeDataType clone() {
		return new BCInteger(content);
	}
}
