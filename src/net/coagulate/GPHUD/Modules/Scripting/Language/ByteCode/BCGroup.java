package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;

import java.util.List;

public class BCGroup extends ByteCodeDataType {
	private CharacterGroup content=null;
	public BCGroup(ParseNode node) { super(node); }
	public BCGroup(ParseNode node,CharacterGroup content) { super(node); this.content=content; }
	public String explain() { return "Group ("+content+")"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Group.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content.getId());
	}
	@Override public String htmlDecode() { return "Avatar</td><td>"+content.getId(); }

	@Override
	public void execute(GSVM vm) {
		vm.push(this);
	}

	public CharacterGroup getContent() { return content; }

	@Override
	public ByteCodeDataType clone() {
		return new BCGroup(node(),content);
	}
}
