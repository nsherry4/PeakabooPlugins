package org.peakaboo.datasource.plugins.GenericJHDF;

import java.io.Closeable;
import java.io.IOException;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;

public class JHDFReader implements Closeable{

	private HdfFile file;
	
	public JHDFReader(HdfFile file) {
		this.file = file;
	}
	
	public Dataset getDataset(String path) {
		return file.getDatasetByPath(path);
	}
	
	public float[] readFloatArray(String path) {
		return readFloatArray(path, 0, new int[] {-1});
	}
	public float[] readFloatArray(String path, int dimension, int[] indices) {
		Dataset ds = file.getDatasetByPath(path);
		int size = ds.getDimensions()[dimension];
		float[] result = new float[size];

		for (int i = 0; i < size; i++) {
			indices[dimension] = i;
			result[i] = readFloatValue(ds, indices);
		}
		
		return result;		
	}
	
	public double[] readDoubleArray(String path) {
		return readDoubleArray(path, 0, new int[] {-1});
	}
	public double[] readDoubleArray(String path, int dimension, int[] indices) {
		Dataset ds = file.getDatasetByPath(path);
		int size = ds.getDimensions()[dimension];
		double[] result = new double[size];

		for (int i = 0; i < size; i++) {
			indices[dimension] = i;
			result[i] = readDoubleValue(ds, indices);
		}
		
		return result;		
	}
	
	public double readDoubleValue(String path, int[] indices) {
		return readDoubleValue(file.getDatasetByPath(path), indices);
	}
	private double readDoubleValue(Dataset ds, int[] indices) {
		
		Object data = ds.getData();
		Object[] arrays;
		double[] values;
		
		for (int i = 0; i < indices.length-1; i++) {
			arrays = (Object[]) data;
			data = arrays[indices[i]];
		}
		
		values = (double[]) data;
		return values[indices[indices.length-1]];
	}
	
	
	public float readFloatValue(String path, int[] indices) {
		return readFloatValue(file.getDatasetByPath(path), indices);
	}
	private float readFloatValue(Dataset ds, int[] indices) {
		
		Object data = ds.getData();
		Object[] arrays;
		float[] values;
		
		for (int i = 0; i < indices.length-1; i++) {
			arrays = (Object[]) data;
			data = arrays[indices[i]];
		}
		
		values = (float[]) data;
		return values[indices[indices.length-1]];
	}

	public HdfFile getHdfFile() {
		return file;
	}
	
	@Override
	public void close() throws IOException {
		file.close();
	}
	
}
