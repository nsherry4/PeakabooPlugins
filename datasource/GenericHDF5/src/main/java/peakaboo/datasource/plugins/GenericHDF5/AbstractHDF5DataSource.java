package peakaboo.datasource.plugins.GenericHDF5;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;
import commonenvironment.AlphaNumericComparitor;
import peakaboo.datasource.model.components.datasize.DataSize;
import peakaboo.datasource.model.components.scandata.ScanData;
import peakaboo.datasource.model.components.scandata.SimpleScanData;
import peakaboo.datasource.model.components.scandata.loaderqueue.LoaderQueue;
import peakaboo.datasource.plugin.AbstractDataSource;
import scitypes.ISpectrum;
import scitypes.Spectrum;

public abstract class AbstractHDF5DataSource extends AbstractDataSource {

	private SimpleScanData scandata;
	private DataSize dataSize;
	private int spectrumSize;

	private String dataPath;
	
	public AbstractHDF5DataSource(String dataPath) {
		this.dataPath = dataPath;
	}

	@Override
	public String pluginVersion() {
		return "1.0";
	}


	
	/**
	 * We impose the following restrictions:<br/>
	 * <ul>
	 * <li>That a scan be contained in a single file, not split across several.</li>
	 * <li>That each file containt the same number of scans.</li>
	 * </ul> 
	 */
	@Override
	public void read(List<Path> paths) throws Exception {
		scandata = new SimpleScanData(paths.get(0).getParent().getFileName().toString());
		
		IHDF5SimpleReader reader = HDF5Factory.openForReading(paths.get(0).toFile());
		HDF5DataSetInformation info = reader.getDataSetInformation(dataPath);
		dataSize = calcDataSize(paths, info);
		spectrumSize = calcScanWidth(info);
		getInteraction().notifyScanCount(dataSize.getDataDimensions().x * dataSize.getDataDimensions().y);


		LoaderQueue queue = scandata.createLoaderQueue(1000);
		
		
		Comparator<String> comparitor = new AlphaNumericComparitor(); 
		paths.sort((a, b) -> comparitor.compare(a.getFileName().toString(), b.getFileName().toString()));
		int filenum = 0;
		for (Path path : paths) {
			readFile(path, queue, filenum++);
			if (getInteraction().checkReadAborted()) {
				queue.finish();
				return;
			}
		}
		
		queue.finish();
	}

	private void readFile(Path path, LoaderQueue queue, int filenum) throws InterruptedException {
		
		//Read the contents of the file as a float array
		HDF5DataSetInformation info = readDatasetInfo(path);
		float[] data = readSpectralData(path);
		

		
		/*
		 * We don't know what format the data is stored in, but we are sure 
		 * that any spectrum contained aren't already in our dataset. That means
		 */
		Map<Integer, Spectrum> scans = new HashMap<>();
		
		
		for (int i = 0; i < data.length; i++) {
			int scan = scanAtIndex(filenum, i, info);
			int channel = channelAtIndex(filenum, i, info);
			
			if (!scans.containsKey(scan)) {
				scans.put(scan, new ISpectrum(spectrumSize));
			}
			Spectrum spectrum = scans.get(scan);
			spectrum.set(channel, data[i]);
			
			if (i % spectrumSize == 0) {
				getInteraction().notifyScanRead(1);	
			}
			
		}
		
		for (int scan : scans.keySet()) {
			queue.submit(scan, scans.get(scan));
		}

		System.gc();
	}
	
	protected abstract DataSize calcDataSize(List<Path> paths, HDF5DataSetInformation datasetInfo);
	protected abstract int calcScanWidth(HDF5DataSetInformation datasetInfo);
	protected abstract int scanAtIndex(int file, int index, HDF5DataSetInformation datasetInfo);
	protected abstract int channelAtIndex(int file, int index, HDF5DataSetInformation datasetInfo);
	

	
	protected float[] readSpectralData(Path path) {
		IHDF5SimpleReader reader = HDF5Factory.openForReading(path.toFile());	
		float[] data = reader.readFloatArray(dataPath);
		reader.close();
		return data;
	}
	
	protected HDF5DataSetInformation readDatasetInfo(Path path) {
		IHDF5SimpleReader reader = HDF5Factory.openForReading(path.toFile());	
		HDF5DataSetInformation info = reader.getDataSetInformation(dataPath);
		reader.close();
		return info;
	}


	@Override
	public ScanData getScanData() {
		return scandata;
	}

	@Override
	public Optional<DataSize> getDataSize() {
		return Optional.of(dataSize);
	}
	
}

