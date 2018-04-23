package peakaboo.datasource.plugins.GenericHDF5;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import net.sciencestudio.autodialog.model.Group;
import peakaboo.datasource.model.components.fileformat.FileFormat;
import peakaboo.datasource.model.components.metadata.Metadata;
import peakaboo.datasource.model.components.physicalsize.PhysicalSize;

public abstract class SimpleHDF5DataSource extends AbstractHDF5DataSource {

	private String dataPath, name, description;
	
	public SimpleHDF5DataSource(String dataPath, String name, String description) {
		super(dataPath);
		this.dataPath = dataPath;
		this.name = name;
		this.description = description;
	}

	@Override
	public Optional<Group> getParameters(List<Path> paths) {
		return Optional.empty();
	}

	@Override
	public Optional<Metadata> getMetadata() {
		return Optional.empty();
	}

	@Override
	public Optional<PhysicalSize> getPhysicalSize() {
		return Optional.empty();
	}

	@Override
	public FileFormat getFileFormat() {
		return new SimpleHDF5FileFormat(dataPath, name, description);
	}


}
