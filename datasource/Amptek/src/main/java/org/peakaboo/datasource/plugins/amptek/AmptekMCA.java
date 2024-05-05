package org.peakaboo.datasource.plugins.amptek;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.peakaboo.dataset.io.DataInputAdapter;
import org.peakaboo.dataset.source.model.DataSourceReadException;
import org.peakaboo.dataset.source.model.components.datasize.DataSize;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormat;
import org.peakaboo.dataset.source.model.components.fileformat.SimpleFileFormat;
import org.peakaboo.dataset.source.model.components.metadata.Metadata;
import org.peakaboo.dataset.source.model.components.physicalsize.PhysicalSize;
import org.peakaboo.dataset.source.model.components.scandata.ScanData;
import org.peakaboo.dataset.source.model.components.scandata.analysis.Analysis;
import org.peakaboo.dataset.source.plugin.AbstractDataSource;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.cyclops.spectrum.ArraySpectrum;
import org.peakaboo.framework.cyclops.spectrum.Spectrum;
import org.peakaboo.tier.Tier;



public class AmptekMCA extends AbstractDataSource implements ScanData {

	private Spectrum spectrum;
	private String scanName; 
	private Analysis analysis;
	
	
	public AmptekMCA() {

	}
	
	@Override
	public String pluginVersion() {
		return "1.4";
	}
	
	@Override
	public String pluginUUID() {
		return "fe4f9def-9cf9-46b8-b460-bbde62a6f144";
	}
	
	private Spectrum readMCA(DataInputAdapter file) throws IOException
	{
	
		List<String> lines = IOUtils.readLines(file.getInputStream()); 
		
		int startIndex = lines.indexOf("<<DATA>>") + 1;
		int endIndex = lines.indexOf("<<END>>");
		
		Spectrum s = new ArraySpectrum(lines.subList(startIndex, endIndex).stream().map(line -> Float.parseFloat(line)).collect(toList()));
		
		analysis = Tier.provider().createDataSourceAnalysis();
		analysis.process(s);
		
		return s;
		
	}
	
	@Override
	public Spectrum get(int index) {
		if (index != 0) return null;
		return spectrum;
	}

	@Override
	public int scanCount() {
		return 1;
	}

	@Override
	public String scanName(int index) {
		return "Scan #" + (index+1);
	}


	@Override
	public float maxEnergy() {
		return 0;
	}

	@Override
	public float minEnergy() {
		return 0;
	}
	
	@Override
	public String datasetName() {
		return scanName;
	}

	
	@Override
	public FileFormat getFileFormat() {
		return new SimpleFileFormat(true, "Amptek MCA", "Amptek MCA XRF data format", Arrays.asList("mca"));
	}
	
	@Override
	public ScanData getScanData() {
		return this;
	}

	
	

	public void read(DataInputAdapter file) throws IOException
	{
		spectrum = readMCA(file);
		scanName = file.getFilename();
	}

	@Override
	public void read(DataSourceContext ctx) throws DataSourceReadException, IOException, InterruptedException {
		List<DataInputAdapter> files = ctx.inputs();
		
		if (files == null) throw new UnsupportedOperationException();
		if (files.size() == 0) throw new UnsupportedOperationException();
		if (files.size() > 1) throw new UnsupportedOperationException();
		
		read(files.get(0));
	}

	

	
	//==============================================
	// UNSUPPORTED FEATURES
	//==============================================
	

	@Override
	public Optional<DataSize> getDataSize() {
		return Optional.empty();
	}

	
	@Override
	public Optional<Metadata> getMetadata() {
		return Optional.empty();
	}

	@Override
	public Optional<PhysicalSize> getPhysicalSize() {
		return Optional.empty();
	}

	@Override
	public Optional<Group> getParameters(List<DataInputAdapter> paths) {
		return Optional.empty();
	}

	@Override
	public Analysis getAnalysis() {
		return this.analysis;
	}


	
}
