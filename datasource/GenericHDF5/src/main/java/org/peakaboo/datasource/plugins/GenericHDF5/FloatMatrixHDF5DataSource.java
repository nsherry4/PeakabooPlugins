package org.peakaboo.datasource.plugins.GenericHDF5;

import java.nio.file.Path;
import java.util.List;

import org.peakaboo.datasource.model.DataSource;
import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.datasize.SimpleDataSize;
import org.peakaboo.framework.cyclops.ISpectrum;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5FloatReader;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

/**
 * This abstract HDF5 {@link DataSource} is designed to make reading single-file
 * HDF5 data easier. Data stored in a single HDF5 file can be accessed by
 * specifying the path to the data and the axis ordering (e.g. "xyz").
 */
public abstract class FloatMatrixHDF5DataSource extends SimpleHDF5DataSource {

	private int xIndex = -1;
	private int yIndex = -1;
	private int zIndex = -1;
	
	private String dataPath;
	
	public FloatMatrixHDF5DataSource(String axisOrder, String dataPath, String name, String description) {
		super(dataPath, name, description);
		this.dataPath = dataPath;
		
		axisOrder = axisOrder.toLowerCase();
		xIndex = axisOrder.indexOf("x");
		yIndex = axisOrder.indexOf("y");
		zIndex = axisOrder.indexOf("z");
		
	}

	@Override
	protected void readFile(Path path, int filenum) throws Exception {
		if (filenum > 0) {
			throw new IllegalArgumentException(getFileFormat().getFormatName() + " requires exactly 1 file");
		}

		IHDF5Reader reader = HDF5Factory.openForReading(path.toFile());
		HDF5DataSetInformation info = reader.getDataSetInformation(dataPath);
		int channels = (int) info.getDimensions()[zIndex];
		
		
		//prep for iterating over all points in scan
		IHDF5FloatReader floatreader = reader.float32();
		DataSize size = getDataSize().get();
		int height = size.getDataDimensions().y;
		int width = size.getDataDimensions().x;
		int[] range = new int[] {-1, -1, -1};
		range[xIndex] = 1;
		range[yIndex] = 1;
		range[zIndex] = channels;
		

		long[] bounds = new long[] {0, 0, 0};
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				//read scan
				int index = (y*width+x);
				
				bounds[yIndex] = y;
				bounds[xIndex] = x;
				bounds[zIndex] = 0;
				
				MDFloatArray mdarray = floatreader.readMDArrayBlock(dataPath, range, bounds);
				super.submitScan(index, new ISpectrum(mdarray.getAsFlatArray(), false));
			}
		}
		
		readMatrixMetadata(reader, channels);
		
	}

	@Override
	protected DataSize getDataSize(List<Path> paths, HDF5DataSetInformation datasetInfo) {
		SimpleDataSize size = new SimpleDataSize();
		size.setDataHeight((int) datasetInfo.getDimensions()[yIndex]);
		size.setDataWidth((int) datasetInfo.getDimensions()[xIndex]);
		return size;
	}
	
	/**
	 * Override this method as a hook into the tail end of the default readFile method.
	 */
	protected void readMatrixMetadata(IHDF5Reader reader, int channels) {};
}
