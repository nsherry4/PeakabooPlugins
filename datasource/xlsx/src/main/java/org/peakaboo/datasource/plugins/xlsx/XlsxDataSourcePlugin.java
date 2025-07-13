package org.peakaboo.datasource.plugins.xlsx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.peakaboo.dataset.io.DataInputAdapter;
import org.peakaboo.dataset.source.model.DataSourceReadException;
import org.peakaboo.dataset.source.model.components.datasize.DataSize;
import org.peakaboo.dataset.source.model.components.fileformat.FileFormat;
import org.peakaboo.dataset.source.model.components.fileformat.SimpleFileFormat;
import org.peakaboo.dataset.source.model.components.metadata.Metadata;
import org.peakaboo.dataset.source.model.components.physicalsize.PhysicalSize;
import org.peakaboo.dataset.source.model.components.scandata.PipelineScanData;
import org.peakaboo.dataset.source.model.components.scandata.ScanData;
import org.peakaboo.dataset.source.plugin.AbstractDataSource;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.cyclops.spectrum.Spectrum;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XlsxDataSourcePlugin extends AbstractDataSource {

	private PipelineScanData scandata;
	
	public static class ReadAbortedException extends RuntimeException {}
	
	/**
	 * The built-in data formatter is very slow, trying to present each cell as it
	 * would be formatted in a spreadsheet (eg as currency, dates, etc). We don't
	 * need that formatting, but to stay consistent with the API, we must convert
	 * the double to a string, and then convert it back to a float in the SAX event
	 * handler
	 */
	public static class NumericDataFormatter extends DataFormatter {
		@Override
		public String formatRawCellContents(double value, int formatIndex, String formatString,
				boolean use1904Windowing) {
			return Double.toString(value);
		}
	}
	
	/**
	 * Functional interface for processing scan data
	 */
	@FunctionalInterface
	public interface ScanProcessor {
		void acceptScan(int index, Spectrum spectrum) throws IOException, InterruptedException;
	}
	
	@Override
	public void read(DataSourceContext ctx) throws DataSourceReadException, IOException, InterruptedException {
		
		// Get the list of inputs, confirm there's exactly one, and extract it
		List<DataInputAdapter> inputSources = ctx.inputs();
		if (inputSources.size() != 1) {
			throw new IllegalArgumentException("Expected 1 file");
		}
		DataInputAdapter inputSource = inputSources.get(0);
		
		// We load our scans into `scandata` and it's threadpool takes the scans the rest of the way 
		scandata = new PipelineScanData(DataInputAdapter.getTitle(inputSources));
		
		// Our event handler will pull the required information as the parser reads the document
		SaxEventHandler saxEventHandler = new SaxEventHandler(this::submit, getInteraction()::checkReadAborted);
		
		// Try to read the input, informing the scandata of our results
		try {
			parse(inputSource, saxEventHandler);
		} catch (ReadAbortedException e) {
			// If the user has requested the read be aborted, we'll end up here.
			// We notify the scandata that the loading is aborted and bail.
			scandata.abort();
		} finally {
			scandata.finish();
		}
		
	}
	
	private static void parse(DataInputAdapter input, SaxEventHandler saxHandler) throws IOException {
		try {
			var container = OPCPackage.open(input.getAndEnsurePath().toFile());
			XSSFReader xssfReader = new XSSFReader(container);

			// Get the first sheet as an input source
			Iterator<InputStream> sheets = xssfReader.getSheetsData();
			if (!sheets.hasNext()) {
				throw new IOException("Spreadsheet contained no data");
			}
			InputSource inputSheet = new InputSource(sheets.next());

			// Create an XML parser to read the input stream for the specific sheet we're looking at 
			XMLReader parser = XMLHelper.newXMLReader();
			parser.setContentHandler(new XSSFSheetXMLHandler(
					xssfReader.getStylesTable(), 
					null,
					new ReadOnlySharedStringsTable(container), 
					saxHandler, 
					new NumericDataFormatter(), 
					true
				));
			parser.parse(inputSheet);
			
		} catch (OpenXML4JException | SAXException e) {
			throw new IOException("Cannot open file, invalid format", e);
		} catch (ParserConfigurationException e) {
			throw new IOException("Parser failed to work as expected", e);
		}
	}
	
	private void submit(int index, Spectrum s) throws InterruptedException {
		scandata.submit(index, s);
		getInteraction().notifyScanCount(1);
	}
	
	@Override
	public Optional<Group> getParameters(List<DataInputAdapter> datafiles) throws DataSourceReadException, IOException {
		return Optional.empty();
	}

	@Override
	public Optional<Metadata> getMetadata() {
		return Optional.empty();
	}

	@Override
	public Optional<DataSize> getDataSize() {
		return Optional.empty();
	}

	@Override
	public Optional<PhysicalSize> getPhysicalSize() {
		return Optional.empty();
	}

	@Override
	public FileFormat getFileFormat() {
		return new SimpleFileFormat(true, "Excel (Office Open XML)", "Microsoft-developed file format for spreadsheets", List.of("xlsx"));
	}

	@Override
	public ScanData getScanData() {
		return scandata;
	}

	@Override
	public String pluginVersion() {
		return "1.0";
	}

	@Override
	public String pluginUUID() {
		return "aa19aa83-c321-41f4-8225-861d97a40952";
	}

}
