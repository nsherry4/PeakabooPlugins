package org.peakaboo.datasource.plugins.CHESS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.peakaboo.app.PeakabooLog;
import org.peakaboo.dataset.io.DataInputAdapter;
import org.peakaboo.dataset.source.model.components.datasize.DataSize;
import org.peakaboo.dataset.source.model.components.datasize.SimpleDataSize;
import org.peakaboo.dataset.source.plugin.plugins.universalhdf5.FloatMatrixHDF5DataSource;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.autodialog.model.Parameter;
import org.peakaboo.framework.autodialog.model.SelectionParameter;
import org.peakaboo.framework.autodialog.model.style.editors.CheckBoxStyle;
import org.peakaboo.framework.autodialog.model.style.editors.DropDownStyle;
import org.peakaboo.framework.cyclops.spectrum.ArraySpectrum;
import org.peakaboo.framework.cyclops.spectrum.Spectrum;
import org.peakaboo.framework.cyclops.spectrum.SpectrumCalculations;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class CHESS extends FloatMatrixHDF5DataSource  {

	private static final String[] PATH_ENDS = new String[]{
			"/measurement/vortex1/counts",
			"/measurement/vortex2/counts",
			"/measurement/vortex3/counts",
			"/measurement/vortex4/counts"
		};
	private static final String NAME = "CHESS";
	private static final String DESC = "CHESS XRF spectral data stored in an HDF5 container";
	
	private SelectionParameter<String> root;
	private Parameter<Boolean> deadtime;
	
	public CHESS() {
		super("xz", NAME, DESC);
	}

	@Override
	public String pluginVersion() {
		return "0.4";
	}

	@Override
	public String pluginUUID() {
		return "9f884f75-616d-4585-9c94-48e9391c7799";
	}
	
	/**
	 * Get the data paths as detected and selected.
	 */
	@Override
	protected List<String> getDataPaths(List<DataInputAdapter> paths) {
		DataInputAdapter path = paths.get(0);
		
		//user has not been prompted to make selection yet, so we list all valid data paths
		if (root == null) { return allDataPaths(path); }
		
		try (IHDF5Reader reader = getReader(path)) {
			List<String> dataPaths = new ArrayList<>();
			String rootPath = root.getValue();
			for (String pathEnd : PATH_ENDS) {
				String dataPath = "/" + rootPath + pathEnd;
				if (reader.exists(dataPath)) {
					dataPaths.add(dataPath);
				}
			}
			return dataPaths;
		} catch (IOException e) {
			PeakabooLog.get().log(Level.SEVERE, "Failed to detect data paths", e);
			return List.of();
		}
	}
	
	public List<String> allDataPaths(DataInputAdapter path) {
		try (IHDF5Reader reader = getReader(path)) {
			List<String> dataPaths = new ArrayList<>();
			for (String rootName : validRoots(path)) {
				for (String pathEnd : PATH_ENDS) {
					String dataPath = "/" + rootName + pathEnd;
					if (reader.exists(dataPath)) {
						dataPaths.add(dataPath);
					}
				}
			}
			
			return dataPaths;
		} catch (IOException e) {
			PeakabooLog.get().log(Level.SEVERE, "Failed to detect data paths", e);
			return List.of();
		}
	}
	
	private static List<String> validRoots(DataInputAdapter path) {
		try (IHDF5Reader reader = getReader(path)) {
			List<String> validRoots = new ArrayList<>();
			List<String> members = reader.getGroupMembers("/");
			for (String member : members) {
				String dataPath = "/" + member + "/measurement/vortex1/counts";
				if (reader.exists(dataPath)) {
					validRoots.add(member);
				}
			}
			return validRoots;
		} catch (IOException e) {
			PeakabooLog.get().log(Level.SEVERE, "Failed to detect data path roots", e);
			return List.of();
		}
	}
	
	@Override
	protected DataSize getDataSize(DataInputAdapter path, HDF5DataSetInformation datasetInfo) {
		SimpleDataSize size = new SimpleDataSize();
		try (IHDF5Reader reader = getReader(path)) {
			//get the y-position array, and assume that the value doesn't change
			//while scanning a single row
			String samzPath = root.getValue() + "/measurement/scalar_data/samz";
			float[] samz = reader.readFloatArray(samzPath);
			float first = samz[0];
			//find the first change, this will give us our width
			int x = 1;
			for (; x < samz.length; x++) {
				if (samz[x] != first) break;
			}
			//integer division truncation should produce the same effect
			//as taking the floor here
			int y = samz.length / x;
			size.setDataWidth(x);
			size.setDataHeight(y);
		} catch (IOException e) {
			PeakabooLog.get().log(Level.SEVERE, "Failed to determine data size", e);
			//Can we do better here? Optional?
			return new SimpleDataSize();
		}
		return size;
	}
	
	public Optional<Group> getParameters(List<DataInputAdapter> datafiles) {
		DataInputAdapter datafile = datafiles.get(0);
		
		//it's important that we initialize this parameter even when there's only one dataset
		//because this is where we look that dataset's name up later on
		List<String> roots = validRoots(datafile);
		root = new SelectionParameter<String>("Dataset", new DropDownStyle<>(), roots.get(0));
		root.setPossibleValues(roots);
		
		//TODO: deadtime correction -- how is dead_time represented in this format?
		deadtime = new Parameter<Boolean>("Deadtime Correction", new CheckBoxStyle(), true);
		if (roots.size() == 1) {
			return Optional.of(new Group("Options", deadtime));
		} else {
			return Optional.of(new Group("Options", root, deadtime));
		}
	}

	protected Spectrum getDeadtimes(String dataPath, IHDF5Reader reader) {
		if (deadtime.getValue()) {
			String dir = dataPath.substring(0, dataPath.lastIndexOf("/"));
			String deadtimePath = dir + "/dead_time";
			float[] deadtimeArray = reader.readFloatArray(deadtimePath);
			//TODO: confirm this assumption
			//Stores deadtime as percent 0-100, need to scale to 0.0-1.0
			Spectrum deadtimes = new ArraySpectrum(deadtimeArray, true);
			SpectrumCalculations.divideBy_inplace(deadtimes, 100f);
			return deadtimes;
		} else {
			return super.getDeadtimes(dataPath, reader);
		}
	}
	
	@Override
	protected String getDatasetTitle(List<DataInputAdapter> paths) {
		if (root.getPossibleValues().size() <= 1) {
			return super.getDatasetTitle(paths);
		} else {
			return super.getDatasetTitle(paths) + " - Dataset " + root.getValue();
		}
	}
	
}
