package org.peakaboo.datasource.plugins.UniversalHDF5;

import org.peakaboo.datasource.model.components.fileformat.SimpleFileFormat;

public class UniversalHDF5DFileFormat extends SimpleFileFormat {

	public UniversalHDF5DFileFormat() {
		super(false, "Generic HDF5 Files", "A generic HDF5 datasource that allows users to select the data to open", new String[] {"h5", "hdf5"});
	}

}
