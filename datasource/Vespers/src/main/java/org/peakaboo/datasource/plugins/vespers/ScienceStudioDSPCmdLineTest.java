package org.peakaboo.datasource.plugins.vespers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.peakaboo.dataset.io.DataInputAdapter;
import org.peakaboo.dataset.io.PathDataInputAdapter;
import org.peakaboo.dataset.source.model.DataSource;
import org.peakaboo.dataset.source.model.DataSource.DataSourceContext;
import org.peakaboo.dataset.source.model.components.datasize.DataSize;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormatCompatibility;
import org.peakaboo.dataset.source.model.components.metadata.Metadata;
import org.peakaboo.dataset.source.model.components.physicalsize.PhysicalSize;
import org.peakaboo.framework.cyclops.spectrum.SpectrumView;

/**
 * @author maxweld
 *
 */
public class ScienceStudioDSPCmdLineTest {

	public static void main(String[] args) throws Exception {

		if(args.length == 0) {
			System.out.println("Usage: java -jar ScienceStudioDSP.jar dataFile.dat [ dataFile_spectra.dat ]");
			return;
		}
		
		DataSource dataSource = new ScienceStudioDataSource();
		
		List<String> filenames = Arrays.asList(args);
		List<DataInputAdapter> files = filenames.stream().map(PathDataInputAdapter::new).collect(Collectors.toList());
		
		if(dataSource.getFileFormat().compatibility(files) != FileFormatCompatibility.NO) {
			System.out.println("Reading: " + filenames);
			var ctx = new DataSourceContext(files);
			dataSource.read(ctx);
			System.out.println("DONE!");
		}		
		else {
			System.out.println("Cannot Read: " + filenames);
		}
		
		
		Optional<Metadata> optMetadata = dataSource.getMetadata();
		Optional<DataSize> optDatasize = dataSource.getDataSize();
		Optional<PhysicalSize> optPhysical = dataSource.getPhysicalSize();
		
		System.out.println("DatasetName: " +  dataSource.getScanData().datasetName());
		if (optMetadata.isPresent()) {
			System.out.println("Creator: " + optMetadata.get().getCreator());
			System.out.println("CreationTime: " + optMetadata.get().getCreationTime());
			System.out.println("StartTime: " + optMetadata.get().getStartTime());
			System.out.println("EndTime: " + optMetadata.get().getEndTime());
		}
		
		System.out.println("Scan Count: " + dataSource.getScanData().scanCount());
		if (optDatasize.isPresent()) {
			System.out.println("Data Dimensions: " + optDatasize.get().getDataDimensions());
		}
		
		if (optPhysical.isPresent()) {
			System.out.println("Real Dimensions: " + optPhysical.get().getPhysicalDimensions());
		}
	
		for(int idx=0; (idx<dataSource.getScanData().scanCount()) && (idx<100); idx++) {
			System.out.print(dataSource.getScanData().scanName(idx) + ": ");
			if (optDatasize.isPresent()) {
				System.out.print(optDatasize.get().getDataCoordinatesAtIndex(idx) + ": ");
			}
			if (optPhysical.isPresent()) {
				System.out.print(optPhysical.get().getPhysicalCoordinatesAtIndex(idx) + ": ");
			}
			System.out.print(spectrumToString(dataSource.getScanData().get(idx), 20));
			System.out.println();
		}
	}

	protected static String spectrumToString(SpectrumView spectrum, int limit) {
		boolean first = true;
		StringBuffer buffer = new StringBuffer("[");
		for(int i=0; i<spectrum.size(); i++) {
			if((limit >= 0) && (limit <= i)) {
				buffer.append(",...");
				break;
			}
			if(first) {
				first = false;
				buffer.append(spectrum.get(i));
			} else {
				buffer.append(", ");
				buffer.append(spectrum.get(i));
			}
		}
		buffer.append("]");
		return buffer.toString();
	}
}
