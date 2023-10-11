package org.peakaboo.datasource.plugins.Horiba5200;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.peakaboo.dataset.source.model.DataSourceReadException;
import org.peakaboo.dataset.source.model.components.datasize.DataSize;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormat;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormatCompatibility;
import org.peakaboo.dataset.source.model.components.metadata.Metadata;
import org.peakaboo.dataset.source.model.components.physicalsize.PhysicalSize;
import org.peakaboo.dataset.source.model.components.scandata.PipelineScanData;
import org.peakaboo.dataset.source.model.components.scandata.ScanData;
import org.peakaboo.dataset.source.model.components.scandata.SimpleScanData;
import org.peakaboo.dataset.source.model.datafile.DataFile;
import org.peakaboo.dataset.source.plugin.AbstractDataSource;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.cyclops.spectrum.ISpectrum;
import org.peakaboo.framework.cyclops.spectrum.Spectrum;

public class Horiba5200 extends AbstractDataSource implements FileFormat {

	private static final String NAME = "Horiba XGT-5200";
	private static final String DESC = "Horiba XGT-5200 micro-XRF Data";
	
	private PipelineScanData scandata;
	
	public Horiba5200() {
	}

	@Override
	public String pluginVersion() {
		return "0.3";
	}

	@Override
	public String pluginUUID() {
		return "e300f15f-3a3d-45cc-92f4-496abf41cd4e";
	}

	@Override
	public Optional<Group> getParameters(List<DataFile> paths) {
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
		return this;
	}

	@Override
	public ScanData getScanData() {
		return scandata;
	}

	@Override
	public void read(List<DataFile> paths) throws DataSourceReadException, IOException, InterruptedException {
		if (paths.size() == 0) { return; }
		DataFile first = paths.get(0);
		
		Map<String, String> properties = getProperties(first);
		
		int channels = toInt(properties.get("Num. of ch"));
		float energy = toFloat(properties.get("Energy of ch(eV)"));
		scandata = new PipelineScanData(DataFile.getTitle(paths));
		scandata.setMaxEnergy(channels * energy / 1000);
		
		//read the numbers
		int index = 0;
		for (DataFile path : paths) {
			scandata.submit(index++, readSingle(path, channels));
		}
		
		scandata.finish();
	}
	
	private Spectrum readSingle(DataFile path, int channels) throws IOException {
		Spectrum s = new ISpectrum(channels);
		int index = 0;
		List<String> lines = path.toLines(Charset.forName("windows-1252"));
		for (String line : lines) {
			//skip over the properties
			if (!isNumeric(line)) { continue; }
			s.add(toFloat(line));
			if (++index >= channels) { break; }
		}
		return s;
	}
	
	private Map<String, String> getProperties(DataFile path) throws IOException {
		//read the header until we hit numbers
		List<String> lines = path.toLines(Charset.forName("windows-1252"));
		Map<String, String> properties = new HashMap<>();
		for (String line : lines) {
			if (isNumeric(line)) { break; }
			String[] parts = line.split("\t", 2);
			String key = parts[0].trim();
			String value = "".trim();
			if (parts.length > 1) { value = parts[1]; }
			properties.put(key, value);
		}
		return properties;
	}
	
	private static boolean isNumeric(String str) {
		try {
			toFloat(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	private static float toFloat(String str) {
		return Float.parseFloat(str.trim());
	}
	
	private static int toInt(String str) {
		return Integer.parseInt(str.trim());
	}

	
	/*
	 * FILE FORMAT METHODS
	 */
	
	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList("txt");
	}

	@Override
	public FileFormatCompatibility compatibility(List<DataFile> filenames) {
		try {
			DataFile first = filenames.get(0);
			//first, reject files larger than 1MB
			if (first.size().orElse(0l) > 1000000) {
				return FileFormatCompatibility.NO;
			}
		
			Map<String, String> properties = getProperties(first);
			if (!properties.containsKey("Label")) { return FileFormatCompatibility.NO; }
			if (!properties.containsKey("Date")) { return FileFormatCompatibility.NO; }
			if (!properties.containsKey("Path")) { return FileFormatCompatibility.NO; }
			if (!properties.containsKey("Num. of ch")) { return FileFormatCompatibility.NO; }
			return FileFormatCompatibility.YES_BY_CONTENTS;
		} catch (Exception e) {
			return FileFormatCompatibility.NO;
		}
	}

	@Override
	public String getFormatName() {
		return NAME;
	}

	@Override
	public String getFormatDescription() {
		return DESC;
	}
	
}
