package peakaboo.datasource.plugins.vespers;

import peakaboo.datasource.model.components.fileformat.FileFormat;

/**
 * @author maxweld
 *
 */
public class ScienceStudioDataSource extends ConverterFactoryDelegatingDSP
{

	@Override
	public String pluginName() {
		return getFormatName();
	}

	@Override
	public String pluginDescription() {
		return getFormatDescription();
	}
	
	@Override
	public String pluginVersion() {
		return "1.1";
	}
	
	@Override
	public String pluginUUID() {
		return "dac5aa61-d157-463d-a13d-b29f05e8d926";
	}
	
	@Override
	public String getFormatName() {
		return "VESPERS CLS Data Acquisition Format";
	}

	@Override
	public String getFormatDescription()
	{
		return "Data format used by the Canadian Light Source for XRF collection on the VESPERS beamline";
	}

	@Override
	public boolean pluginEnabled() {
		return true;
	}
	
	
	@Override
	public FileFormat getFileFormat() {
		return this;
	}


	
}
