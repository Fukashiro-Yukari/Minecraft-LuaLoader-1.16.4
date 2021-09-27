package com.itranswarp.lualoader.compiler;

import com.nepqneko.lualoader.lualoader;
import org.luaj.lualoader.vm2.LuaError;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;

/**
 * In-memory compile Java source code as String.
 * 
 * @author michael
 */
public class JavaStringCompiler {

	JavaCompiler compiler;
	StandardJavaFileManager stdManager;
	DiagnosticCollector<JavaFileObject> diagnostics;
	boolean islua;

	public JavaStringCompiler(boolean islua) {
		this.compiler = ToolProvider.getSystemJavaCompiler();
		this.diagnostics = new DiagnosticCollector<JavaFileObject>();
		this.stdManager = compiler.getStandardFileManager(this.diagnostics, null, null);
		this.islua = islua;
	}

	/**
	 * Compile a Java source file in memory.
	 * 
	 * @param fileName
	 *            Java file name, e.g. "Test.java"
	 * @param source
	 *            The source code as String.
	 * @return The compiled results as Map that contains class name as key,
	 *         class binary as value.
	 * @throws IOException
	 *             If compile error.
	 */
	public Map<String, byte[]> compile(String fileName, String source) throws IOException {
		try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager)){
			JavaFileObject javaFileObject = manager.makeStringSource(fileName, source);
			CompilationTask task = compiler.getTask(null, manager, diagnostics, null, null, Collections.singletonList(javaFileObject));
			Boolean result = task.call();
			StringBuilder diagnosticBuilder = new StringBuilder();

			for (Diagnostic<? extends JavaFileObject> diagnostic:diagnostics.getDiagnostics()){
				buildDiagnosticMessage(diagnostic,diagnosticBuilder);
			}

			if (islua) lualoader.LuaError(diagnosticBuilder.toString());
			else lualoader.logger.error(diagnosticBuilder.toString());
			if (result == null || !result){
				if (islua) throw new LuaError("Compilation failed.");
				else throw new RuntimeException("Compilation failed.");
			}

			return manager.getClassBytes();
		}
	}

	protected boolean buildDiagnosticMessage(Diagnostic diagnostic, StringBuilder diagnosticBuilder){
		Object source = diagnostic.getSource();
		String sourceErrorDetails = "";

		if (source != null) {
			JavaFileObject sourceFile = JavaFileObject.class.cast(source);
			CharSequence sourceCode = null;

			try {
				sourceCode = sourceFile.getCharContent(true);
			} catch (IOException e){
				if (islua) lualoader.LuaError(e);
				else lualoader.logger.error(e);
			}

			int startPosition = Math.max((int)diagnostic.getStartPosition()-10,0);
			int endPosition = Math.min(sourceCode.length(),(int)diagnostic.getEndPosition()+10);
			sourceErrorDetails = sourceCode.subSequence(startPosition, endPosition)+"";
		}

		if (diagnostic.getLineNumber() != -1){
			diagnosticBuilder.append(String.format("Error on line %d:",diagnostic.getLineNumber()));
			diagnosticBuilder.append("\n");
			diagnosticBuilder.append(diagnostic.getMessage(null));
			diagnosticBuilder.append("\n");
			diagnosticBuilder.append(sourceErrorDetails);
			diagnosticBuilder.append("\n");
		}

		return diagnostic.getKind().equals(Diagnostic.Kind.ERROR);
	}

	/**
	 * Load class from compiled classes.
	 * 
	 * @param name
	 *            Full class name.
	 * @param classBytes
	 *            Compiled results as a Map.
	 * @return The Class instance.
	 * @throws ClassNotFoundException
	 *             If class not found.
	 * @throws IOException
	 *             If load error.
	 */
	public Class<?> loadClass(String name, Map<String, byte[]> classBytes) throws ClassNotFoundException, IOException {
		try (MemoryClassLoader classLoader = new MemoryClassLoader(classBytes)) {
			return classLoader.loadClass(name);
		}
	}
}
