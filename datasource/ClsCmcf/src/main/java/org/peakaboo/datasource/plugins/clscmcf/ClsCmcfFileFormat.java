package org.peakaboo.datasource.plugins.clscmcf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.peakaboo.app.PeakabooLog;
import org.peakaboo.dataset.io.DataInputAdapter;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormat;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormatCompatibility;
import org.peakaboo.framework.bolt.plugin.core.AlphaNumericComparitor;

public class ClsCmcfFileFormat implements FileFormat {

	@Override
	public List<String> getFileExtensions() {
		return Arrays.asList("xdi");
	}

	@Override
	public FileFormatCompatibility compatibility(List<DataInputAdapter> inputs) {
		if (inputs.isEmpty()) { return FileFormatCompatibility.NO; }
		
		AlphaNumericComparitor alphacomp = new AlphaNumericComparitor();
		inputs.sort((f1, f2) -> alphacomp.compare(f1.getFilename(), f2.getFilename()));
		
		for (DataInputAdapter input : inputs) {
			if (!input.getFilename().endsWith("xdi")) {
				return FileFormatCompatibility.NO;
			}
		}
		
		DataInputAdapter file = inputs.get(0);
		
		try (InputStream in = file.getInputStream()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();
			if (!line.startsWith("# XDI/1.0 ")) {
				return FileFormatCompatibility.NO;
			}
			
			//check properties
			Map<String, String> properties = getFileProperties(reader);			
			if (
					(!"XRFScanner".equals(properties.get("CMCF.scan_type"))) ||
					(!"energy keV".equals(properties.get("Column.1"))) ||
					(!"normfluor".equals(properties.get("Column.2"))) ||
					(!"ifluor.1".equals(properties.get("Column.3")))			
				) {
				return FileFormatCompatibility.NO;
			}
			
			return FileFormatCompatibility.YES_BY_CONTENTS;
			
		} catch (IOException e) {
			PeakabooLog.get().log(Level.WARNING, "Failed to inspect file", e);
			return FileFormatCompatibility.NO;
		}
	}
	
	/**
	 * Takes a BufferedReader which has been advanced past the first header line to
	 * the properies section of the document and returns the properties as a map
	 * @throws IOException 
	 */
	static Map<String, String> getFileProperties(BufferedReader reader) throws IOException {
		Map<String, String> props = new HashMap<>();
		
		while (true) {
			String line = reader.readLine();
			if (!line.startsWith("# ") || line.equals("# ///")) { break; }
			String[] parts = line.substring(2).split(": ", 2);
			if (parts.length != 2) { break; }
			props.put(parts[0], parts[1]);
		}
		
		return props;
	}

	@Override
	public String getFormatName() {
		return "CLS CMCF";
	}

	@Override
	public String getFormatDescription() {
		return "CLS CMCF's single-point XRF data format";
	}

}
