package aeminium.jparcompiler.model;

import javax.management.RuntimeErrorException;

import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.CtElement;

public class Permission {
	
	public PermissionType type;
	public boolean instance;
	public boolean control;
	public CtElement target;
	public CtLocalVariable<?> index;
	
	public Permission(PermissionType t, CtElement te) {
		if (te == null) throw new RuntimeErrorException(null, "Target element is null");
		type = t;
		target = te;
		instance = false;
		control = false;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Permission)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		Permission p = (Permission) obj;
		return p.type == this.type && p.instance == this.instance && p.control == this.control && p.target == this.target && p.index == this.index;
	}
	
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("P(");
		b.append((target == null) ? "null" : target.toString());
		b.append("," + target.getClass().getSimpleName());
		b.append(",");
		b.append(type);
		
		if (instance) {
			b.append(",I");
		}
		if (control) {
			b.append(",R");
		}
		if (index != null) {
			b.append(",IND_" + index.getSimpleName());
		}
		b.append(")");
		return b.toString();
	}

	public Permission copy() {
		Permission p = new Permission(type, target);
		p.instance = this.instance;
		p.control = this.control;
		p.index = this.index;
		return p;
	}
}
