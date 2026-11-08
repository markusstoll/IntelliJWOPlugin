package org.wocommunity.plugins.intellij.eomodel.core.model;

/**
 * Utility methods matching WOLips StringUtils behavior.
 */
public final class StringUtils {

	private StringUtils() {
	}

	/**
	 * Converts camelCase to uppercase with underscores, matching WOLips convention:
	 * {@code StringUtils.camelCaseToUnderscore(name).toUpperCase()}.
	 * Handles acronym boundaries like {@code fkLandingPageMPElementId} -> {@code FK_LANDING_PAGE_MP_ELEMENT_ID}.
	 */
	public static String camelCaseToUnderscore(String camelString) {
		if (camelString == null) {
			return "";
		}
		StringBuilder underscore = new StringBuilder();
		boolean lastCharacterWasWordBreak = false;
		boolean lastCharacterWasCapital = false;
		int length = camelString.length();
		for (int i = 0; i < length; i++) {
			char ch = camelString.charAt(i);
			if (Character.isUpperCase(ch)) {
				boolean isLastCharacter = (i == length - 1);
				boolean nextCharacterIsCapital = (!isLastCharacter && Character.isUpperCase(camelString.charAt(i + 1)));
				if (i > 0 && ((!lastCharacterWasWordBreak && !lastCharacterWasCapital) || (!nextCharacterIsCapital && !isLastCharacter))) {
					underscore.append("_");
					lastCharacterWasWordBreak = true;
				} else {
					lastCharacterWasWordBreak = false;
				}
				lastCharacterWasCapital = true;
			} else if (ch == '_') {
				lastCharacterWasWordBreak = true;
				lastCharacterWasCapital = false;
			} else {
				lastCharacterWasWordBreak = false;
				lastCharacterWasCapital = false;
			}
			underscore.append(Character.toUpperCase(ch));
		}
		return underscore.toString();
	}
}

