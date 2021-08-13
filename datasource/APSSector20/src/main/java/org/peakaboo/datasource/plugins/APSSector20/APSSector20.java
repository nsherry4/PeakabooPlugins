package org.peakaboo.datasource.plugins.APSSector20;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.peakaboo.datasource.model.components.datasize.DataSize;
import org.peakaboo.datasource.model.components.datasize.SimpleDataSize;
import org.peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import org.peakaboo.datasource.model.components.physicalsize.SimplePhysicalSize;
import org.peakaboo.datasource.plugins.GenericHDF5.SimpleHDF5DataSource;
import org.peakaboo.framework.cyclops.Coord;
import org.peakaboo.framework.cyclops.SISize;
import org.peakaboo.framework.cyclops.spectrum.ISpectrum;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5MDDataBlock;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;

public class APSSector20 extends SimpleHDF5DataSource {

	private final static String DATA_PATH = "/2D Scan/MCA 1";
	private SimplePhysicalSize physical;
	
	private HDF5DataSetInformation info;
	private List<Iterator<HDF5MDDataBlock<MDFloatArray>>> blockLists;
	private int rowCount, columnCount, scanSize; 
		
	public APSSector20() {
		super(DATA_PATH, "APS Sector 20", "HDF5 Data from Sector 20 of the APS Synchrotron");
	}

	@Override
	public String pluginVersion() {
		return "1.3";
	}
	
	@Override
	public String pluginUUID() {
		return "40cb5bfc-6ee3-4972-928f-f5bafd093e91";
	}
	
	@Override
	public void read(List<Path> paths) throws Exception {
		if (paths.size() != 1) {
			throw new UnsupportedOperationException();
		}
		super.read(paths);
		readPhysicalSize(paths.get(0));
	}
	
	@Override
	protected DataSize getDataSize(List<Path> paths, HDF5DataSetInformation info) {
		long[] dimensions = info.getDimensions();
		SimpleDataSize dataSize = new SimpleDataSize();
		dataSize.setDataHeight((int) dimensions[0]);
		dataSize.setDataWidth((int) dimensions[1]);
		return dataSize;
	}

	
	@Override
	protected void readFile(Path path, int filenum) throws InterruptedException {
		String entry = "/2D Scan/MCA 1";
		IHDF5Reader reader = HDF5Factory.openForReading(path.toFile());
		
		info = reader.getDataSetInformation(entry);
		rowCount = (int) info.getDimensions()[0];
		columnCount = (int) info.getDimensions()[1];
		scanSize = (int) info.getDimensions()[2];
				
		blockLists = getBlockLists(reader);
		

		
		//This format stores uses data blocks which each store 1 row
		
		for (int row = 0; row < rowCount; row++) {
			float[] rowData = readRow(reader, row);
						
			for (int column = 0; column < columnCount; column++) {
				float[] scan = Arrays.copyOfRange(rowData, (int)(column*scanSize), (int)((column+1)*scanSize));
				super.submitScan((int)(row*columnCount+column), new ISpectrum(scan, false));
			}
			
			if (getInteraction().checkReadAborted()) {
				return;
			}
			
		}


	}
	
	private List<Iterator<HDF5MDDataBlock<MDFloatArray>>> getBlockLists(IHDF5Reader reader) {
		List<Iterator<HDF5MDDataBlock<MDFloatArray>>> blockList = new ArrayList<>();
		String base = "/2D Scan/MCA ";
		
		int entryNumber = 1;
		while (true) {
			String entry = base + entryNumber;
			if (!reader.exists(entry)) {
				return blockList;
			}
			blockList.add(reader.float32().getMDArrayNaturalBlocks(entry).iterator());
			entryNumber++;
		}
	}
	
	private float[] readRow(IHDF5Reader reader, int row) {
		float[] rowData = new float[scanSize * columnCount];
		
		//merge each extra MCA Entry into this data array for the row
		for (Iterator<HDF5MDDataBlock<MDFloatArray>> MCABlockList : blockLists) {
			float[] MCARowData = MCABlockList.next().getData().getAsFlatArray();
			for (int i = 0; i < rowData.length; i++) {
				rowData[i] += MCARowData[i];
			}
		}
		return rowData;
	}
	

	private void readPhysicalSize(Path path) {
		IHDF5SimpleReader reader = HDF5Factory.openForReading(path.toFile());
		float[] ypos = reader.readFloatArray("/2D Scan/Y Positions");
		float[] xpos = reader.readFloatArray("/2D Scan/X Positions");
		
		physical = new SimplePhysicalSize(SISize.um);
		
		int index = 0;
		//xpos holds position values for all points, ypos only holds position information per row
		for (int x = 0; x < xpos.length; x++) {
			int y = (int) Math.floor(x / columnCount);
			physical.putPoint(index++, new Coord<>(xpos[x], ypos[y]));
		}
		
	}
	
	@Override
	public Optional<PhysicalSize> getPhysicalSize() {
		return Optional.ofNullable(physical);
	}

}
