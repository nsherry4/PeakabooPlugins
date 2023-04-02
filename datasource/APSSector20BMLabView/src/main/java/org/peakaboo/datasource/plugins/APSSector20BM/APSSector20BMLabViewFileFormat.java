package org.peakaboo.datasource.plugins.APSSector20BM;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.peakaboo.app.PeakabooLog;
import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.fileformat.FileFormatCompatibility;
import org.peakaboo.datasource.model.datafile.DataFile;

public class APSSector20BMLabViewFileFormat implements FileFormat {

	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList("0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009");
	}

	@Override
	public FileFormatCompatibility compatibility(List<DataFile> filenames) {
		if (filenames.size() != 1) {
			return FileFormatCompatibility.NO;
		}
		
		DataFile file = filenames.get(0);
		try (InputStream in = file.getInputStream()){
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			
			String line;
			while (true) {
				line = reader.readLine();
				if (line == null) {
					//whoopsie
					return FileFormatCompatibility.NO;
				}
				if (!line.trim().isEmpty()) {
					//found the first non-empty line
					break;
				}
			}
			
			
			if (line.trim().toLowerCase().startsWith("#1-d data file")) {
				return FileFormatCompatibility.YES_BY_CONTENTS;
			} else {
				return FileFormatCompatibility.NO;
			}
			
		} catch (IOException e) {
			PeakabooLog.get().log(Level.WARNING, "Failed to inspect file(s)", e);
			return FileFormatCompatibility.NO;
		}
		
	}

	@Override
	public String getFormatName() {
		return "APS Sector 20-BM LabVIEW";
	}

	@Override
	public String getFormatDescription() {
		return "LabVIEW Spot Data from Sector 20-BM of the APS Synchrotron";
	}


}
