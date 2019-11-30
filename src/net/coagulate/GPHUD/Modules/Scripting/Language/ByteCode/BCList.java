package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class BCList extends ByteCodeDataType {

	int elements=0; // used by the compiler

	final List<ByteCodeDataType> content=new ArrayList<>(); // used by the VM
	public BCList(ParseNode n) {super(n);}
	public BCList(ParseNode n,int elements) { super(n); this.elements=elements; }
	public BCList(ParseNode n,ByteCodeDataType e) { super(n); content.add(e); elements++; }

	@Nonnull
	@Override
	public String explain() {
		return "List (#"+elements+")";
	}

	@Override
	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.List.get());
		addShort(bytes,elements);
	}
	@Nonnull
	@Override public String htmlDecode() { return "List</td><td>"+elements; }

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		// pull the list from the stack!
		for (int i=0;i<elements;i++) {
			ByteCodeDataType data = vm.pop();
			if (data instanceof BCList) { throw new GSInvalidExpressionException("You can not nest a List inside a List"); }
			content.add(data);
		}
		elements=content.size();
		vm.push(this);
	}

	@Nonnull
	public List<ByteCodeDataType> getContent() {
		return content;
	}

	@Nonnull
	@Override
	public ByteCodeDataType clone() {
		BCList clone=new BCList(node());
		clone.elements=elements;
		for(ByteCodeDataType element:content) {
			clone.content.add(element.clone());
		}
		return clone;
	}
}
