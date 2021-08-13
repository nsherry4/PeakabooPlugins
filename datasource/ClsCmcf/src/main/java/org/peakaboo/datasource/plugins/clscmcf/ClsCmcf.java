package org.peakaboo.datasource.plugins.clscmcf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.metadata.Metadata;
import org.peakaboo.datasource.model.components.metadata.SimpleMetadata;
import org.peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import org.peakaboo.datasource.model.components.scandata.ScanData;
import org.peakaboo.datasource.model.components.scandata.SimpleScanData;
import org.peakaboo.datasource.plugin.AbstractDataSource;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.bolt.plugin.core.AlphaNumericComparitor;
import org.peakaboo.framework.cyclops.spectrum.ISpectrum;



public class ClsCmcf extends AbstractDataSource {

	private SimpleScanData scandata;
	private SimpleMetadata metadata;
	
	@Override
	public Optional<Group> getParameters(List<Path> paths) {
		return Optional.empty();
	}

	@Override
	public Optional<Metadata> getMetadata() {
		return Optional.of(metadata);
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
		return new ClsCmcfFileFormat();
	}

	@Override
	public ScanData getScanData() {
		return scandata;
	}

	@Override
	public void read(List<Path> paths) throws Exception {
		if (paths.isEmpty()) { throw new IllegalArgumentException("Missing files");}
		
		AlphaNumericComparitor alphacomp = new AlphaNumericComparitor();
		paths.sort((f1, f2) -> alphacomp.compare(f1.toString(), f2.toString()));
		
		Path file = paths.get(0);
		scandata = new SimpleScanData(file.getFileName().toString());
		
		
		for (Path path : paths) {
			readFile(path);			
		}
	}
	
	private void readFile(Path path) throws IOException {
		try (InputStream in = Files.newInputStream(path)) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();
			if (!line.startsWith("# XDI/1.0 ")) {
				throw new RuntimeException("File header does not match");
			}
			
			if (metadata == null) {
				metadata = new SimpleMetadata();
				Map<String, String> props = ClsCmcfFileFormat.getFileProperties(reader);
				metadata.creationTime = props.get("Scan.start_time");
				metadata.startTime = props.get("Scan.start_time");
				metadata.endTime = props.get("Scan.end_time");
				metadata.creator = props.get(""); //TODO
				metadata.facilityName = props.get("Facility.name");
				metadata.laboratoryName = props.get("Beamline.name");
				metadata.techniqueName = props.get("CMCF.scan_type");
				metadata.sampleName = props.get("Sample.name");
				metadata.instrumentName = props.get("Facility.xray_source");
			}
			
			skipHeader(reader);
			
			String headers = reader.readLine();
	
			List<Float> values = new ArrayList<>();
			float energy=0, normfluor, ifluor1, lastenergy = -Float.MAX_VALUE;
			scandata.setMinEnergy(0);
			scandata.setMaxEnergy(0);
			while (true) {
				line = reader.readLine();
				if (line == null) { break; }
				line = line.trim();
				if (line.startsWith("#")) { continue; }
				String[] parts = line.split("\\s+", 3);
				
				energy = Float.parseFloat(parts[0]);
				normfluor = Float.parseFloat(parts[1]);
				ifluor1 = Float.parseFloat(parts[2]);
				
				if (energy < lastenergy) {
					throw new RuntimeException("Eneergy channels not in sorted ascending order");
				}
				
				if (values.isEmpty()) {
					//min energy comes from the first entry
					scandata.setMinEnergy(energy);
				}
				values.add(normfluor);
			}
			scandata.setMaxEnergy(energy);
			
			scandata.add(new ISpectrum(values));
			
		}
	}
	
	private void skipHeader(BufferedReader reader) throws IOException {
		while (true) {
			String line = reader.readLine();
			if ("# ---".equals(line)) {
				break;
			}
			if (line == null) {
				throw new RuntimeException("Unexpected end of file");
			}	
		}
	}
	
	@Override
	public String pluginVersion() {
		return "0.1";
	}

	@Override
	public String pluginUUID() {
		return "d46fb0ba-e11e-4153-aab4-43d2e18903bb";
	}


	
}
