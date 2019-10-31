package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;

import java.util.List;

public class BCCharacter extends ByteCodeDataType {
	private Char content=null; public Char getContent() { return content; }
	public BCCharacter() {}
	public BCCharacter(Char content) { this.content=content; }
	public String explain() { return "Character("+content+") (push)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Character.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content.getId());
	}
	@Override public String htmlDecode() { return "Avatar</td><td>"+content.getId(); }

	@Override
	public void execute(GSVM vm) {
		vm.push(this);
	}

	@Override
	public ByteCodeDataType add(ByteCodeDataType var) {
		if (var.getClass().equals(BCString.class)) { return new BCString(
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
		return new BCString(content.getName());
	}

	@Override
	public ByteCodeDataType clone() {
		return new BCCharacter(content);
	}
}
