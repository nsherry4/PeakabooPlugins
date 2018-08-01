package peakaboo.datasource.plugins.APSSector20;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;
import peakaboo.datasource.model.components.datasize.DataSize;
import peakaboo.datasource.model.components.datasize.SimpleDataSize;
import peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import peakaboo.datasource.model.components.physicalsize.SimplePhysicalSize;
import peakaboo.datasource.plugins.GenericHDF5.SimpleHDF5DataSource;
import scitypes.Coord;
import scitypes.SISize;

public class APSSector20 extends SimpleHDF5DataSource {

	private final static String DATA_PATH = "/2D Scan/MCA 1";
	private SimplePhysicalSize physical;
	
	public APSSector20() {
		super(DATA_PATH, "APS Sector 20", "HDF5 Data from Sector 20 of the APS Synchrotron");
	}

	@Override
	public String pluginVersion() {
		return "1.0.1";
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
	protected DataSize calcDataSize(List<Path> paths, HDF5DataSetInformation info) {
		long[] dimensions = info.getDimensions();
		SimpleDataSize dataSize = new SimpleDataSize();
		dataSize.setDataHeight((int) dimensions[0]);
		dataSize.setDataWidth((int) dimensions[1]);
		return dataSize;
	}

	@Override
	protected int calcScanWidth(HDF5DataSetInformation info) {
		return (int) info.getDimensions()[2];
	}

	@Override
	protected int scanAtIndex(int file, int index, HDF5DataSetInformation info) {
		return index / calcScanWidth(info);
	}

	@Override
	protected int channelAtIndex(int file, int index, HDF5DataSetInformation info) {
		return index % calcScanWidth(info);
	}
	
	@Override
	protected float[] readSpectralData(Path path) {
		IHDF5SimpleReader reader = HDF5Factory.openForReading(path.toFile());
		float[] scan = reader.readFloatArray("/2D Scan/MCA 1");

		int block = 2;
		while (true) {
			String blockName = "/2D Scan/MCA " + block++;
			if (!reader.exists(blockName)) {
				break;
			}
			float[] mca = reader.readFloatArray(blockName);	
			for (int i = 0; i < scan.length; i++) {
				scan[i] += mca[i];
			}
		}
	
		return scan;
	}
	
	private void readPhysicalSize(Path path) {
		IHDF5SimpleReader reader = HDF5Factory.openForReading(path.toFile());
		float[] ypos = reader.readFloatArray("/2D Scan/Y Positions");
		float[] xpos = reader.readFloatArray("/2D Scan/X Positions");
		
		physical = new SimplePhysicalSize(SISize.um);
		
		int index = 0;
		for (int y = 0; y < ypos.length; y++) {
			for (int x = 0; x < xpos.length; x++) {
				physical.putPoint(index++, new Coord<>(xpos[x], ypos[y]));
			}
		}
		
	}
	
	@Override
	public Optional<PhysicalSize> getPhysicalSize() {
		return Optional.of(physical);
	}

}
