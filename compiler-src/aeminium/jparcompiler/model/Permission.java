package aeminium.jparcompiler.model;

import javax.management.RuntimeErrorException;

import spoon.reflect.declaration.CtElement;

public class Permission {
	
	public PermissionType type;
	public boolean instance;
	public boolean ret;
	public CtElement target;
	
	public Permission(PermissionType t, CtElement te) {
		if (te == null) throw new RuntimeErrorException(null, "assas");
		type = t;
		target = te;
		instance = false;
		ret = false;
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
		return p.type == this.type && p.instance == this.instance && p.ret == this.ret && p.target == this.target;
	}
	
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("P(");
		b.append((target == null) ? "null" : target.getSignature());
		b.append(",");
		b.append(type);
		
		if (instance) {
			b.append(",I");
		}
		if (ret) {
			b.append(",R");
		}
		b.append(")");
		return b.toString();
	}
}
