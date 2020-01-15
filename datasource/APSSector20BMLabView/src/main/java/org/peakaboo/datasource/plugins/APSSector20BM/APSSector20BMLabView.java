package org.peakaboo.datasource.plugins.APSSector20BM;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.metadata.Metadata;
import org.peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import org.peakaboo.datasource.model.components.scandata.ScanData;
import org.peakaboo.datasource.model.components.scandata.SimpleScanData;
import org.peakaboo.datasource.plugin.AbstractDataSource;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.cyclops.ISpectrum;



public class APSSector20BMLabView extends AbstractDataSource {

	private SimpleScanData scandata;
	
	@Override
	public Optional<Group> getParameters(List<Path> paths) {
		return Optional.empty();
	}

	@Override
	public Optional<Metadata> getMetadata() {
		return Optional.empty();
	}

	@Override
	public Optional<DataSize> getDataSize() {
		return Optional.empty();
	}

	@Override
	public Optional<PhysicalSize> getPhysicalSize() {
		return Optional.empty();
	}

	@Override
	public FileFormat getFileFormat() {
		return new APSSector20BMLabViewFileFormat();
	}

	@Override
	public ScanData getScanData() {
		return scandata;
	}

	@Override
	public void read(List<Path> paths) throws Exception {
		if (paths.size() != 1) {
			throw new IllegalArgumentException("Expected exactly 1 file");
		}
		
		Path path = paths.get(0);
		
		scandata = new SimpleScanData(path.getFileName().toString());
		
		try (InputStream in = Files.newInputStream(path)) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			//search for the start of the data
			while (true) {
				String line = reader.readLine();
				if (line == null) { return; }
				if (line.trim().toLowerCase().startsWith("# column headings:")) {
					//found the start of the data
					break;
				}
			}
			
			//clear the "#channel det 1 det 2" header line
			reader.readLine();
			
			List<Float> channels = new ArrayList<>();
			while (true) {
				String line = reader.readLine();
				if (line == null) { break; }
				line = line.trim();
				if (line.startsWith("#")) { continue; }
				String[] parts = line.split("\\s+");
				int channelNumber = Integer.parseInt(parts[0]);
				if (channelNumber != channels.size()) {
					throw new RuntimeException("Channels must be listed in order");
				}
				float sum = 0f;
				for (String part : Arrays.copyOfRange(parts, 1, parts.length)) {
					sum += Float.parseFloat(part);
				}
				sum /= (float)(parts.length-1);
				channels.add(sum);
			}
			
			scandata.add(new ISpectrum(channels));
			
		}
		
	}

	@Override
	public String pluginVersion() {
		return "1.0";
	}

	@Override
	public String pluginUUID() {
		return "675b80f4-c6b3-4e5f-89e5-ac1c01dec86d";
	}
	
}
