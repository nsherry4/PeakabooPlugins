package org.peakaboo.datasource.plugins.UniversalHDF5;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.peakaboo.datasource.model.components.fileformat.FileFormat;
import org.peakaboo.datasource.plugins.GenericHDF5.FloatMatrixHDF5DataSource;
import org.peakaboo.datasource.plugins.UserDrivenHDF5.UserDrivenHDF5DFileFormat;
import org.peakaboo.datasource.plugins.UserDrivenHDF5.UserDrivenHDF5DataSource;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.autodialog.model.Parameter;
import org.peakaboo.framework.autodialog.model.SelectionParameter;
import org.peakaboo.framework.autodialog.model.classinfo.EnumClassInfo;
import org.peakaboo.framework.autodialog.model.style.editors.DropDownStyle;
import org.peakaboo.framework.autodialog.model.style.editors.LabelStyle;
import org.peakaboo.framework.autodialog.model.style.layouts.FramedLayoutStyle;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;

public class UniversalHDF5DataSource extends FloatMatrixHDF5DataSource {

	public enum Axis {
		X, Y, Z, None;
	}
	
	private SelectionParameter<String> path;
	private Parameter<String> pathinfo;
	private SelectionParameter<Axis> widthAxis;
	private SelectionParameter<Axis> heightAxis;
	private SelectionParameter<Axis> spectrumAxis;
	private Group pathGroup, axisGroup, topGroup;
	
	public UniversalHDF5DataSource() {
		super("Generic HDF5 Datasource", "A generic HDF5 datasource that allows users to select the data to open");
	}
	
	private void initialize(List<Path> paths) {
		IHDF5SimpleReader mdreader = super.getMetadataReader(paths);
		List<String> datasetPaths = listDatasets(mdreader);
		
		path = new SelectionParameter<String>("Path", new DropDownStyle<String>(), datasetPaths.get(0));
		path.setPossibleValues(datasetPaths);
		
		pathinfo = new Parameter<String>("Path Information", new LabelStyle(), getPathInfo(mdreader, datasetPaths.get(0)));
		
		path.getValueHook().addListener(newpath -> {
			pathinfo.setValue(getPathInfo(mdreader, newpath));
		});
		
		spectrumAxis = new SelectionParameter<UniversalHDF5DataSource.Axis>("Spectrum Axis", new DropDownStyle<>(), Axis.X, new EnumClassInfo<>(Axis.class), this::validator);
		widthAxis = new SelectionParameter<UniversalHDF5DataSource.Axis>("Width Axis", new DropDownStyle<>(), Axis.None, new EnumClassInfo<>(Axis.class), this::validator);
		heightAxis = new SelectionParameter<UniversalHDF5DataSource.Axis>("Height Axis", new DropDownStyle<>(), Axis.None, new EnumClassInfo<>(Axis.class), this::validator);
		
		spectrumAxis.setPossibleValues(Axis.values());
		widthAxis.setPossibleValues(Axis.values());
		heightAxis.setPossibleValues(Axis.values());
		
		pathGroup = new Group("Dataset Path", Arrays.asList(path, pathinfo), new FramedLayoutStyle());
		axisGroup = new Group("Data Matrix Layout", Arrays.asList(spectrumAxis, widthAxis, heightAxis), new FramedLayoutStyle());
		topGroup = new Group("HDF5 Parameters", pathGroup, axisGroup);
	}
	
	private boolean validator(Parameter<?> param) {
		//if (widthAxis.getValue() == heightAxis.getValue() && widthAxis.getValue() != Axis.None) { return false; }
		//if (widthAxis.getValue() == spectrumAxis.getValue() && widthAxis.getValue() != Axis.None) { return false; }
		//if (heightAxis.getValue() == spectrumAxis.getValue() && widthAxis.getValue() != Axis.None) { return false; }
		if (spectrumAxis.getValue() == Axis.None) { return false; }
		return true;
	}
	
