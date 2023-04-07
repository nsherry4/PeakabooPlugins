package org.peakaboo.datasource.plugins.sigray;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.peakaboo.dataset.source.model.components.fileformat.FileFormat;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormatCompatibility;
import org.peakaboo.dataset.source.model.datafile.DataFile;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;

public class SigrayHDF5FileFormat implements FileFormat {

	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList("h5", "hdf5");
	}

	public FileFormatCompatibility compatibility(DataFile path) {
		
		try {
			IHDF5SimpleReader reader = HDF5Factory.openForReading(path.getAndEnsurePath().toFile());
			HDF5DataSetInformation info = reader.getDataSetInformation("/MAPS/mca_arr");
		} catch (Exception e) {
			return FileFormatCompatibility.NO;
		}
		
		return FileFormatCompatibility.YES_BY_CONTENTS;
		
	}

	@Override
	public FileFormatCompatibility compatibility(List<DataFile> paths) {
		if (paths.size() != 1) {
			return FileFormatCompatibility.NO;
		}
		
		return compatibility(paths.get(0));
	}

	@Override
	public String getFormatName() {
		return "Sigray HDF5 2015";
	}

	@Override
	public String getFormatDescription() {
		return "Sigray XRF scans in an HDF5 container";
	}

}
