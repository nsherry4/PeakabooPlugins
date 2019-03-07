package org.peakaboo.datasource.plugins.GenericHDF5;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.metadata.Metadata;
import org.peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import org.peakaboo.datasource.model.components.scandata.ScanData;
import org.peakaboo.datasource.model.components.scandata.SimpleScanData;
import org.peakaboo.datasource.model.components.scandata.loaderqueue.LoaderQueue;
import org.peakaboo.datasource.plugin.AbstractDataSource;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;
import cyclops.Spectrum;
import net.sciencestudio.autodialog.model.Group;
import net.sciencestudio.bolt.plugin.core.AlphaNumericComparitor;

public abstract class SimpleHDF5DataSource extends AbstractDataSource {

	private SimpleScanData scandata;
	private DataSize dataSize;
	private LoaderQueue queue;
	
	
	private String dataPath, name, description;
	
	public SimpleHDF5DataSource(String dataPath, String name, String description) {
		this.dataPath = dataPath;
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
		return new SimpleHDF5FileFormat(dataPath, name, description);
	}


	@Override
	public void read(List<Path> paths) throws Exception {
		scandata = new SimpleScanData(paths.get(0).getParent().getFileName().toString());
		
		IHDF5SimpleReader reader = HDF5Factory.openForReading(paths.get(0).toFile());
		HDF5DataSetInformation info = reader.getDataSetInformation(dataPath);
		dataSize = getDataSize(paths, info);
		getInteraction().notifyScanCount(dataSize.getDataDimensions().x * dataSize.getDataDimensions().y);


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
	
	protected abstract void readFile(Path path, int filenum) throws Exception;
	protected abstract DataSize getDataSize(List<Path> paths, HDF5DataSetInformation datasetInfo);

}
