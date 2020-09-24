package org.peakaboo.datasource.plugins.GenericHDF5;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.peakaboo.datasource.model.DataSource;
import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.datasize.SimpleDataSize;
import org.peakaboo.framework.cyclops.ISpectrum;
import org.peakaboo.framework.cyclops.Spectrum;
import org.peakaboo.framework.cyclops.SpectrumCalculations;

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
	
	public FloatMatrixHDF5DataSource(String axisOrder, String dataPath, String name, String description) {
		super(dataPath, name, description);
		readAxisOrder(axisOrder);
	}

	public FloatMatrixHDF5DataSource(String axisOrder, String name, String description) {
		super(name, description);
		readAxisOrder(axisOrder);
	}
	
	private void readAxisOrder(String axisOrder) {
		//X and Y represent x and y positions on a raster scan/map
		//Z represents channels in a single scan
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

		IHDF5Reader reader = getReader(path);
		HDF5DataSetInformation info = reader.getDataSetInformation(dataPaths.get(0));
		int channels = (int) info.getDimensions()[zIndex];
		
		
		//prep for iterating over all points in scan
		IHDF5FloatReader floatreader = reader.float32();
		int height = dataSize.getDataDimensions().y;
		int width = dataSize.getDataDimensions().x;
		int[] range;
		if (yIndex >= 0) {
			range = new int[] {-1, -1, -1};
			range[xIndex] = 1;
			range[yIndex] = 1;
			range[zIndex] = channels;
		} else {
			//There is no y axis, only scans (x) and channels (z)
			range = new int[] {-1, -1};
			range[xIndex] = 1;
			range[zIndex] = channels;
		}


		Map<String, Spectrum> livetimes = new HashMap<>();
		for (String dataPath : dataPaths) {
			Spectrum livetime = getDeadtimes(dataPath, reader);
			SpectrumCalculations.subtractListFrom_inplace(livetime, 1, 0);
			livetimes.put(dataPath, livetime);
		}
		
		long[] bounds = yIndex == -1 ? new long[] {0, 0} : new long[] {0, 0, 0};
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					
					int index = (y*width+x);
					Spectrum agg = new ISpectrum(channels);
					for (String dataPath : dataPaths) {
						
						//read scan
						if (yIndex > -1) { bounds[yIndex] = y; }
						bounds[xIndex] = x;
						bounds[zIndex] = 0;
						MDFloatArray mdarray = floatreader.readMDArrayBlock(dataPath, range, bounds);
						//get a temporary spectrum that uses the flatarray result as a backing array
						Spectrum scan = new ISpectrum(mdarray.getAsFlatArray(), true);
						//deadtime correction
						SpectrumCalculations.divideBy_inplace(scan, livetimes.get(dataPath).get(index));
						//add scan to aggregate
						SpectrumCalculations.addLists_inplace(agg, scan);
					}
					super.submitScan(index, agg);
					
				}
			}

		
		readMatrixMetadata(reader, channels);
		
	}
	
	@Override
	protected final DataSize getDataSize(List<Path> paths, HDF5DataSetInformation datasetInfo) {
		Path path = paths.get(0);
		return getDataSize(path, datasetInfo);
	}
	protected DataSize getDataSize(Path path, HDF5DataSetInformation datasetInfo) {
		SimpleDataSize size = new SimpleDataSize();
		size.setDataHeight(yIndex == -1 ? 1 : (int) datasetInfo.getDimensions()[yIndex]);
		size.setDataWidth((int) datasetInfo.getDimensions()[xIndex]);
		return size;
	}
	
	protected static IHDF5Reader getReader(Path path) {
		return HDF5Factory.openForReading(path.toFile());
	}
	
	/**
	 * Override this method as a hook into the tail end of the default readFile method.
	 */
	protected void readMatrixMetadata(IHDF5Reader reader, int channels) {};
	
	protected Spectrum getDeadtimes(String dataPath, IHDF5Reader reader) {
		return new ISpectrum(dataSize.size(), 0f);
	}
}
