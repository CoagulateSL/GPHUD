package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCGroup extends ByteCodeDataType {
	@Nullable private CharacterGroup content;
	
	public BCGroup(final ParseNode node) {
		super(node);
	}
	
	public BCGroup(final ParseNode node,@Nullable final CharacterGroup content) {
		super(node);
		this.content=content;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String explain() {
		return "Group ("+content+")";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Group.get());
		if (content==null) {
			bytes.add((byte)0xff);
			bytes.add((byte)0xff);
			bytes.add((byte)0xff);
			bytes.add((byte)0xff);
			return;
		}
		addInt(bytes,content.getId());
	}
	
	@Nonnull
	@Override
	public String htmlDecode() {
		return "Avatar</td><td>"+getContent().getId();
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		vm.push(this);
	}
	
	@Nonnull
	public CharacterGroup getContent() {
		if (content==null) {
			throw new GSInternalError("Group has no content (group)");
		}
		return content;
	}
	
	@Nullable
	public CharacterGroup getContentNullable() {
		return content;
	}
	
	@Nullable
	@Override
	public ByteCodeDataType clone() {
		return new BCGroup(node(),content);
	}
	
	@Override
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */
	public boolean strictlyEquals(final ByteCodeDataType find) {
		if (!(find instanceof BCGroup)) {
			return false;
		}
		return ((BCGroup)find).content.getId()==content.getId();
	}
}
