package org.peakaboo.datasource.plugins.Horiba5200;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.peakaboo.dataset.io.PathDataInputAdapter;
import org.peakaboo.dataset.source.model.DataSource.DataSourceContext;
import org.peakaboo.dataset.source.model.DataSourceReadException;
import org.peakaboo.framework.cyclops.FloatException;

public class Tests {

	@Test
	public void main() throws DataSourceReadException, IOException, InterruptedException {

		var ds = new Horiba5200();
		var file = new File("src/test/resources/datasets/Transect/Transect1003.txt").getAbsoluteFile();
		ds.read(new DataSourceContext(List.of(new PathDataInputAdapter(file))));
		float counts = ds.getScanData().get(0).get(100);
		FloatException.guard(counts);
		assertEquals(counts, 5f, 0.001f);
		
	}
	
}
