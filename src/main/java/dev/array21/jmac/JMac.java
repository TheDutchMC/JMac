package dev.array21.jmac;

import java.io.BufferedReader;
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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

public class JMac implements Plugin<Project> {
	
	String sourceSetName = "main";
	private Project project;
	
	@TaskAction
	public void preprocess() {
		System.out.println("Preprocessing...");
		
		List<Macro> macros = new ArrayList<>();
		
		SourceSet mainSourceSet = this.project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(this.sourceSetName);
		mainSourceSet.getAllJava().forEach(javaFile -> {
			BufferedReader buffReader;
			try {
				buffReader = new BufferedReader(new FileReader(javaFile));
			} catch(IOException e) {
				e.printStackTrace();
				return;
			}
			
			Pattern validNamePattern = Pattern.compile("[a-zA-Z._-]*$");
			
			List<String> macroBody = new LinkedList<>();
			AtomicBoolean inMacroDefinition = new AtomicBoolean(false);
			AtomicReference<String> atomicPackageName = new AtomicReference<>();
			AtomicReference<String> atomicMacroName = new AtomicReference<>();
			AtomicInteger bracketCount = new AtomicInteger(0);
			
			buffReader.lines().forEach(line -> {
				// START OF MACRO DECLARATION PARSING
				if(line.startsWith("package")) {
					String packageName = line.replace("package", "").replace(";", "").trim();
					atomicPackageName.set(packageName);
					return;
				}
								
				if(line.startsWith("macro_rules!")) {
					String macroName = line.replace("macro_rules!", "").trim();
					if(macroName.contains("{")) {
						macroName = macroName.replace("{", "").trim();
						bracketCount.incrementAndGet();
						inMacroDefinition.set(true);
					}
					
					if(macroName.isEmpty()) {
						throw new IllegalArgumentException("Macro name must not be empty");
					}
										
					Matcher validNameMatcher = validNamePattern.matcher(macroName);
					if(!validNameMatcher.matches()) {
						throw new IllegalArgumentException("Macro name must only contain [a-z], [A-Z], a period '.', an underscore '_' or a hyphen '-'");
					}
					
					atomicMacroName.set(macroName);
					return;
				}
				
				if(line.trim().startsWith("{") && !inMacroDefinition.get()) {
					bracketCount.incrementAndGet();
					inMacroDefinition.set(true);
					return;
				}
				
				if(line.contains("{")) {
					bracketCount.incrementAndGet();
				}
				
				if(line.contains("}")) {
					int localBracketCount = bracketCount.decrementAndGet();
					if(localBracketCount == 0) {
						//TODO parse macro itself
						
						String[] body = macroBody.toArray(new String[0]);
						Macro m = new Macro(atomicMacroName.get(), atomicPackageName.get(), String.join("\n", body));
						macros.add(m);
						
						inMacroDefinition.set(false);
						macroBody.clear();
					}
				}
				
				if(inMacroDefinition.get()) {
					macroBody.add(line);
				}
				
				// END OF MACRO DECLARATION PARSING
				//
				// START OF MACRO USAGE PARSING
			});
			
			try {
				buffReader.close();
			} catch(IOException e) {
				e.printStackTrace();
				return;
			}
		});
		
		macros.forEach(macro -> {
			System.out.println(String.format("Found macro '%s' in package '%s' with body:", macro.name(), macro.packageName()));
			System.out.println(macro.body());
			System.out.println("---------------------------");
		});
	}

	@Override
	public void apply(Project p) {
		this.project = p;
		p.task("preprocess").doFirst(t -> preprocess());
	}
}