	private String getPathInfo(IHDF5SimpleReader mdreader, String path) {
		HDF5DataSetInformation info = mdreader.getDataSetInformation(path);
		
		StringBuilder sb = new StringBuilder();
		sb.append("Dimensions: ");
		
		int count = 0;
		for (long dim : info.getDimensions()) {
			if (count > 0) { sb.append(", "); }
			
			sb.append(Axis.values()[count]);
			sb.append("=");
			sb.append(dim);
			
			count++;
		}
		
		return sb.toString();
	}
	
	private List<String> listDatasets(IHDF5SimpleReader mdreader) {
		List<String> datasets = listDatasets(mdreader, "/");
		
		Comparator<String> scorer = new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				float s1 = scoreDataset(mdreader, o1);
				float s2 = scoreDataset(mdreader, o2);
				//backwards comparison to higher scores are first
				return Float.compare(s2, s1);
			}
		};
		datasets.sort(scorer);
		return datasets;
		
	}
	
	private List<String> listDatasets(IHDF5SimpleReader mdreader, String path) {
		if (!mdreader.isGroup(path)) {
			//remove trailing slash from path, as this is not a group (folder)
			path = path.substring(0, path.length()-1);
			//leaf nodes return themselves if they're a good candidate for containing data
			if (scoreDataset(mdreader, path) < 0.2f) {
				return Collections.emptyList();
			} else {
				return Collections.singletonList(path);
			}
		} else {
			//non-leaf nodes return all child leaves
			List<String> leaves = new ArrayList<>();
			for (String subpath : mdreader.getGroupMembers(path)) {
				String fullpath = path + subpath + "/";
				leaves.addAll(listDatasets(mdreader, fullpath));
			}
			return leaves;
		}
		
	}
	
	private float scoreDataset(IHDF5SimpleReader mdreader, String path) {
		HDF5DataSetInformation info = mdreader.getDataSetInformation(path);
		float score = 1f;
		
		//Does this dataset contain at least one axis that is a power of two and largish?
		boolean hasPowerOfTwo = false;
		for (long dim : info.getDimensions()) {
			if (dim > 128) { hasPowerOfTwo |= isPowerOfTwo(dim); }
		}
		if (!hasPowerOfTwo) {
			score *= 0.5f;
		}
		
		
		//3 axes are more likely than two, more than one
		int dims = info.getRank();
		if (dims == 0 || dims > 3) {
			score = 0;
		} else if (dims == 1) {
			score *= 0.25;
		} else if (dims == 2) {
			score *= 0.5;
		} else {
			//exactly 3, no downward adjustment
		}
		
		//must have more than 255 total elements
		if (info.getNumberOfElements() <= 256) {
			score *= 0.1f;
		}
		
		return score;
		
	}
	
	@Override
	public FileFormat getFileFormat() {
		return new UserDrivenHDF5DFileFormat();
	}
	
	@Override
	public Optional<Group> getParameters(List<Path> paths) {
		initialize(paths);
		return Optional.of(topGroup);
	}
	
	@Override
	public String pluginVersion() {
		return "0.1";
	}

	@Override
	public String pluginUUID() {
		return "3ca671cf-6bfd-4372-9aae-d36b3a19d476";
	}

	
	//During `read`, this will be called to get the final list of HDF5 paths (not files) to read
	protected List<String> getDataPaths(List<Path> paths) {
		return Collections.singletonList(path.getValue());
	}
	
	protected String getAxisOrder() {
		String[] order = new String[] {"", "", ""};
		Axis axis;
		List<Axis> axes = Arrays.asList(Axis.values());

		StringBuilder sb = new StringBuilder();
	
		axis = widthAxis.getValue();
		if (axis != Axis.None) {
			order[axes.indexOf(axis)] = "x";
		}
		
		axis = heightAxis.getValue();
		if (axis != Axis.None) {
			order[axes.indexOf(axis)] = "y";
		}
		
		axis = spectrumAxis.getValue();
		if (axis != Axis.None) {
			order[axes.indexOf(axis)] = "z";
		}
		
		
		for (String o : order) {
			sb.append(o);
		}
		return sb.toString();
		
	}

	
	private static boolean isPowerOfTwo(long value) {
		return (value & value-1) == 0;
	}
	
}
