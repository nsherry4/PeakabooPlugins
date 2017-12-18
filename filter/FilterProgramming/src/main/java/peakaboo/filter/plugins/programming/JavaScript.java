package peakaboo.filter.plugins.programming;


import autodialog.model.Parameter;
import autodialog.view.swing.editors.SwingEditorFactory;
import bolt.scripting.BoltScriptExecutionException;
import bolt.scripting.functions.BoltMap;
import bolt.scripting.languages.JavascriptLanguage;
import bolt.scripting.languages.Language;
import de.sciss.syntaxpane.syntaxkits.JavaScriptSyntaxKit;
import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import peakaboo.filter.editors.CodeEditor;
import peakaboo.filter.editors.JavaScriptCodeStyle;
import peakaboo.filter.model.AbstractSimpleFilter;
import scitypes.ISpectrum;
import scitypes.ReadOnlySpectrum;
import scitypes.Spectrum;


public class JavaScript extends AbstractSimpleFilter {

	static {
		SwingEditorFactory.registerStyleProvider("javascript-code-editor", () -> new CodeEditor("JavaScript", new JavaScriptSyntaxKit()));
	}
	
	
	Parameter<String> code;
	private CodeEditor editor;	
	private boolean jsSupported = true;
	
	private static final String header = "" + 
	"// The input spectrum is located in a variable named spectrumIn\n" +
	"// Place the result of the filter in a variable named spectrumOut\n" +
	"// spectrumIn and spectrumOut are of Java type float[]. \n" +
	"\n\n";
	
	
	private BoltMap<float[], float[]> boltmap;
	
	public JavaScript() {
		super();
		
	
		try 
		{
			Language language = new JavascriptLanguage();
			language.setClassLoader(this.getClass().getClassLoader());
			boltmap = new BoltMap<float[], float[]>(language, "spectrumIn", "spectrumOut", "");
			boltmap.setMultithreaded(true);	
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.out);
			jsSupported = false;
		}
		
		
	}
	
	@Override
	public String pluginVersion() {
		return "1.0";
	}
	
	@Override
	public void initialize() 
	{
		code = new Parameter<>("JavaScript Code", new JavaScriptCodeStyle(), header + "spectrumOut = spectrumIn;");
		addParameter(code);
	}

	@Override
	public String getFilterName() {
		return "JavaScript Code";
	}

	@Override
	public String getFilterDescription() {
		return "Allows you to create your own filter in the JavaScript programming language.";
	}

	@Override
	public FilterType getFilterType() {
		return FilterType.PROGRAMMING;
	}

	@Override
	public boolean validateParameters() {
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
		return jsSupported;
	}

	@Override
	public boolean canFilterSubset() {
		return true;
	}

}
