package peakaboo.datasource.plugins.vespers;

import java.io.File;
import java.util.Arrays;

import peakaboo.datasource.model.DataSource;
import peakaboo.datasource.model.components.fileformat.FileFormatCompatibility;

public class PeakabooTest {

	public static void main(String[] args) {
		
		DataSource ds = new ScienceStudioDataSource();
		
		FileFormatCompatibility compat = ds.getFileFormat().compatibility(Arrays.asList(new File("/home/nathaniel/Projects/SS/Peakaboo Data/Old CLS/12hcaustic.006.dat").toPath(), new File("/home/nathaniel/Projects/SS/Peakaboo Data/Old CLS/12hcaustic.006_spectra.dat").toPath()));
		System.out.println(compat);
		
	}
	
}
