package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;

import java.util.ArrayList;
import java.util.List;

public class BCList extends ByteCodeDataType {

	int elements=0; // used by the compiler

	List<ByteCodeDataType> content=new ArrayList<>(); // used by the VM
	public BCList() {}
	public BCList(int elements) { this.elements=elements; }
	public BCList(ByteCodeDataType e) { content.add(e); elements++; }

	@Override
	public String explain() {
		String r="List ("+elements+")";
		return r;
	}

	@Override
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.List.get());
		addShort(bytes,elements);
	}
	@Override public String htmlDecode() { return "List</td><td>"+elements; }

	@Override
	public void execute(GSVM vm) {
		// pull the list from the stack!
		for (int i=0;i<elements;i++) {
			content.add(vm.pop());
		}
		elements=content.size();
		vm.push(this);
	}

	public List<ByteCodeDataType> getContent() {
		return content;
	}

	@Override
	public ByteCodeDataType clone() {
		BCList clone=new BCList();
		clone.elements=elements;
		for(ByteCodeDataType element:content) {
			clone.content.add(element.clone());
		}
		return clone;
	}
}