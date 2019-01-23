package org.peakaboo.filter.plugins.programming;


import org.peakaboo.filter.editors.CodeEditor;
import org.peakaboo.filter.editors.CodeStyle;
import org.peakaboo.filter.editors.JavaCodeStyle;
import org.peakaboo.filter.model.AbstractSimpleFilter;
import org.peakaboo.filter.model.FilterType;

import cyclops.ISpectrum;
import cyclops.ReadOnlySpectrum;
import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import net.sciencestudio.autodialog.model.Parameter;
import net.sciencestudio.autodialog.view.swing.editors.SwingEditorFactory;
import net.sciencestudio.bolt.compiler.BoltJavaMap;


public class Java extends AbstractSimpleFilter {

	static {
		SwingEditorFactory.registerStyleProvider("java-code-editor", () -> new CodeEditor("java", new JavaSyntaxKit()));
	}
	
	BoltJavaMap<float[], float[]> boltJavaMap;
	
	private CodeStyle style;
	private Parameter<String> code;
	
	private static final String defaultBoltFunction = "" + 
		"return JavaFilter.filter(spectrumIn);";
	
	private static final String defaultCode = "" + 
	"class JavaFilter { \n" +
	"	public static float[] filter(float[] spectrum){ \n" +
	"		return spectrum; \n" +
	"	} \n" +
	"} \n";

	
	public Java() {
		boltJavaMap = new BoltJavaMap<float[], float[]>("spectrumIn", float[].class, float[].class);
	}
	
	@Override
	public String pluginVersion() {
		return "1.2";
	}
	
	@Override
	public String pluginUUID() {
		return "3f8761ae-5702-46d9-8a59-446e4a70f9fa";
	}
	
	@Override
	public void initialize() {
		style = new JavaCodeStyle();
		code = new Parameter<>("Java Code", style, defaultCode, this::validate);
		addParameter(code);
	}
	
	@Override
	public FilterType getFilterType() {
		return FilterType.PROGRAMMING;
	}


	public boolean validate(Parameter<String> p) {
		
		try {
						
			boltJavaMap.setFunctionText(defaultBoltFunction);
			boltJavaMap.setOtherText("");
			boltJavaMap.setIncludeText(code.getValue());
			
			boltJavaMap.apply(new float[]{1, 2, 3, 4});
			return true;
		} catch (Exception e) {
			style.errorMessage = e.getMessage();
			e.printStackTrace();
			return false;
		}
		
	}

	@Override
	protected ReadOnlySpectrum filterApplyTo(ReadOnlySpectrum data) {
		
		//in this plugin, validate also puts the user code into the mapper 
		validate(code);
		
		return new ISpectrum(boltJavaMap.apply(data.backingArrayCopy()));
	}

	@Override
	public boolean canFilterSubset() {
		return true;

	}

	@Override
	public String getFilterName() {
		return "Java Code";

	}

	@Override
	public String getFilterDescription() {
		return "Allows you to create your own filter in the Java programming language";
	}

	@Override
	public boolean pluginEnabled() {
		return true;

	}
	
}
