package org.peakaboo.filter.editors;

import org.peakaboo.framework.autodialog.model.style.CoreStyle;
import org.peakaboo.framework.autodialog.model.style.SimpleStyle;

public class CodeStyle extends SimpleStyle<String> {

	public String errorMessage;

	public CodeStyle(String style) {
		super(style, CoreStyle.TEXT_AREA);
	}

}