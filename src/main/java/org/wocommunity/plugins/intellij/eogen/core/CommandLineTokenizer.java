package org.wocommunity.plugins.intellij.eogen.core;

import java.text.CharacterIterator;
import java.text.ParseException;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

public class CommandLineTokenizer implements Enumeration<String> {
	private final StringCharacterIterator iterator;

	public static List<String> tokenize(String line) {
		List<String> result = new ArrayList<>();
		if (line == null) {
			return result;
		}
		CommandLineTokenizer tokenizer = new CommandLineTokenizer(line);
		while (tokenizer.hasMoreTokens()) {
			try {
				String token = tokenizer.nextToken();
				if (token != null) {
					result.add(token);
				}
			} catch (NoSuchElementException | ParseException e) {
				break;
			}
		}
		return result;
	}

	public CommandLineTokenizer(String line) {
		this.iterator = new StringCharacterIterator(line != null ? line : "");
		reset();
	}

	public void reset() {
		this.iterator.first();
	}

	@Override
	public boolean hasMoreElements() {
		return hasMoreTokens();
	}

	public boolean hasMoreTokens() {
		char c = iterator.current();
		while (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
			c = iterator.next();
		}
		return c != CharacterIterator.DONE;
	}

	@Override
	public String nextElement() {
		try {
			return nextToken();
		} catch (ParseException | NoSuchElementException e) {
			return null;
		}
	}

	public String nextToken() throws ParseException {
		// 1. Skip leading whitespace
		char c = iterator.current();
		while (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
			c = iterator.next();
		}

		if (c == CharacterIterator.DONE) {
			throw new NoSuchElementException("There are no more tokens.");
		}

		StringBuilder token = new StringBuilder();
		char quoteChar = 0;
		boolean escapeNext = false;
		boolean hadQuotes = false;

		while (c != CharacterIterator.DONE) {
			if (escapeNext) {
				if (c == '\r') {
					c = iterator.next();
					if (c == '\n') {
						c = iterator.next();
					}
				} else if (c == '\n') {
					c = iterator.next();
				} else {
					token.append(c);
					c = iterator.next();
				}
				escapeNext = false;
			} else if (quoteChar == '\'') {
				if (c == '\'') {
					quoteChar = 0;
					hadQuotes = true;
					c = iterator.next();
				} else {
					token.append(c);
					c = iterator.next();
				}
			} else if (quoteChar == '"') {
				if (c == '\\') {
					escapeNext = true;
					c = iterator.next();
				} else if (c == '"') {
					quoteChar = 0;
					hadQuotes = true;
					c = iterator.next();
				} else {
					token.append(c);
					c = iterator.next();
				}
			} else {
				// Not inside quotes
				if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
					break; // Token finished
				} else if (c == '\\') {
					escapeNext = true;
					c = iterator.next();
					// Check for line continuation: \ followed by newline
					if (c == '\r' || c == '\n') {
						if (c == '\r') {
							c = iterator.next();
							if (c == '\n') {
								c = iterator.next();
							}
						} else {
							c = iterator.next();
						}
						escapeNext = false;
						// If token was empty, skip following whitespace
						if (token.length() == 0 && !hadQuotes) {
							while (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
								c = iterator.next();
							}
						}
					}
				} else if (c == '"' || c == '\'') {
					quoteChar = c;
					hadQuotes = true;
					c = iterator.next();
				} else {
					token.append(c);
					c = iterator.next();
				}
			}
		}

		if (token.length() == 0 && !hadQuotes) {
			throw new NoSuchElementException("There are no more tokens.");
		}

		return token.toString();
	}
}
