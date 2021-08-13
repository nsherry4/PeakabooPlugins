package org.peakaboo.datasource.plugins.GenericJHDF;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.bolt.plugin.core.AlphaNumericComparitor;
import org.peakaboo.framework.cyclops.spectrum.Spectrum;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;

/*
 * THIS CODE IS NOT PRODUCTION-READY, SINCE THE jHDF LIBRARY CURRENTLY
 * HAS NO WAY TO READ SLICES OF AN HDF FILE. WITHOUT THIS, AN ENTIRE
 * MATRIX MUST BE LOADED INTO MEMORY ALL AT ONCE AND THEN READ FROM
 * 
 * READING SLICES IS ON THE DRAWING BOARD FOR THIS LIBRARY, BUT NOT YET
 * IMPLEMENTED.
 * https://github.com/jamesmudd/jhdf/issues/52
 */

public abstract class AbstractJHDFDataSource extends AbstractDataSource {

	private SimpleScanData scandata;
	private DataSize dataSize;
	private LoaderQueue queue;

	private String name, description;
	protected List<String> dataPaths;

	public AbstractJHDFDataSource(String dataPath, String name, String description) {
		this(Collections.singletonList(dataPath), name, description);
	}

	public AbstractJHDFDataSource(List<String> dataPaths, String name, String description) {
		this.dataPaths = dataPaths;
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
		return new SimpleJHDFFileFormat(dataPaths, name, description);
	}

	@Override
	public void read(List<Path> paths) throws Exception {
		scandata = new SimpleScanData(paths.get(0).getParent().getFileName().toString());

		// retrieve the first dataset and use it to calculate the total dataset size
		HdfFile firstFile = new HdfFile(paths.get(0));
		Dataset firstDataset = firstFile.getDatasetByPath(dataPaths.get(0));
		dataSize = getDataSize(paths, firstDataset);
		getInteraction().notifyScanCount(dataSize.getDataDimensions().x * dataSize.getDataDimensions().y);
		firstFile.close();

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

	protected abstract DataSize getDataSize(List<Path> paths, Dataset firstDataset);

}
