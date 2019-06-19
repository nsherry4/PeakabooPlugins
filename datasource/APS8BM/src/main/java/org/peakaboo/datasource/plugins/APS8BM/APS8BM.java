package org.peakaboo.datasource.plugins.APS8BM;

import java.util.Arrays;
import java.util.Optional;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import org.peakaboo.datasource.model.components.physicalsize.SimplePhysicalSize;
import org.peakaboo.datasource.plugins.GenericHDF5.FloatMatrixHDF5DataSource;
import org.peakaboo.datasource.plugins.GenericHDF5.SimpleHDF5FileFormat;
import org.peakaboo.framework.cyclops.Coord;
import org.peakaboo.framework.cyclops.SISize;

import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class APS8BM extends FloatMatrixHDF5DataSource {

	private static final String PATH = "/MAPS/Spectra/mca_arr";
	private static final String NAME = "APS-8BM Format";
	private static final String DESC = "APS-8BM XRF spectral data  stored in an HDF5 container";
	private SimplePhysicalSize physical;
	
	public APS8BM() {
		super("zyx", PATH, NAME, DESC);
	}

	@Override
	public FileFormat getFileFormat() {
		return new SimpleHDF5FileFormat(Arrays.asList(PATH, "/MAPS/Spectra/Energy", "/MAPS/Scan/x_axis"), NAME, DESC);
	}

	@Override
	public String pluginVersion() {
		return "1.0";
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
	protected void readMatrixMetadata(IHDF5Reader reader, int channels) {
		
		DataSize size = getDataSize().get();
		int height = size.getDataDimensions().y;
		int width = size.getDataDimensions().x;
		
		physical = new SimplePhysicalSize(SISize.um);
		double[] xpos = reader.readDoubleArray("/MAPS/Scan/x_axis");
		double[] ypos = reader.readDoubleArray("/MAPS/Scan/y_axis");
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				//read physical position
				int index = (y*width+x);
				physical.putPoint(index, new Coord<>(xpos[x], ypos[y]));
				
			}
		}
		
		float[] energies = reader.readFloatArray("/MAPS/Spectra/Energy");
		getScanData().setMaxEnergy(energies[channels-1]);
		getScanData().setMinEnergy(energies[0]);
		
	}


}
