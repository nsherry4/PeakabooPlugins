package org.peakaboo.datasource.plugins.vespers;

import org.peakaboo.dataset.source.model.DataSource;

public class PeakabooTest {

	public static void main(String[] args) {
		
		DataSource ds = new ScienceStudioDataSource();
		
		//FileFormatCompatibility compat = ds.getFileFormat().compatibility(Arrays.asList(new File("12hcaustic.006.dat").toPath(), new File("12hcaustic.006_spectra.dat").toPath()));
		//System.out.println(compat);
		
	}
	
}
