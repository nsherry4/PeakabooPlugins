package org.peakaboo.datasource.plugins.GenericJHDF;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.fileformat.FileFormatCompatibility;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import io.jhdf.api.Node;

public class SimpleJHDFFileFormat implements FileFormat {

	private List<String> dataPaths;
	private String formatName, formatDescription;
	
	public SimpleJHDFFileFormat(String dataPath, String formatName, String formatDescription) {
		this.dataPaths = Collections.singletonList(dataPath);
		this.formatName = formatName;
		this.formatDescription = formatDescription;
	}
	
	public SimpleJHDFFileFormat(List<String> dataPaths, String formatName, String formatDescription) {
		this.dataPaths = dataPaths;
		this.formatName = formatName;
		this.formatDescription = formatDescription;
	}
	
	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList(new String[] {"h5", "hdf5"});
	}

	public FileFormatCompatibility compatibility(Path path) {
		try (HdfFile hdf = new HdfFile(path)){
			for (String dataPath : dataPaths) {
				Node node = hdf.getByPath(dataPath);
			}
		} catch (Exception e) {
			System.out.println(e);
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
