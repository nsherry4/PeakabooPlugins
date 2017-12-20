package peakaboo.filter.plugins.programming;


import autodialog.model.Parameter;
import autodialog.model.style.editors.TextAreaStyle;
import bolt.scripting.BoltScriptExecutionException;
import bolt.scripting.functions.BoltMap;
import peakaboo.filter.editors.CodeEditor;
import peakaboo.filter.model.AbstractSimpleFilter;
import peakaboo.filter.model.FilterType;
import scitypes.ISpectrum;
import scitypes.ReadOnlySpectrum;


public class JPython extends AbstractSimpleFilter {

	Parameter<String> code;
	private CodeEditor editor;	
	private boolean pythonSupported = true;
	
	private static final String header = "" + 
	"from java.util import * \n" +
	"import array\n" +
	"\n" +
	"# The input spectrum is located in a variable named spectrumIn\n" +
	"# Place the result of the filter in a variable named spectrumOut\n" +
	"# spectrumIn and spectrumOut are of Java type float[]. \n" +
	"\n\n";
	
	
	private BoltMap<float[], float[]> boltmap;
	
	public JPython() {
		super();
		
		//jython seems to be broken at the moment. Something about repackaging it in maven, maybe?
		pythonSupported = false;
//		try 
//		{
//			Language language = new PythonLanguage();
//			language.setClassLoader(this.getClass().getClassLoader());
//			boltmap = new BoltMap<float[], float[]>(language, "spectrumIn", "spectrumOut", "");
//			boltmap.setMultithreaded(true);	
//		}
//		catch (Throwable t)
//		{
//			t.printStackTrace(System.out);
//			pythonSupported = false;
//		}
		
		
	}
	
	@Override
	public String pluginVersion() {
		return "1.0";
	}
	
	@Override
	public void initialize() 
	{
		//editor = new CodeEditor("python", new PythonSyntaxKit());
		code = new Parameter<>("JPython Code", new TextAreaStyle(), header + "spectrumOut = spectrumIn", this::validate);
		addParameter(code);
	}

	@Override
	public String getFilterName() {
		return "JPython Code";
	}

	@Override
	public String getFilterDescription() {
		return "Allows you to create your own filter in the Python programming language using JPython";
	}

	@Override
	public FilterType getFilterType() {
		return FilterType.PROGRAMMING;
	}

	public boolean validate(Parameter<String> p) {
		try {
			boltmap.setScript(getCode());
			if (  boltmap.apply(new float[]{1, 2, 3, 4}) instanceof float[]  ){
				return true;
			} else {
				throw new BoltScriptExecutionException("Type mismatch for spectrumOut");
			}
		} catch (Exception e) {
			editor.errorMessage = e.getMessage();
			return false;
		}
		
	}

	@Override
	protected ReadOnlySpectrum filterApplyTo(ReadOnlySpectrum data) {
		
		boltmap.setScript(getCode());
		
		float[] source = data.backingArrayCopy();
		float[] result = boltmap.apply(source);
				
		return new ISpectrum(result);	
		
	}

	private String getCode()
	{
		return code.getValue();
	}
	
	@Override
	public boolean pluginEnabled() {
		return pythonSupported;
	}

	@Override
	public boolean canFilterSubset() {
		return true;
	}

}
