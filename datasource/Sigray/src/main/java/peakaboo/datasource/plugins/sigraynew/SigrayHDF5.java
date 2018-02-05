package peakaboo.datasource.plugins.sigraynew;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;
import commonenvironment.AlphaNumericComparitor;
import peakaboo.datasource.model.AbstractDataSource;
import peakaboo.datasource.model.components.datasize.DataSize;
import peakaboo.datasource.model.components.datasize.SimpleDataSize;
import peakaboo.datasource.model.components.fileformat.FileFormat;
import peakaboo.datasource.model.components.metadata.Metadata;
import peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import peakaboo.datasource.model.components.scandata.ScanData;
import peakaboo.datasource.model.components.scandata.SimpleScanData;
import scitypes.ISpectrum;
import scitypes.Spectrum;

public class SigrayHDF5 extends AbstractDataSource {

	private SimpleScanData scandata;
	private SimpleDataSize dataSize;


	@Override
	public String pluginVersion() {
		return "1.0";
	}
	


	private int index3(int x, int y, int z, int dx, int dy, int dz) {
		//return x + y * dx + z * dx * dy;
		return z + y*dz + x*dz*dy;
	}

	@Override
	public void read(File file) throws Exception {
		read(Collections.singletonList(file));
	}

	@Override
	public void read(List<File> files) throws Exception {
		scandata = new SimpleScanData(files.get(0).getParentFile().getName());
		dataSize = new SimpleDataSize();
		
		IHDF5SimpleReader reader = HDF5Factory.openForReading(files.get(0));
		HDF5DataSetInformation info = reader.getDataSetInformation("/entry/detector/data1");
		long size[] = info.getDimensions();
		int dx = (int) size[0];
		int dy = (int) size[1];
		int dz = (int) size[2];
		getInteraction().notifyScanCount(dx * dy * files.size());
		
		dataSize.setDataHeight(files.size());
		dataSize.setDataWidth(dx);
		
		Comparator<String> comparitor = new AlphaNumericComparitor(); 
		files.sort((a, b) -> comparitor.compare(a.getName(), b.getName()));
		for (File file : files) {
			readRow(file);
			if (getInteraction().checkReadAborted()) {
				return;
			}
		}
		
	}

	private void readRow(File file) {
		IHDF5SimpleReader reader = HDF5Factory.openForReading(file);
		HDF5DataSetInformation info = reader.getDataSetInformation("/entry/detector/data1");
		long size[] = info.getDimensions();
		int dx = (int) size[0];
		int dy = (int) size[1];
		int dz = (int) size[2];
		
				
		float[] data1 = reader.readFloatArray("/entry/detector/data1");



		/*
		 * data is stored im mca_arr in x, y, z order, but we're going through
		 * one spectrum at a time for speed. Because we don't want to store
		 * everything in memory, we're using a special kind of list which writes
		 * everything to disk. This means that if we `get` a spectrum from the
		 * list and write to it, this won't be reflected in the list of spectra,
		 * we have to explicitly write it back. Therefore, we do all the
		 * modifications to a spectrum at once, even though it probably means
		 * more cache misses in the source array.
		 */
		for (int y = 0; y < dy; y++) { // y-axis
			for (int x = 0; x < dx; x++) { // x-axis

				Spectrum s = new ISpectrum(Arrays.copyOfRange(data1, index3(x, y, 0, dx, dy, dz), index3(x, y, dz, dx, dy, dz)));
//				for (int z = 0; z < dz; z++) { // (z-axis, channels)
//
//					int mca_index = index3(x, y, z, dx, dy, dz);
//
//					s.set((int) z, data1[mca_index]);
//				}
				scandata.add(s);
			}
			getInteraction().notifyScanRead(dx);
		}

		reader.close();
		System.gc();
	}


	@Override
	public FileFormat getFileFormat() {
		return new SigrayHDF5FileFormat();
	}

	@Override
	public ScanData getScanData() {
		return scandata;
	}

	@Override
	public PhysicalSize getPhysicalSize() {
		return null;
	}

	@Override
	public DataSize getDataSize() {
		return dataSize;
	}
	
	@Override
	public Metadata getMetadata() {
		return null;
	}
//	
//	public static void main(String[] args) throws ClassInheritanceException, ClassInstantiationException {
//		DataSourceLoader.registerPlugin(SigrayHDF5.class);
//		Peakaboo.main(args);
//	}

}
