package peakaboo.datasource.plugins.sigray;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;
import peakaboo.datasource.model.components.fileformat.FileFormat;
import peakaboo.datasource.model.components.fileformat.FileFormatCompatibility;

public class SigrayHDF5FileFormat implements FileFormat {

	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList("h5", "hdf5");
	}

	@Override
	public FileFormatCompatibility compatibility(Path path) {
		
		try {
			IHDF5SimpleReader reader = HDF5Factory.openForReading(path.toFile());
			HDF5DataSetInformation info = reader.getDataSetInformation("/MAPS/mca_arr");
		} catch (Exception e) {
			return FileFormatCompatibility.NO;
		}
		
		return FileFormatCompatibility.YES_BY_CONTENTS;
		
	}

	@Override
	public FileFormatCompatibility compatibility(List<Path> filenames) {
		return FileFormatCompatibility.NO;
	}

	@Override
	public String getFormatName() {
		return "Sigray HDF5";
	}

	@Override
	public String getFormatDescription() {
		return "Sigray XRF scans in an HDF5 container";
	}

}
