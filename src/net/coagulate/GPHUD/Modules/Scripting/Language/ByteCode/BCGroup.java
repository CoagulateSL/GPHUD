package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCGroup extends ByteCodeDataType {
	@Nullable
	private CharacterGroup content=null;
	public BCGroup(ParseNode node) { super(node); }
	public BCGroup(ParseNode node, @Nullable CharacterGroup content) { super(node); this.content=content; }
	@Nonnull
	public String explain() { return "Group ("+content+")"; }
	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.Group.get());
		if (content==null) { bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff); bytes.add((byte)0xff);return; }
		addInt(bytes,content.getId());
	}
	@Nonnull
	@Override public String htmlDecode() { return "Avatar</td><td>"+getContent().getId(); }

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		vm.push(this);
	}

	@Nullable
	public CharacterGroup getContentNullable() { return content; }

	@Nonnull
	public CharacterGroup getContent() {
		if (content==null) { throw new SystemException("Group has no content (group)"); }
		return content;
	}

	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCGroup(node(),content);
	}
}
