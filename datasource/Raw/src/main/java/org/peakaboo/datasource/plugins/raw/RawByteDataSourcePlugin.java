package org.peakaboo.datasource.plugins.raw;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.peakaboo.dataset.source.model.components.datasize.DataSize;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormat;
import org.peakaboo.dataset.source.model.components.fileformat.SimpleFileFormat;
import org.peakaboo.dataset.source.model.components.metadata.Metadata;
import org.peakaboo.dataset.source.model.components.physicalsize.PhysicalSize;
import org.peakaboo.dataset.source.model.components.scandata.PipelineScanData;
import org.peakaboo.dataset.source.model.components.scandata.ScanData;
import org.peakaboo.dataset.source.model.datafile.DataFile;
import org.peakaboo.dataset.source.plugin.AbstractDataSource;
import org.peakaboo.filter.model.Filter;
import org.peakaboo.filter.plugins.noise.WeightedAverageNoiseFilter;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.autodialog.model.Parameter;
import org.peakaboo.framework.autodialog.model.style.editors.IntegerSpinnerStyle;
import org.peakaboo.framework.cyclops.spectrum.ISpectrum;
import org.peakaboo.framework.cyclops.spectrum.ReadOnlySpectrum;
import org.peakaboo.framework.cyclops.spectrum.Spectrum;

public class RawByteDataSourcePlugin extends AbstractDataSource {

	Parameter<Integer> paramChannels;
	Group parameters;
	
	PipelineScanData scandata;
	
	public RawByteDataSourcePlugin() {
		paramChannels = new Parameter<Integer>("Channels per Spectrum", new IntegerSpinnerStyle(), 2048);
		parameters = new Group("Parameters", paramChannels);
	}
	
	@Override
	public Optional<Group> getParameters(List<DataFile> datafiles) {
		if (datafiles.size() != 1) {
			throw new IllegalArgumentException(getFileFormat().getFormatName() + " requires exactly 1 file");
		}
		DataFile datafile = datafiles.get(0);		
		
		/*
		 * We perform a kind of autocorrelation test to try and guess what the channel
		 * count per spectrum is.
		 */
		ReadOnlySpectrum sample = smoothSample(loadSample(datafile, 65536));
		if (sample != null) {
			float bestScore = 0;
			int bestPeriod = 2048;
			for (int period = 100; period <= 8192; period++) {
				float score = offsetScore(sample, period);
				if (score > bestScore) {
					bestPeriod = period;
					bestScore = score;
				}
			}
						
			paramChannels.setValue(bestPeriod);
		}
		
		return Optional.of(parameters);
	}

	private Spectrum loadSample(DataFile datafile, int size) {
		
		try {
			BufferedInputStream bis = new BufferedInputStream(datafile.getInputStream());
			byte[] byteArray = new byte[size];
			int bytesRead = bis.read(byteArray);
			if (bytesRead != size) {
				return null;
			}
			Spectrum sample = new ISpectrum(size);
			for (int i = 0; i < size; i++) {
				sample.set(i, byteArray[i]);
			}
			return sample;
		} catch (IOException e) {
			return null;
		}

	}
	
	private ReadOnlySpectrum smoothSample(Spectrum sample) {
		Filter filter = new WeightedAverageNoiseFilter();
		filter.initialize();
		return filter.filter(sample);
	}
	
	private float offsetScore(ReadOnlySpectrum sample, int offset) {
		/*
		 * We don't compensate for the number of points overlapping, because more points
		 * overlapping biases the algorithm towards not detecting some multiple of the
		 * real period
		 */
		int sum = 0;
		for (int i = offset; i < sample.size() - offset; i++) {
			sum += sample.get(i) * sample.get(offset+i);
		}
		return sum;

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
		return new SimpleFileFormat(true, "Raw Spectral Bytes", "Reads spectra as raw bytes in a binary file", "raw");
	}

	@Override
	public ScanData getScanData() {
		return scandata;
	}

	@Override
	public void read(List<DataFile> datafiles) throws IOException, DataSourceReadException, InterruptedException {
		if (datafiles.size() != 1) {
			throw new IllegalArgumentException(getFileFormat().getFormatName() + " requires exactly 1 file");
		}
		DataFile datafile = datafiles.get(0);
		int dataChannels = paramChannels.getValue();
		
		scandata = new PipelineScanData(datafile.getBasename());
		long size = datafile.size().orElse(0l);
		getInteraction().notifyScanCount((int)(size / dataChannels));
		
		BufferedInputStream bis = new BufferedInputStream(datafile.getInputStream());
		
		int index = 0;
		int count = 0;
		byte[] byteArray = new byte[dataChannels];
		while (true) {
			int bytesRead = bis.read(byteArray);
			if (bytesRead != dataChannels) {
				//end of input or something went wrong
				break;
			}
			
			float[] asFloatArray = new float[dataChannels];
			for (int i = 0; i < dataChannels; i++) {
				asFloatArray[i] = byteArray[i];
			}
			
			scandata.submit(index++, new ISpectrum(asFloatArray));

			count++;
			if (count == 100) {
				getInteraction().notifyScanRead(count);
				count = 0;
			}
			
		}
		
		scandata.finish();
		
	}

	@Override
	public String pluginVersion() {
		return "1.1";
	}

	@Override
	public String pluginUUID() {
		return "f634f445-f315-4b83-a725-dd085849d5b6";
	}

}
