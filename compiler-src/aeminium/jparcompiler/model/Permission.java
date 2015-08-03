package aeminium.jparcompiler.model;

import javax.management.RuntimeErrorException;

import spoon.reflect.declaration.CtElement;

public class Permission {
	
	public PermissionType type;
	public boolean instance;
	public boolean control;
	public CtElement target;
	
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
		return p.type == this.type && p.instance == this.instance && p.control == this.control && p.target == this.target;
	}
	
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("P(");
		b.append((target == null) ? "null" : target.getSignature());
		b.append("," + target.getClass().getSimpleName());
		b.append(",");
		b.append(type);
		
		if (instance) {
			b.append(",I");
		}
		if (control) {
			b.append(",R");
		}
		b.append(")");
		return b.toString();
	}
}
