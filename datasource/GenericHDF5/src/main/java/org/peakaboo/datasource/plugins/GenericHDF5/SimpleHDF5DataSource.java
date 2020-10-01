package org.peakaboo.datasource.plugins.GenericHDF5;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.metadata.Metadata;
import org.peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import org.peakaboo.datasource.model.components.scandata.SimpleScanData;
import org.peakaboo.datasource.model.components.scandata.loaderqueue.LoaderQueue;
import org.peakaboo.datasource.plugin.AbstractDataSource;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;
import org.peakaboo.framework.cyclops.Spectrum;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.bolt.plugin.core.AlphaNumericComparitor;

public abstract class SimpleHDF5DataSource extends AbstractDataSource {

	private SimpleScanData scandata;
	protected DataSize dataSize;
	private LoaderQueue queue;
	
	
	private String name, description;
	/**
	 * This is the official set of data paths as determined by the
	 * dataPaths or dataPathsFunction provided. It will become available
	 * (ie non-null) before calls to readFile. 
	 */
	protected List<String> dataPaths;
	/**
	 * If the dataPathPreset is provided with the constructor, the default
	 * implementation of getDataPaths will return it
	 */
	private String dataPathPreset = null;
	
	public SimpleHDF5DataSource(String dataPath, String name, String description) {
		this(name, description);
		this.dataPathPreset = dataPath;
	}
	
	public SimpleHDF5DataSource(String name, String description) {
		this.name = name;
		this.description = description;
	}
	
	@Override
	public Optional<Group> getParameters(List<Path> paths) {
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
	public FileFormat getFileFormat() {
		return new SimpleHDF5FileFormat(this::getDataPaths, name, description);
	}


	@Override
	public void read(List<Path> paths) throws Exception {
		String datasetName = getDatasetTitle(paths);
		scandata = new SimpleScanData(datasetName);
		
		Path firstPath = paths.get(0);
		IHDF5SimpleReader reader = HDF5Factory.openForReading(firstPath.toFile());
		dataPaths = getDataPaths(paths);
		HDF5DataSetInformation info = reader.getDataSetInformation(dataPaths.get(0));
		dataSize = getDataSize(paths, info);
		getInteraction().notifyScanCount(dataSize.size());


		queue = scandata.createLoaderQueue(1000);
		
		
		Comparator<String> comparitor = new AlphaNumericComparitor(); 
		paths.sort((a, b) -> comparitor.compare(a.getFileName().toString(), b.getFileName().toString()));
		int filenum = 0;
		for (Path path : paths) {
			readFile(path, filenum++);
			if (getInteraction().checkReadAborted()) {
				queue.finish();
				return;
			}
		}
		
		queue.finish();
	}
	
	protected final void submitScan(int index, Spectrum scan) throws InterruptedException {
		queue.submit(index, scan);
		if (index > 0 && index % 50 == 0) {
			getInteraction().notifyScanRead(50);
		}
	}
	
	@Override
	public SimpleScanData getScanData() {
		return scandata;
	}
	
	@Override
	public Optional<DataSize> getDataSize() {
		return Optional.of(dataSize);
	}
	
	protected String getDatasetTitle(List<Path> paths) {
		String title = paths.get(0).getParent().getFileName().toString();
		if (paths.size() == 1) {
			title = paths.get(0).getFileName().toString();
		}
		return title;
	}
	
	protected abstract void readFile(Path path, int filenum) throws Exception;
	protected abstract DataSize getDataSize(List<Path> paths, HDF5DataSetInformation datasetInfo);
	
	//TODO: make this mandatory (abstract) -- no default implementation
	protected List<String> getDataPaths(List<Path> paths) {
		if (dataPathPreset != null) {
			return Collections.singletonList(dataPathPreset);
		}
		throw new IllegalStateException("Data path not provided with constructor and getDataPaths not overridden");
	}

}
