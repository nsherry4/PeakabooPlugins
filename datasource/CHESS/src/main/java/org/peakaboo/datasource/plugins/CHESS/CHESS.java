package org.peakaboo.datasource.plugins.CHESS;

import java.util.Arrays;

import org.peakaboo.datasource.plugins.GenericHDF5.FloatMatrixHDF5DataSource;

public class CHESS extends FloatMatrixHDF5DataSource  {

	private static final String[] PATHS = new String[]{
			"/2/measurement/vortex1/counts",
			"/2/measurement/vortex2/counts",
			"/2/measurement/vortex3/counts",
			"/2/measurement/vortex4/counts"
		};
	private static final String NAME = "CHESS";
	private static final String DESC = "CHESS XRF spectral data stored in an HDF5 container";
	
	public CHESS() {
		super("xz", Arrays.asList(PATHS), NAME, DESC);
	}

	@Override
	public String pluginVersion() {
		return "0.1";
	}

	@Override
	public String pluginUUID() {
		return "9f884f75-616d-4585-9c94-48e9391c7799";
	}
	

}
