package org.peakaboo.datasource.plugins.APSSector11;

import org.peakaboo.dataset.source.plugin.plugins.universalhdf5.FloatMatrixHDF5DataSource;

public class APSSector11 extends FloatMatrixHDF5DataSource  {

	private static final String PATH = "/data/data";
	private static final String NAME = "APS-11 Format";
	private static final String DESC = "APS-11 XRF spectral data stored in an HDF5 container";
	
	public APSSector11() {
		super("yxz", PATH, NAME, DESC);
	}

	@Override
	public String pluginVersion() {
		return "0.2";
	}

	@Override
	public String pluginUUID() {
		return "37b76d2b-7cab-4b9f-9609-e4e9aba071bd";
	}
	

}
