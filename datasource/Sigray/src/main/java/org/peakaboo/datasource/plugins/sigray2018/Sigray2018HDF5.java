package org.peakaboo.datasource.plugins.sigray2018;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.datasize.SimpleDataSize;
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
import org.peakaboo.framework.cyclops.ISpectrum;
import org.peakaboo.framework.cyclops.Spectrum;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.bolt.plugin.core.AlphaNumericComparitor;

public class Sigray2018HDF5 extends AbstractDataSource {

	private SimpleScanData scandata;
	private SimpleDataSize dataSize;


	@Override
	public String pluginVersion() {
		return "1.3";
	}
	
	@Override
	public String pluginUUID() {
		return "0196935e-2902-4a89-8c06-772db437d59a";
	}

	private int index3(int x, int y, int z, int dx, int dy, int dz) {
		//return x + y * dx + z * dx * dy;
		return z + y*dz + x*dz*dy;
	}


	@Override
	public void read(List<Path> paths) throws Exception {
		String title = "";
		if (paths.size() == 1) {
			title = paths.get(0).getFileName().toString();
		} else {
			title = paths.get(0).getParent().getFileName().toString();
		}
		scandata = new SimpleScanData(title);
		dataSize = new SimpleDataSize();
		
		IHDF5SimpleReader reader = HDF5Factory.openForReading(paths.get(0).toFile());
		HDF5DataSetInformation info = reader.getDataSetInformation("/entry/detector/data1");
		long size[] = info.getDimensions();
		int dx = (int) size[0];
		int dy = (int) size[1];
		int dz = (int) size[2];
		getInteraction().notifyScanCount(dx * dy * paths.size());
		
		dataSize.setDataHeight(paths.size());
		dataSize.setDataWidth(dx);

		LoaderQueue queue = scandata.createLoaderQueue(dz*2);
		
		Comparator<String> comparitor = new AlphaNumericComparitor(); 
		paths.sort((a, b) -> comparitor.compare(a.getFileName().toString(), b.getFileName().toString()));
		for (Path path : paths) {
			readRow(path, queue);
			if (getInteraction().checkReadAborted()) {
				queue.finish();
				return;
			}
		}
		
		queue.finish();
	}

	private void readRow(Path path, LoaderQueue queue) throws InterruptedException {
		IHDF5SimpleReader reader = HDF5Factory.openForReading(path.toFile());
		HDF5DataSetInformation info = reader.getDataSetInformation("/entry/detector/data1");
		long size[] = info.getDimensions();
		int dx = (int) size[0];
		int dy = (int) size[1];
		int dz = (int) size[2];
		
		float[] data1 = reader.readFloatArray("/entry/detector/data1");
		reader.close();

		
		for (int y = 0; y < dy; y++) { // y-axis
			Spectrum[] spectra = new Spectrum[dx];
			for (int x = 0; x < dx; x++) { // x-axis
				Spectrum s = new ISpectrum(Arrays.copyOfRange(data1, index3(x, y, 0, dx, dy, dz), index3(x, y, dz, dx, dy, dz)), false);
				queue.submit(s);
			}
			getInteraction().notifyScanRead(dx);
		}

		
		data1 = null;
		System.gc();
	}


	@Override
	public FileFormat getFileFormat() {
		return new Sigray2018HDF5FileFormat();
	}

	@Override
	public ScanData getScanData() {
		return scandata;
	}

	@Override
	public Optional<PhysicalSize> getPhysicalSize() {
		return Optional.empty();
	}

	@Override
	public Optional<DataSize> getDataSize() {
		return Optional.of(dataSize);
	}
	
	
	@Override
	public Optional<Metadata> getMetadata() {
		return Optional.empty();
	}
	
	@Override
	public Optional<Group> getParameters(List<Path> paths) {
		return Optional.empty();
	}

	
}
