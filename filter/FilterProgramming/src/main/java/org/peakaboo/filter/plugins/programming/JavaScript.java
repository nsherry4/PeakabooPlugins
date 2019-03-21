package org.peakaboo.filter.plugins.programming;


import org.peakaboo.filter.editors.CodeEditor;
import org.peakaboo.filter.editors.CodeStyle;
import org.peakaboo.filter.editors.JavaScriptCodeStyle;
import org.peakaboo.filter.model.AbstractSimpleFilter;
import org.peakaboo.filter.model.FilterType;

import org.peakaboo.framework.cyclops.ISpectrum;
import org.peakaboo.framework.cyclops.ReadOnlySpectrum;
import de.sciss.syntaxpane.syntaxkits.JavaScriptSyntaxKit;
import org.peakaboo.framework.autodialog.model.Parameter;
import org.peakaboo.framework.autodialog.view.swing.editors.SwingEditorFactory;
import org.peakaboo.framework.bolt.scripting.BoltScriptExecutionException;
import org.peakaboo.framework.bolt.scripting.functions.BoltMap;
import org.peakaboo.framework.bolt.scripting.languages.JavascriptLanguage;
import org.peakaboo.framework.bolt.scripting.languages.Language;


public class JavaScript extends AbstractSimpleFilter {

	static {
		SwingEditorFactory.registerStyleProvider("javascript-code-editor", () -> new CodeEditor("JavaScript", new JavaScriptSyntaxKit()));
	}
	
	CodeStyle style;
	Parameter<String> code;
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
		return "1.2";
	}
	
	@Override
	public String pluginUUID() {
		return "003f98a3-b000-44ee-9a1d-12506f8447e4";
	}
	
	@Override
	public void initialize() 
	{
		style = new JavaScriptCodeStyle();
		code = new Parameter<>("JavaScript Code", style, header + "spectrumOut = spectrumIn;", this::validate);
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

	public boolean validate(Parameter<String> p) {
		try {
			boltmap.setScript(getCode());
			if (  boltmap.apply(new float[]{1, 2, 3, 4}) instanceof float[]  ){
				return true;
			} else {
				throw new BoltScriptExecutionException("Type mismatch for spectrumOut");
			}
		} catch (Exception e) {
			style.errorMessage = e.getMessage();
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
