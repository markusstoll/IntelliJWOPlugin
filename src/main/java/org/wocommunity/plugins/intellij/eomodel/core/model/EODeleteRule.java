package org.wocommunity.plugins.intellij.eomodel.core.model;

public enum EODeleteRule {
	NULLIFY("EODeleteRuleNullify", "Nullify"),
	CASCADE("EODeleteRuleCascade", "Cascade"),
	DENY("EODeleteRuleDeny", "Deny"),
	NO_ACTION("EODeleteRuleNoAction", "No Action");

	private final String plistName;
	private final String label;

	EODeleteRule(String plistName, String label) {
		this.plistName = plistName;
		this.label = label;
	}

	public String getPlistName() {
		return plistName;
	}

	public String getLabel() {
		return label;
	}

	public static EODeleteRule fromPlistName(String plistName) {
		if (plistName == null) {
			return NULLIFY;
		}
		for (EODeleteRule rule : values()) {
			if (rule.plistName.equalsIgnoreCase(plistName) || rule.name().equalsIgnoreCase(plistName)) {
				return rule;
			}
		}
		return NULLIFY;
	}

	@Override
	public String toString() {
		return label;
	}
}

