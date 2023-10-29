package org.peakaboo.datasource.plugins.sigray2018;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.peakaboo.dataset.source.model.DataSourceReadException;
import org.peakaboo.dataset.source.model.components.datasize.DataSize;
import org.peakaboo.dataset.source.model.components.datasize.SimpleDataSize;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormat;
import org.peakaboo.dataset.source.model.components.metadata.Metadata;
import org.peakaboo.dataset.source.model.components.physicalsize.PhysicalSize;
import org.peakaboo.dataset.source.model.components.scandata.PipelineScanData;
import org.peakaboo.dataset.source.model.components.scandata.ScanData;
import org.peakaboo.dataset.source.model.datafile.DataFile;
import org.peakaboo.dataset.source.plugin.AbstractDataSource;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.bolt.plugin.core.AlphaNumericComparitor;
import org.peakaboo.framework.cyclops.spectrum.ISpectrum;
import org.peakaboo.framework.cyclops.spectrum.Spectrum;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;

public class Sigray2018HDF5 extends AbstractDataSource {

	private PipelineScanData scandata;
	private SimpleDataSize dataSize;


	@Override
	public String pluginVersion() {
		return "1.4";
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
	public void read(List<DataFile> paths) throws IOException, DataSourceReadException, InterruptedException {
		scandata = new PipelineScanData(DataFile.getTitle(paths));
		dataSize = new SimpleDataSize();
		
		IHDF5SimpleReader reader = HDF5Factory.openForReading(paths.get(0).getAndEnsurePath().toFile());
		HDF5DataSetInformation info = reader.getDataSetInformation("/entry/detector/data1");
		long size[] = info.getDimensions();
		int dx = (int) size[0];
		int dy = (int) size[1];
		int dz = (int) size[2];
		dy *= paths.size(); //Individual files seem to only encode individual dimensions
		getInteraction().notifyScanCount(dx * dy);
		
		dataSize.setDataHeight(dy);
		dataSize.setDataWidth(dx);
	
		Comparator<String> comparitor = new AlphaNumericComparitor(); 
		paths.sort((a, b) -> comparitor.compare(a.getFilename(), b.getFilename()));
		int index = 0;
		for (DataFile path : paths) {
			index = readRow(path, index);
			if (getInteraction().checkReadAborted()) {
				scandata.finish();
				return;
			}
		}
		
		scandata.finish();
	}

	// Reads a file (which probably contains a single row of spectra). Scans are
	// submitted with a 1D index which is passed in, incremented as needed, and
	// returned.
	private int readRow(DataFile path, int index) throws DataSourceReadException, IOException, InterruptedException {
		IHDF5SimpleReader reader = HDF5Factory.openForReading(path.getAndEnsurePath().toFile());
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
				scandata.submit(index++, s);

			}
			getInteraction().notifyScanRead(dx);
		}

		
		data1 = null;
		System.gc();
		
		return index;
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
	public Optional<Group> getParameters(List<DataFile> paths) {
		return Optional.empty();
	}

	
}
