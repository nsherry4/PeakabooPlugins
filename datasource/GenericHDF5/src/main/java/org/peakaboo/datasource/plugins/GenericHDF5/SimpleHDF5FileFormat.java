package org.peakaboo.datasource.plugins.GenericHDF5;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.fileformat.FileFormatCompatibility;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;

public class SimpleHDF5FileFormat implements FileFormat {

	private List<String> dataPaths;
	private String formatName, formatDescription;
	
	public SimpleHDF5FileFormat(String dataPath, String formatName, String formatDescription) {
		this.dataPaths = Collections.singletonList(dataPath);
		this.formatName = formatName;
		this.formatDescription = formatDescription;
	}
	
	public SimpleHDF5FileFormat(List<String> dataPaths, String formatName, String formatDescription) {
		this.dataPaths = dataPaths;
		this.formatName = formatName;
		this.formatDescription = formatDescription;
	}
	
	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList(new String[] {"h5", "hdf5"});
	}

	public FileFormatCompatibility compatibility(Path path) {
		try (IHDF5SimpleReader reader = HDF5Factory.openForReading(path.toFile())) {
			for (String dataPath : dataPaths) {
				reader.getDataSetInformation(dataPath);
			}
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
		return formatName;
	}

	@Override
	public String getFormatDescription() {
		return formatDescription;
	}

}
