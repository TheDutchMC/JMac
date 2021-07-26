package dev.array21.jmac.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.array21.jmac.exception.MacroSyntaxException;
import dev.array21.jmac.parser.components.MacroContext;

public class DeclarationParser {
	
	private static final Pattern MACRO_NAME_REGEX = Pattern.compile("[a-zA-Z._-]*$");
	
	public static MacroContext[] findMacroDeclarations(File input) throws IOException {
		List<MacroContext> macros = new ArrayList<>();
		BufferedReader buffReader = new BufferedReader(new FileReader(input));
		
		List<String> macroBody = new LinkedList<>();
		AtomicBoolean inMacroDefinition = new AtomicBoolean(false);
		AtomicReference<String> atomicPackageName = new AtomicReference<>();
		AtomicReference<String> atomicMacroName = new AtomicReference<>();
		AtomicInteger bracketCount = new AtomicInteger(0);
		
		AtomicInteger lineInFile = new AtomicInteger(0);
		buffReader.lines().forEach(line -> {
			lineInFile.incrementAndGet();
			
			// If the line starts with 'package', that must be the package declaration
			if(line.startsWith("package")) {
				String packageName = line.replace("package", "").replace(";", "").trim();
				atomicPackageName.set(packageName);
				return;
			}
			
			// If the line starts with macro_rules!, that is the beginning of a macro declaration
			if(line.startsWith("macro_rules!")) {
				
				// The package name must be set before the first macro declaration, if not it is a syntax error
				if(atomicPackageName.get() == null) {
					throw new MacroSyntaxException(String.format("Expected package declaration, but found macro declaration (line %d in %s.%s)", 
							lineInFile.get(), 
							atomicPackageName.get(), 
							input.getName().replace(".java", "")));
				}
				
				// Get the macro name, this might contain a '{'
				String macroName = line.replace("macro_rules!", "").trim();
				
				// If the name contains a '{', we're entering the macro declaration
				// Patch up the name by removing the '{'
				if(macroName.contains("{")) {
					macroName = macroName.replace("{", "").trim();
					bracketCount.incrementAndGet();
					inMacroDefinition.set(true);
				}
				
				// Empty names aren't permitted
				if(macroName.isEmpty()) {
					throw new MacroSyntaxException(String.format("Macro name must not be empty (line %d in %s.%s)", 
							lineInFile.get(), 
							atomicPackageName.get(), 
							input.getName().replace(".java", "")));
				}
				
				// Names must adhere to the above regex
				Matcher validNameMatcher = MACRO_NAME_REGEX.matcher(macroName);
				if(!validNameMatcher.matches()) {
					throw new MacroSyntaxException(String.format("Macro name must only contain [a-z], [A-Z], a period '.', an underscore '_' or a hyphen '-' (line %d in %s.%s)", 
							lineInFile.get(), 
							atomicPackageName.get(), 
							input.getName().replace(".java", "")));
				}
				
				atomicMacroName.set(macroName);
				return;
			}
			
			// If we're not in the macro body yet AND the line starts with a '{' AND the name is already known
			// then we're entering the macro declaration
			if(line.trim().startsWith("{") && !inMacroDefinition.get() && atomicMacroName.get() != null) {
				bracketCount.incrementAndGet();
				inMacroDefinition.set(true);
				return;
			}
			
			if(line.contains("{") && inMacroDefinition.get()) {
				bracketCount.incrementAndGet();
			}
			
			if(line.contains("}") && inMacroDefinition.get()) {
				int localBracketCount = bracketCount.decrementAndGet();
				
				// When the brackCount reaches '0' we've reached the end of the macro declaration
				if(localBracketCount == 0) {
					
					String[] body = macroBody.toArray(new String[0]);
					MacroContext m = new MacroContext(atomicMacroName.get(), atomicPackageName.get(), String.join("\n", body));
					macros.add(m);
					
					// Reset all Atomics
					inMacroDefinition.set(false);
					macroBody.clear();
					atomicMacroName.set(null);
					lineInFile.set(0);
				}
			}
			
			if(inMacroDefinition.get()) {
				macroBody.add(line);
			}
			
			return;
		});
		
		buffReader.close();
		
		// We've reached EOF, but the bracketCount is not 0
		// The user must have forgotten a '}', or multiple
		if(bracketCount.get() != 0) {
			throw new MacroSyntaxException(String.format("Expected '}' but found EOF (line %d in %s.%s)", 
					lineInFile.get(), 
					atomicPackageName.get(), 
					input.getName().replace(".java", "")));
		}
		
		
		return macros.toArray(new MacroContext[0]);
	}
}
