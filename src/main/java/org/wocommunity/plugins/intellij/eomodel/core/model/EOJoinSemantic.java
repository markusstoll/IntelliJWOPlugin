package org.wocommunity.plugins.intellij.eomodel.core.model;

public enum EOJoinSemantic {
	INNER("EOInnerJoin", "Inner Join"),
	FULL_OUTER("EOFullOuterJoin", "Full Outer Join"),
	LEFT_OUTER("EOLeftOuterJoin", "Left Outer Join"),
	RIGHT_OUTER("EORightOuterJoin", "Right Outer Join");

	private final String plistName;
	private final String label;

	EOJoinSemantic(String plistName, String label) {
		this.plistName = plistName;
		this.label = label;
	}

	public String getPlistName() {
		return plistName;
	}

	public String getLabel() {
		return label;
	}

	public static EOJoinSemantic fromPlistName(String plistName) {
		if (plistName == null) {
			return INNER;
		}
		for (EOJoinSemantic semantic : values()) {
			if (semantic.plistName.equalsIgnoreCase(plistName) || semantic.name().equalsIgnoreCase(plistName)) {
				return semantic;
			}
		}
		return INNER;
	}

	@Override
	public String toString() {
		return label;
	}
}

