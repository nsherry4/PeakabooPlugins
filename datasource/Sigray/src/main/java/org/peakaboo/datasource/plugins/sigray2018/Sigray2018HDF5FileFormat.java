package org.peakaboo.datasource.plugins.sigray2018;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.fileformat.FileFormatCompatibility;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;

public class Sigray2018HDF5FileFormat implements FileFormat {

	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList("h5", "hdf5");
	}

	public FileFormatCompatibility compatibility(Path filename) {
		
		try {
			IHDF5SimpleReader reader = HDF5Factory.openForReading(filename.toFile());
			reader.getDataSetInformation("/entry/detector/data1");
		} catch (Exception e) {
			return FileFormatCompatibility.NO;
		}
		
		return FileFormatCompatibility.YES_BY_CONTENTS;
		
	}

	@Override
	public FileFormatCompatibility compatibility(List<Path> filenames) {
		return compatibility(filenames.get(0));
	}

	@Override
	public String getFormatName() {
		return "Sigray HDF5 2018";
	}

	@Override
	public String getFormatDescription() {
		return "Other Sigray XRF scans in an HDF5 container";
	}

}
