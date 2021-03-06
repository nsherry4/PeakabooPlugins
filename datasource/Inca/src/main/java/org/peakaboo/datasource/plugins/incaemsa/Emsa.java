package org.peakaboo.datasource.plugins.incaemsa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.fileformat.FileFormatCompatibility;
import org.peakaboo.datasource.model.components.metadata.Metadata;
import org.peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import org.peakaboo.datasource.model.components.scandata.ScanData;
import org.peakaboo.datasource.model.components.scandata.SimpleScanData;
import org.peakaboo.datasource.plugin.AbstractDataSource;

import org.peakaboo.framework.cyclops.ISpectrum;
import org.peakaboo.framework.cyclops.Spectrum;
import org.peakaboo.framework.autodialog.model.Group;

public class Emsa extends AbstractDataSource implements FileFormat {

	private SimpleScanData scanData;
	private Map<String, String> tags;
	
	private static final String FORMAT_HEADER = "#FORMAT      : EMSA/MAS Spectral Data File";
	
	public Emsa() {
		
	}
	
	
	@Override
	public String pluginVersion() {
		return "1.3";
	}
	
	@Override
	public String pluginUUID() {
		return "f5af3f37-9c96-41c9-87cf-7042d7e299f4";
	}
	
	/******************************
	 * DataSourceFileFormat
	 ******************************/
	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList("txt", "emsa", "msa");
	}

	public FileFormatCompatibility compatibility(Path file) {
		if (
				!file.toAbsolutePath().toString().toLowerCase().endsWith(".txt") && 
				!file.toAbsolutePath().toString().toLowerCase().endsWith(".emsa") &&
				!file.toAbsolutePath().toString().toLowerCase().endsWith(".msa")
			) return FileFormatCompatibility.NO;
		
		try {
			Scanner scanner = new Scanner(file);
			scanner.useDelimiter("\n");
			if (!scanner.hasNext()) return FileFormatCompatibility.NO;
			String line = scanner.next();
			if (!line.trim().toLowerCase().equals(FORMAT_HEADER.toLowerCase())) return FileFormatCompatibility.NO;
			return FileFormatCompatibility.YES_BY_CONTENTS;
		} catch (IOException e) {
			return FileFormatCompatibility.NO;
		}
			
	}

	@Override
	public FileFormatCompatibility compatibility(List<Path> files) {
		if (files.size() == 0) return FileFormatCompatibility.NO;
		return compatibility(files.get(0));
	}

	
	@Override
	public FileFormat getFileFormat() {
		return this;
	}
	

	@Override
	public String getFormatName() {
		return "Inca EMSA";
	}

	@Override
	public String getFormatDescription() {
		return "Inca EMSA X-ray Spectra";
	}
	
	
	
	
	
	
	
	private void readTags(String filename) throws Exception {
		
		if (tags != null) { return; }
		tags = new HashMap<>();
		
		Scanner scanner = new Scanner(new File(filename));
		scanner.useDelimiter("\n");
		
		while (scanner.hasNext()) {
			String line = scanner.next();
			if (!line.trim().startsWith("#")) continue; //not a comment
			if (!line.contains(":")) { continue; } //not a key:value comment
			
			String key, value, parts[];
			parts = line.split(":");
			key = parts[0].trim().substring(1).trim();  //get rid of #
			value = parts[1].trim();
			tags.put(key, value);
			
		}
		scanner.close();
	}
	
	public void read(Path file) throws Exception {
		
		scanData = new SimpleScanData(getFormatName());
		
		readTags(file.toAbsolutePath().toString());
		
		Map<Float, Float> energies = new HashMap<>();
		Scanner scanner = new Scanner(file);
		scanner.useDelimiter("\n");
		
		while (scanner.hasNext()) {
			String line = scanner.next();
			if (line.trim().startsWith("#")) continue; //comment
			if (!line.contains(",")) continue; //not an "energy, count" line
			
			String energy, counts, parts[];
			parts = line.split(",");
			energy = parts[0].trim();
			counts = parts[1].trim();
			
			try {
				energies.put(Float.parseFloat(energy), Float.parseFloat(counts));
			} catch (NumberFormatException e) {}
			
		}
		scanner.close();
		
		
		float tag_xperchan = 0.1f, tag_offset = 0, tag_choffset = 0;
		int tag_npoints = 2048;
				
		//width of channel
		if (tags.containsKey("XPERCHAN")) {
			tag_xperchan = Float.parseFloat(tags.get("XPERCHAN"));
		}
		
		//energy value of first channel
		if (tags.containsKey("OFFSET")) {
			tag_offset = Float.parseFloat(tags.get("OFFSET"));
		}
		
		//translation of all channelCount
		if (tags.containsKey("CHOFFSET")) {
			tag_choffset = Float.parseFloat(tags.get("CHOFFSET"));
		}
		
		//number of channelCount
		if (tags.containsKey("NPOINTS")) {
			tag_npoints = (int)Float.parseFloat(tags.get("NPOINTS"));
		}
			
		Spectrum spectrum = new ISpectrum(tag_npoints );
		
		List<Float> keys = new ArrayList<>(energies.keySet());
		keys.sort((a, b) -> Float.compare(a, b));

		float offset = tag_offset;
		float maxEnergy=0f;
		float minEnergy=Float.MAX_VALUE;
		for (float energy : keys) {
			
			if (offset < 0) {
				offset += tag_xperchan;
				continue;
			}
			
			spectrum.add(energies.get(energy));
			
			if (energy < minEnergy) {
				minEnergy = energy;
			}
			if (energy > maxEnergy) {
				maxEnergy = energy;
			}
			
			
		}
		scanData.setMaxEnergy(maxEnergy);
		scanData.setMinEnergy(minEnergy);
		scanData.add(spectrum);
		
	}

	@Override
	public void read(List<Path> files) throws Exception {
		for (Path file : files) {
			read(file);
		}
	}

	@Override
	public ScanData getScanData() {
		return scanData;
	}
	
	
	//==============================================
	// UNSUPPORTED FEATURES
	//==============================================
	

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
	public Optional<Group> getParameters(List<Path> paths) {
		return Optional.empty();
	}






}
