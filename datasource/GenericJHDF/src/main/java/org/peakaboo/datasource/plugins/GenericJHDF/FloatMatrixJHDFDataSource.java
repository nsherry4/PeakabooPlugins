package org.peakaboo.datasource.plugins.GenericJHDF;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.datasize.SimpleDataSize;
import org.peakaboo.datasource.model.datafile.DataFile;
import org.peakaboo.framework.cyclops.spectrum.ISpectrum;
import org.peakaboo.framework.cyclops.spectrum.Spectrum;
import org.peakaboo.framework.cyclops.spectrum.SpectrumCalculations;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;

public abstract class FloatMatrixJHDFDataSource extends AbstractJHDFDataSource {


	private int xIndex = -1;
	private int yIndex = -1;
	private int zIndex = -1;
	
	public FloatMatrixJHDFDataSource(String axisOrder, String dataPath, String name, String description) {
		super(dataPath, name, description);
		readAxisOrder(axisOrder);
	}
	
	/**
	 * More than one dataPath here means more than one detector, the values of which
	 * will be summed.
	 */
	public FloatMatrixJHDFDataSource(String axisOrder, List<String> dataPaths, String name, String description) {
		super(dataPaths, name, description);
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
	protected void readFile(DataFile path, int filenum) throws IOException, DataSourceReadException {
		if (filenum > 0) {
			throw new IllegalArgumentException(getFileFormat().getFormatName() + " requires exactly 1 file");
		}

		try (HdfFile hdf = new HdfFile(path.getAndEnsurePath())) {
				
			
			Dataset firstDataset = hdf.getDatasetByPath(dataPaths.get(0));
			int channels = (int) firstDataset.getDimensions()[zIndex];
			
			
			//prep for iterating over all points in scan
	//		IHDF5FloatReader floatreader = reader.float32();
			DataSize size = getDataSize().get();
			int height = size.getDataDimensions().y;
			int width = size.getDataDimensions().x;
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
			
			//Calls to Dataset#getData take FOREVER
			//so we load & cache them here.
			//NOTE: this is all TERRIBLE for memory use
			Map<String, Object> pathdata = new HashMap<>();
			for (String dataPath : dataPaths) {
				pathdata.put(dataPath, hdf.getDatasetByPath(dataPath).getData());
			}
			
			int[] bounds = yIndex == -1 ? new int[] {0, 0} : new int[] {0, 0, 0};
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					
					int index = (y*width+x);
					Spectrum agg = new ISpectrum(channels);
					for (String dataPath : dataPaths) {
						
						Dataset ds = hdf.getDatasetByPath(dataPath);
						Object data = pathdata.get(dataPath);
						//read scan
						if (yIndex > -1) { bounds[yIndex] = y; }
						bounds[xIndex] = x;
						bounds[zIndex] = 0;
						Spectrum scan = new ISpectrum(HdfUtil.readFloatArray(ds, data, zIndex, bounds), true);
						SpectrumCalculations.addLists_inplace(agg, scan);
						
					}
					try {
						super.submitScan(index, agg);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new DataSourceReadException("Interrupted", e);
					}
					
				}
			}

			
			readMatrixMetadata(hdf, channels);
		
		}
		
	}

	@Override
	protected DataSize getDataSize(List<DataFile> paths, Dataset firstDataset) {
		SimpleDataSize size = new SimpleDataSize();
		size.setDataHeight(yIndex == -1 ? 1 : (int) firstDataset.getDimensions()[yIndex]);
		size.setDataWidth((int) firstDataset.getDimensions()[xIndex]);
		return size;
	}
	
	
	/**
	 * Override this method as a hook into the tail end of the default readFile method.
	 */
	protected void readMatrixMetadata(HdfFile reader, int channels) {};
}
