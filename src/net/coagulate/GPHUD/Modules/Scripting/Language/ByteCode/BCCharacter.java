package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import java.util.List;

public class BCCharacter extends ByteCodeDataType {
	public BCCharacter(ParseNode n) { super(n); }
	private Char content=null; public Char getContent() { return content; }
	public BCCharacter(ParseNode n, Char content) { super(n); this.content=content; }
	public String explain() { return "Character ("+content+")"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Character.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content.getId());
	}
	@Override public String htmlDecode() { return "Character</td><td>"+content.getId(); }

	@Override
	public void execute(State st, GSVM vm, boolean simulation) {
		vm.push(this);
	}

	@Override
	public ByteCodeDataType add(ByteCodeDataType var) {
		if (var.getClass().equals(BCString.class)) { return new BCString(node(),
				toString() + var.toString()) ; }
		throw new GSInvalidExpressionException("Can't add BCCharacter + "+var.getClass().getSimpleName());
	}

	@Override
	public ByteCodeDataType subtract(ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can't subtract with BCCharacter");
	}

	@Override
	public ByteCodeDataType multiply(ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can't multiply with BCCharacter");
	}

	@Override
	public ByteCodeDataType divide(ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can't divide with BCCharacter");
	}

	@Override
	public BCString toBCString() {
		return new BCString(node(),content.getName());
	}

	@Override
	public ByteCodeDataType clone() {
		return new BCCharacter(node(),content);
	}
}
