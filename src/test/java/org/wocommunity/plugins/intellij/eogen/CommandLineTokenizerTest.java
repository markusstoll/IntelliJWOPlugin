package org.wocommunity.plugins.intellij.eogen;

import org.junit.jupiter.api.Test;
import org.wocommunity.plugins.intellij.eogen.core.CommandLineTokenizer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandLineTokenizerTest {

	@Test
	public void testEmptyAndNull() {
		assertTrue(CommandLineTokenizer.tokenize(null).isEmpty());
		assertTrue(CommandLineTokenizer.tokenize("").isEmpty());
		assertTrue(CommandLineTokenizer.tokenize("   \t\n  ").isEmpty());
	}

	@Test
	public void testSimpleTokens() {
		List<String> tokens = CommandLineTokenizer.tokenize("-destination src -subclassDestination src");
		assertEquals(4, tokens.size());
		assertEquals("-destination", tokens.get(0));
		assertEquals("src", tokens.get(1));
		assertEquals("-subclassDestination", tokens.get(2));
		assertEquals("src", tokens.get(3));
	}

	@Test
	public void testQuotedTokens() {
		List<String> tokens = CommandLineTokenizer.tokenize("-model \"My Model/art.eomodeld\" -java");
		assertEquals(3, tokens.size());
		assertEquals("-model", tokens.get(0));
		assertEquals("My Model/art.eomodeld", tokens.get(1));
		assertEquals("-java", tokens.get(2));
	}

	@Test
	public void testEscapedQuotes() {
		List<String> tokens = CommandLineTokenizer.tokenize("-define-foo \"a \\\"quoted\\\" value\"");
		assertEquals(2, tokens.size());
		assertEquals("-define-foo", tokens.get(0));
		assertEquals("a \"quoted\" value", tokens.get(1));
	}

	@Test
	public void testSingleQuotes() {
		List<String> tokens = CommandLineTokenizer.tokenize("-destination 'My Folder/src'");
		assertEquals(2, tokens.size());
		assertEquals("-destination", tokens.get(0));
		assertEquals("My Folder/src", tokens.get(1));
	}
}

