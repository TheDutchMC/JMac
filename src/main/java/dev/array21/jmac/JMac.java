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

import dev.array21.jmac.parser.FileMacroDeclarationParser;
import dev.array21.jmac.parser.components.MacroContext;

public class JMac implements Plugin<Project> {
	
	String sourceSetName = "main";
	private Project project;
	
	@TaskAction
	public void preprocess() {
		System.out.println("Preprocessing...");
		
		List<MacroContext> macros = new ArrayList<>();
		
		SourceSet mainSourceSet = this.project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(this.sourceSetName);
		mainSourceSet.getAllJava().forEach(javaFile -> {
			try {
				MacroContext[] macrosInFile = FileMacroDeclarationParser.findMacroDeclarations(javaFile);
				macros.addAll(Arrays.asList(macrosInFile));
			} catch(IOException e) {
				e.printStackTrace();
			}
		});

	}

	@Override
	public void apply(Project p) {
		this.project = p;
		p.task("preprocess").doFirst(t -> preprocess());
	}
}
