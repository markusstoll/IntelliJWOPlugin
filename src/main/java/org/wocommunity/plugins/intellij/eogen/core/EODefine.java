package org.wocommunity.plugins.intellij.eogen.core;

import java.util.Objects;

public class EODefine {
	private String name;
	private String value;

	public EODefine() {
	}

	public EODefine(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EODefine eoDefine = (EODefine) o;
		return Objects.equals(name, eoDefine.name) && Objects.equals(value, eoDefine.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}

	@Override
	public String toString() {
		return name + "=" + value;
	}
}

