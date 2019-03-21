package org.peakaboo.filter.plugins.programming;


import org.peakaboo.filter.editors.CodeEditor;
import org.peakaboo.filter.model.AbstractSimpleFilter;
import org.peakaboo.filter.model.FilterType;

import org.peakaboo.framework.cyclops.ISpectrum;
import org.peakaboo.framework.cyclops.ReadOnlySpectrum;
import org.peakaboo.framework.autodialog.model.Parameter;
import org.peakaboo.framework.autodialog.model.style.editors.TextAreaStyle;
import org.peakaboo.framework.bolt.scripting.BoltScriptExecutionException;
import org.peakaboo.framework.bolt.scripting.functions.BoltMap;


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
		return "1.2";
	}
	
	@Override
	public String pluginUUID() {
		return "725c9b05-5fd1-45d9-8b89-3b366c91e398";
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
			//TODO: Need a PythonCodeStyle to move this data to the ui
			//style.errorMessage = e.getMessage();
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
