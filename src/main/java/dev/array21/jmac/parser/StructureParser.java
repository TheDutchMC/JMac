package dev.array21.jmac.parser;

import java.nio.CharBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dev.array21.jmac.exception.MacroSyntaxException;
import dev.array21.jmac.parser.components.MacroContext;
import dev.array21.jmac.parser.components.MacroStructure;
import dev.array21.jmac.util.Buffers;

/**
 * Parser for a macro body to seperate out the matcher and the transcriber
 * @author Tobias de Bruijn
 */
public class StructureParser {

	public static MacroStructure[] parseMacroBody(MacroContext macro) {
		
		List<MacroStructure> structBuffer = new LinkedList<>();
		
		AtomicBoolean inMacroTranscriber = new AtomicBoolean(false);
		AtomicBoolean inMacroMatcher = new AtomicBoolean(false);
		AtomicBoolean matcherArrowSeen = new AtomicBoolean(false);
		AtomicBoolean commaSeen = new AtomicBoolean(false);
		
		AtomicInteger prevChar = new AtomicInteger();
		AtomicInteger matcherBraceCounter = new AtomicInteger(0);
		AtomicInteger transcriberBracketCounter = new AtomicInteger(0);
		
		CharBuffer charBuffer = CharBuffer.allocate(macro.body().length());
				
		AtomicInteger lineCounter = new AtomicInteger(0);
		AtomicInteger colCounter = new AtomicInteger(0);
		
		AtomicReference<String> matcher = new AtomicReference<>();
		AtomicReference<String> transcriber = new AtomicReference<>();
		
		macro.body().lines().forEach(line -> {			
			lineCounter.incrementAndGet();
			
			// We dont care for comments
			if(line.trim().startsWith("//")) {
				return;
			}
			
			line.chars().forEach(c -> {
				colCounter.incrementAndGet();
								
				// We dont care about spaces
				if(c == ' ') {
					if(!inMacroTranscriber.get()) {
						return;
					}
				}
				
				if(c == ',') {
					commaSeen.set(true);
				}
				
				// Potentially the beginning of a matcher
				if(c == '(' && !inMacroTranscriber.get()) {
					
					matcherBraceCounter.incrementAndGet();

					// If we're not in a matcher already, this is the beginning of a matcher
					if(!inMacroMatcher.get()) {
						if(structBuffer.size() > 0 && !commaSeen.get()) {
							throw new MacroSyntaxException(String.format("Expected ',', but found beginning of macro matcher (line %d column %d in %s.%s)", lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
						}
						
						commaSeen.set(false);
						Buffers.zero(charBuffer);
						inMacroMatcher.set(true);
					} else {
						charBuffer.append((char) c);
					}
					
					return;
				}
				
				// Potential end of a matcher
				if(c == ')' && !inMacroTranscriber.get()) {
					int braceCount = matcherBraceCounter.decrementAndGet();
					
					// If brace count is 0, we are likely to be at the end of the matcher
					if(braceCount == 0) {
						if(!inMacroMatcher.get()) {
							throw new MacroSyntaxException(String.format("Expected '(', but found ')' (line %d column %d in macro %s.%s)", lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
						}
												
						inMacroMatcher.set(false);
						matcher.set(charBuffer.position(0).toString());
					} else {
						charBuffer.append((char) c);
					}		
					
					// Less than zero is a syntax error
					if(braceCount < 0) {
						throw new MacroSyntaxException(String.format("Expected \"=>\" but found ')' (line %d column %d in macro %s.%s)", lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
					}
					
					return;
				}
				
				// Check for the matcherArrow
				// i.e in `() => {}`
				//            ^^ This one
				if(c == '>' && !inMacroMatcher.get() && !inMacroTranscriber.get()) {
					if(prevChar.get() != '=') {
						throw new MacroSyntaxException(String.format("Expected \"=>\" but found \"%s>\" (line %d column %d in macro %s.%s)", (char) prevChar.get(), lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
					}
					
					matcherArrowSeen.set(true);					
					return;
				}
				
				// Potential start of transcriber
				if(c == '{') {
					// Character isn't allowed in the matcher
					if(inMacroMatcher.get()) {
						throw new MacroSyntaxException(String.format("Unexpected character '{' in macro matcher (line %d column %d in macro %s.%s)", lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
					}
					
					// A matcher must have been declared before this character
 					if(matcher.get() == null) {
 						throw new MacroSyntaxException(String.format("Expected macro matcher, found beginning of macro transcriber (line %d column %d in macro %s.%s)", lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
 					}
 					
 					// A matcherArrow must have been declared before this character
 					if(!matcherArrowSeen.get()) {
 						throw new MacroSyntaxException(String.format("Expected \"=>\" found macro beginning of macro transcriber (line %d column %d in macro %s.%s)", lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
 					}
					
					transcriberBracketCounter.incrementAndGet();
					
					// Start of transcriber
					if(!inMacroTranscriber.get()) {
						Buffers.zero(charBuffer);
						inMacroTranscriber.set(true);
					} else {
						charBuffer.append((char) c);
					}
					
					return;
				}
				
				// Potential end of transcriber
				if(c == '}') {
					// Character is not allowed in matcher
					if(inMacroMatcher.get()) {
						throw new MacroSyntaxException(String.format("Unexpected character '}' in macro matcher (line %d column %d in macro %s.%s)", lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
					}
					
					int bracketCount = transcriberBracketCounter.decrementAndGet();
					
					// If bracket count is 0, we've reached the end of the transcriber
					if(bracketCount == 0) {
						
						// If we're not in a transcriber, then this character is now allowed
						if(!inMacroTranscriber.get()) {
							throw new MacroSyntaxException(String.format("Expected '{', but found '}' (line %d column %d in macro %s.%s)", lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
						}
						
						inMacroTranscriber.set(false);
						transcriber.set(charBuffer.position(0).toString());
						charBuffer.clear();
						
						// Replace n spaces with 1 space
						// It is important to keep at least 1 space if there were multiple
						// E.g consider the following
						// return    1;
						// If we remove all spaces here, we get `return1;`, which is not valid Java
						// But if we leave 1 space, we get `return 1;`, which is totally valid
						String transcriberStr = transcriber.get();
						String spacesFixed = transcriberStr.replaceAll("\\s{2,}", " ").trim();
							
						MacroStructure struct = new MacroStructure(macro, matcher.get(), spacesFixed);
						structBuffer.add(struct);
						
						matcher.set(null);
						transcriber.set(null);
						matcherArrowSeen.set(false);
					} else {
						charBuffer.append((char) c);
					}
					
					// Less than zero is never allowed
					if(bracketCount < 0) {
						throw new MacroSyntaxException(String.format("Unexpected '}' (line %d column %d in macro %s.%s)", lineCounter.get(), colCounter.get(), macro.packageName(), macro.name()));
					}
					
					return;
				}
				
				prevChar.set(c);
				charBuffer.append((char) c);	
			});
			colCounter.set(0);
		});
		
		// We have reached the end of the macro, but the user never closed the matcher
		if(inMacroMatcher.get()) {
			throw new MacroSyntaxException(String.format("Expected end of macro matcher, but reached end of macro (macro %s.%s)", macro.packageName(), macro.name()));
		}
		
		if(matcher.get() != null) {
			if(matcherArrowSeen.get()) {
				throw new MacroSyntaxException(String.format("Expected macro transcriber, but reached end of macro (macro %s.%s)", macro.packageName(), macro.name()));
			} else {
				throw new MacroSyntaxException(String.format("Expected \"=>\", but reached end of macro (macro %s.%s)", macro.packageName(), macro.name()));
			}
		}
		
		// We have reached the end of the macro, but the user never closed the transcribed
		if(inMacroTranscriber.get()) {
			throw new MacroSyntaxException(String.format("Expected end of macro transcriber, but reached end of macro (macro %s.%s)", macro.packageName(), macro.name()));
		}
		
		// We have reached the end of the macro, but the user didn't declare a matcher AND a transcriber
		if(structBuffer.isEmpty()) {
			throw new MacroSyntaxException(String.format("Expected at least one macro matcher and transcriber, but reached end of macro (macro %s.%s)", macro.packageName(), macro.name()));
		}
		
		return structBuffer.toArray(new MacroStructure[0]);
	}
}
