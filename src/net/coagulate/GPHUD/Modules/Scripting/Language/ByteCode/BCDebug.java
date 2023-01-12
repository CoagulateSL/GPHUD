package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCDebug extends ByteCode {
	private final int line;
	private final int column;
	
	public BCDebug(final ParseNode n,final int line,final int column) {
		super(n);
		this.line=line;
		this.column=column;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explain() {
		return "Debug (Line "+line+" column "+column+")";
	}
	
	@Override
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Debug.get());
		addShort(bytes,line);
		addShort(bytes,column);
	}
	
	@Nonnull
	@Override
	public String htmlDecode() {
		return "Debug</td><td>"+line+":"+column;
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSVM vm,final boolean simulation) {
		vm.column=column;
		vm.row=line;
	}
}
