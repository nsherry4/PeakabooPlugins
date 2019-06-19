package org.peakaboo.datasource.plugins.sigray;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.datasize.SimpleDataSize;
import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.fileformat.SimpleFileFormat;
import org.peakaboo.datasource.model.components.metadata.Metadata;
import org.peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import org.peakaboo.datasource.model.components.scandata.ScanData;
import org.peakaboo.datasource.model.components.scandata.SimpleScanData;
import org.peakaboo.datasource.plugin.AbstractDataSource;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;
import org.peakaboo.framework.cyclops.Bounds;
import org.peakaboo.framework.cyclops.Coord;
import org.peakaboo.framework.cyclops.ISpectrum;
import org.peakaboo.framework.cyclops.SISize;
import org.peakaboo.framework.cyclops.Spectrum;
import org.peakaboo.framework.autodialog.model.Group;

public class SigrayHDF5 extends AbstractDataSource {

	protected SimpleScanData scandata;
	protected SimpleDataSize dataSize;
	protected SigrayHDF5Dimensions physicalSize;


	@Override
	public String pluginVersion() {
		return "1.3";
	}
	
	@Override
	public String pluginUUID() {
		return "1d47d45a-7027-4758-a9ce-906cd2f4aa29";
	}


	private int index3(int x, int y, int z, int dx, int dy) {
		return x + y * dx + z * dx * dy;
	}

	// private Integer file_id = null;


	
	public void read(Path path) throws Exception {

		scandata = new SimpleScanData(path.getFileName().toString());

		
		
		IHDF5SimpleReader reader = HDF5Factory.openForReading(path.toFile());

		HDF5DataSetInformation info = reader.getDataSetInformation("/MAPS/mca_arr");
		long size[] = info.getDimensions();
		int dz = (int) size[0];
		int dy = (int) size[1];
		int dx = (int) size[2];

		physicalSize = new SigrayHDF5Dimensions(this);
		dataSize = new SimpleDataSize();
		dataSize.setDataWidth(dx);
		dataSize.setDataHeight(dy);
		
		
		getInteraction().notifyScanCount(dx * dy);
		
		float[] mca_arr = reader.readFloatArray("/MAPS/mca_arr");
		float[] scalers = reader.readFloatArray("/MAPS/scalers");

		// real scan dimensions;
		physicalSize.coords = new Coord[dx][dy];
		for (int y = 0; y < dy; y++) { // y-axis
			for (int x = 0; x < dx; x++) { // x-axis

				int x_index = index3(x, y, 17, dx, dy);
				int y_index = index3(x, y, 18, dx, dy);

				Coord<Number> coord = new Coord<>(scalers[x_index], scalers[y_index]);
				physicalSize.coords[x][y] = coord;
			}
		}
		// TODO: no such method exception in jni?
		physicalSize.units = SISize.mm;
		// SISize.valueOf(reader.readStringArray("/MAPS/scalar_units")[17]);

		// max energy
		float[] energy = reader.readFloatArray("/MAPS/energy");
		scandata.setMaxEnergy(energy[dz - 1]);

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

				int scan_index = (x + y * dx);
				Spectrum s = new ISpectrum(dz);

				for (int z = 0; z < dz; z++) { // (z-axis, channelCount)

					int mca_index = index3(x, y, z, dx, dy);

					s.set((int) z, mca_arr[mca_index]);
				}
				scandata.set(scan_index, s);
			}
			getInteraction().notifyScanRead(dx);
		}

		reader.close();
		System.gc();

	}

	@Override
	public void read(List<Path> files) throws Exception {
		read(files.get(0));
	}



	@Override
	public Optional<Metadata> getMetadata() {
		return Optional.empty();
	}

	@Override
	public Optional<DataSize> getDataSize() {
		return Optional.of(dataSize);
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
	public Optional<PhysicalSize> getPhysicalSize() {
		return Optional.of(physicalSize);
	}
	
	@Override
	public Optional<Group> getParameters(List<Path> paths) {
		return Optional.empty();
	}


}

class SigrayHDF5Dimensions implements PhysicalSize {

	protected Coord<Number> coords[][];
	protected SISize units;
	private SigrayHDF5 datasource;

	public SigrayHDF5Dimensions(SigrayHDF5 scan) {
		datasource = scan;
	}

	@Override
	public Coord<Number> getPhysicalCoordinatesAtIndex(int index) throws IndexOutOfBoundsException {
		Coord<Integer> xy = datasource.dataSize.getDataCoordinatesAtIndex(index);
		return coords[xy.x][xy.y];
	}

	@Override
	public Coord<Bounds<Number>> getPhysicalDimensions() {

		Number x1 = getPhysicalCoordinatesAtIndex(0).x;
		Number y1 = getPhysicalCoordinatesAtIndex(0).y;
		Number x2 = getPhysicalCoordinatesAtIndex(datasource.scandata.scanCount() - 1).x;
		Number y2 = getPhysicalCoordinatesAtIndex(datasource.scandata.scanCount() - 1).y;

		Bounds<Number> xDim = new Bounds<Number>(x1, x2);
		Bounds<Number> yDim = new Bounds<Number>(y1, y2);
		return new Coord<Bounds<Number>>(xDim, yDim);

	}

	@Override
	public SISize getPhysicalUnit() {
		return units;
	}




}
