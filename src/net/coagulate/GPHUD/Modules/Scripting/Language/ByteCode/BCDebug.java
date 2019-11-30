package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCDebug extends ByteCode {
	private int line=0;
	private int column=0;
	public BCDebug(ParseNode n, int line, int column) { super(n); this.line=line; this.column=column; }

	@Nonnull
	@Override
	public String explain() {
		return "Debug (Line "+line+" column "+column+")";
	}

	@Override
	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.Debug.get());
		addShort(bytes,line);
		addShort(bytes,column);
	}
	@Nonnull
	@Override public String htmlDecode() { return "Debug</td><td>"+line+":"+column; }

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		vm.column=column; vm.row=line;
	}
}
