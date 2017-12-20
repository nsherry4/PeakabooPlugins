package peakaboo.filter.plugins.programming;


import autodialog.model.Parameter;
import autodialog.view.swing.editors.SwingEditorFactory;
import bolt.compiler.BoltJavaMap;
import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import peakaboo.filter.editors.CodeEditor;
import peakaboo.filter.editors.JavaCodeStyle;
import peakaboo.filter.model.AbstractSimpleFilter;
import peakaboo.filter.model.FilterType;
import scitypes.ISpectrum;
import scitypes.ReadOnlySpectrum;


public class Java extends AbstractSimpleFilter {

	static {
		SwingEditorFactory.registerStyleProvider("java-code-editor", () -> new CodeEditor("java", new JavaSyntaxKit()));
	}
	
	BoltJavaMap<float[], float[]> boltJavaMap;
	
	private Parameter<String> code;
	private CodeEditor editor;
	
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
		return "1.0";
	}
	
	@Override
	public void initialize() {
		code = new Parameter<>("Java Code", new JavaCodeStyle(), defaultCode, this::validate);
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
			editor.errorMessage = e.getMessage();
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
