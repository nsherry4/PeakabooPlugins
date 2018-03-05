package peakaboo.datasource.plugins.sigray2018;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;
import peakaboo.datasource.model.components.fileformat.FileFormat;
import peakaboo.datasource.model.components.fileformat.FileFormatCompatibility;

public class Sigray2018HDF5FileFormat implements FileFormat {

	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList("h5", "hdf5");
	}

	@Override
	public FileFormatCompatibility compatibility(File filename) {
		
		try {
			IHDF5SimpleReader reader = HDF5Factory.openForReading(filename);
			HDF5DataSetInformation info = reader.getDataSetInformation("/entry/detector/data1");
		} catch (Exception e) {
			return FileFormatCompatibility.NO;
		}
		
		return FileFormatCompatibility.YES_BY_CONTENTS;
		
	}

	@Override
	public FileFormatCompatibility compatibility(List<File> filenames) {
		return compatibility(filenames.get(0));
	}

	@Override
	public String getFormatName() {
		return "Other Sigray HDF5";
	}

	@Override
	public String getFormatDescription() {
		return "Other Sigray XRF scans in an HDF5 container";
	}

}
