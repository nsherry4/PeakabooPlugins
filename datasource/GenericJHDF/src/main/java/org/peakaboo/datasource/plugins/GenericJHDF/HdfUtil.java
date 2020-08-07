package org.peakaboo.datasource.plugins.GenericJHDF;

import io.jhdf.api.Dataset;

public class HdfUtil {

	public static float[] readFloatArray(Dataset ds) {
		return readFloatArray(ds, ds.getData(), 0, new int[] {-1});
	}
	public static float[] readFloatArray(Dataset ds, Object data, int dimension, int[] indices) {
		int size = ds.getDimensions()[dimension];
		float[] result = new float[size];

		for (int i = 0; i < size; i++) {
			indices[dimension] = i;
			result[i] = readFloatValue(data, indices);
		}
		
		return result;		
	}
	
	public static double[] readDoubleArray(Dataset ds) {
		return readDoubleArray(ds, ds.getData(), 0, new int[] {-1});
	}
	public static double[] readDoubleArray(Dataset ds, Object data, int dimension, int[] indices) {
		int size = ds.getDimensions()[dimension];
		double[] result = new double[size];

		for (int i = 0; i < size; i++) {
			indices[dimension] = i;
			result[i] = readDoubleValue(data, indices);
		}
		
		return result;		
	}
	

	private static double readDoubleValue(Object data, int[] indices) {
		
		Object[] arrays;
		double[] values;
		
		for (int i = 0; i < indices.length-1; i++) {
			arrays = (Object[]) data;
			data = arrays[indices[i]];
		}
		
		values = (double[]) data;
		return values[indices[indices.length-1]];
	}
	

	private static float readFloatValue(Object data, int[] indices) {
		
		Object[] arrays;
		float[] values;
		
		for (int i = 0; i < indices.length-1; i++) {
			arrays = (Object[]) data;
			data = arrays[indices[i]];
		}
		
		values = (float[]) data;
		return values[indices[indices.length-1]];
	}

}
