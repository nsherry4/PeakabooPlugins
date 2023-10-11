package org.peakaboo.datasource.plugins.GenericJHDF;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.peakaboo.dataset.source.model.DataSourceReadException;
import org.peakaboo.dataset.source.model.components.datasize.DataSize;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormat;
import org.peakaboo.dataset.source.model.components.metadata.Metadata;
import org.peakaboo.dataset.source.model.components.physicalsize.PhysicalSize;
import org.peakaboo.dataset.source.model.components.scandata.PipelineScanData;
import org.peakaboo.dataset.source.model.components.scandata.ScanData;
import org.peakaboo.dataset.source.model.components.scandata.SimpleScanData;
import org.peakaboo.dataset.source.model.components.scandata.loaderqueue.LoaderQueue;
import org.peakaboo.dataset.source.model.datafile.DataFile;
import org.peakaboo.dataset.source.plugin.AbstractDataSource;
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

	private PipelineScanData scandata;
	private DataSize dataSize;

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
	public Optional<Group> getParameters(List<DataFile> paths) {
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
	public void read(List<DataFile> paths) throws IOException, DataSourceReadException {
		scandata = new PipelineScanData(DataFile.getTitle(paths));

		// retrieve the first dataset and use it to calculate the total dataset size
		HdfFile firstFile = new HdfFile(paths.get(0).getAndEnsurePath());
		Dataset firstDataset = firstFile.getDatasetByPath(dataPaths.get(0));
		dataSize = getDataSize(paths, firstDataset);
		getInteraction().notifyScanCount(dataSize.getDataDimensions().x * dataSize.getDataDimensions().y);
		firstFile.close();

		Comparator<String> comparitor = new AlphaNumericComparitor();
		paths.sort((a, b) -> comparitor.compare(a.getFilename(), b.getFilename()));
		int filenum = 0;
		for (DataFile path : paths) {
			readFile(path, filenum++);
			if (getInteraction().checkReadAborted()) {
				scandata.finish();
				return;
			}
		}

		scandata.finish();
	}

	protected final void submitScan(int index, Spectrum scan) throws InterruptedException {
		scandata.submit(index, scan);
		if (index > 0 && index % 50 == 0) {
			getInteraction().notifyScanRead(50);
		}
	}

	@Override
	public ScanData getScanData() {
		return scandata;
	}

	@Override
	public Optional<DataSize> getDataSize() {
		return Optional.of(dataSize);
	}

	
	

	protected abstract void readFile(DataFile path, int filenum) throws IOException, DataSourceReadException;

	protected abstract DataSize getDataSize(List<DataFile> paths, Dataset firstDataset);

}
