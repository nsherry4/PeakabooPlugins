package org.peakaboo.datasource.plugins.APS8BM;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.datasize.SimpleDataSize;
import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import org.peakaboo.datasource.model.components.physicalsize.SimplePhysicalSize;
import org.peakaboo.datasource.plugins.GenericHDF5.SimpleHDF5DataSource;
import org.peakaboo.datasource.plugins.GenericHDF5.SimpleHDF5FileFormat;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5FloatReader;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import cyclops.Coord;
import cyclops.ISpectrum;
import cyclops.SISize;
import net.sciencestudio.autodialog.model.Group;

public class APS8BM extends SimpleHDF5DataSource {

	private static final String PATH = "/MAPS/Spectra/mca_arr";
	private static final String NAME = "APS-8BM Format";
	private static final String DESC = "APS-8BM XRF spectral data  stored in an HDF5 container";
	private SimplePhysicalSize physical;
	
	public APS8BM() {
		super(PATH, NAME, DESC);
	}
	
	@Override
	public Optional<Group> getParameters(List<Path> paths) {
		return Optional.empty();
	}

	@Override
	public FileFormat getFileFormat() {
		return new SimpleHDF5FileFormat(Arrays.asList(PATH, "/MAPS/Spectra/Energy", "/MAPS/Scan/x_axis"), NAME, DESC);
	}

	@Override
	public String pluginVersion() {
		return "0.1";
	}

	@Override
	public String pluginUUID() {
		return "ae3469bc-a0b6-4329-ba37-08895fc28c50";
	}
	
	@Override
	public Optional<PhysicalSize> getPhysicalSize() {
		return Optional.of(physical);
	}

	@Override
	protected void readFile(Path path, int filenum) throws Exception {
		if (filenum > 0) {
			throw new IllegalArgumentException(getFileFormat().getFormatName() + " requires exactly 1 file");
		}
		
		
		
		IHDF5Reader reader = HDF5Factory.openForReading(path.toFile());
		HDF5DataSetInformation info = reader.getDataSetInformation(PATH);
		int channels = (int) info.getDimensions()[0];
		
		float[] energies = reader.readFloatArray("/MAPS/Spectra/Energy");
		getScanData().setMaxEnergy(energies[channels-1]);
		getScanData().setMinEnergy(energies[0]);
		

		
		//prep for iterating over all points in scan
		IHDF5FloatReader floatreader = reader.float32();
		DataSize size = getDataSize().get();
		int height = size.getDataDimensions().y;
		int width = size.getDataDimensions().x;
		int[] range = new int[] {channels, 1, 1};
		
		//prep for reading physical size
		physical = new SimplePhysicalSize(SISize.um);
		double[] xpos = reader.readDoubleArray("/MAPS/Scan/x_axis");
		double[] ypos = reader.readDoubleArray("/MAPS/Scan/y_axis");
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				//read scan
				int index = (y*width+x);
				MDFloatArray mdarray = floatreader.readMDArrayBlock(PATH, range, new long[] {0, x, y});
				super.submitScan(index, new ISpectrum(mdarray.getAsFlatArray(), false));
				
				//read physical position
				physical.putPoint(index, new Coord<>(xpos[x], ypos[y]));
				
			}
		}
	}

	@Override
	protected DataSize getDataSize(List<Path> paths, HDF5DataSetInformation datasetInfo) {
		SimpleDataSize size = new SimpleDataSize();
		size.setDataHeight((int) datasetInfo.getDimensions()[1]);
		size.setDataWidth((int) datasetInfo.getDimensions()[2]);
		return size;
	}

}
