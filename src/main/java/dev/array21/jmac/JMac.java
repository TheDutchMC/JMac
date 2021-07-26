package dev.array21.jmac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import dev.array21.jmac.parser.DeclarationParser;
import dev.array21.jmac.parser.StructureParser;
import dev.array21.jmac.parser.components.MacroContext;
import dev.array21.jmac.parser.components.MacroStructure;

public class JMac implements Plugin<Project> {
	
	String sourceSetName = "main";
	private Project project;
	
	@TaskAction
	public void preprocess() {
		System.out.println("Preprocessing...");
		
		List<MacroContext> macros = new ArrayList<>();
		
		SourceSet mainSourceSet = this.project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(this.sourceSetName);
		
		System.out.println("Declaration parsing...");
		
		mainSourceSet.getAllJava().forEach(javaFile -> {
			try {
				MacroContext[] macrosInFile = DeclarationParser.findMacroDeclarations(javaFile);
				macros.addAll(Arrays.asList(macrosInFile));
			} catch(IOException e) {
				e.printStackTrace();
			}
		});
		
		System.out.println("Structure parsing...");
		
		macros.forEach(macroContext -> {
			MacroStructure[] structs = StructureParser.parseMacroBody(macroContext);
			for(int i = 0; i < structs.length; i++) {
				MacroStructure struct = structs[i];
				
				System.out.println(String.format("(%d) Macro %s.%s has the following matcher:", i+1, macroContext.packageName(), macroContext.name()));
				System.out.println(struct.matcher());
				System.out.println(String.format("(%d) And the following transcriber:", i+1));
				System.out.println(struct.transcriber());
			}
		});
	}

	@Override
	public void apply(Project p) {
		this.project = p;
		p.task("preprocess").doFirst(t -> preprocess());
	}
}
