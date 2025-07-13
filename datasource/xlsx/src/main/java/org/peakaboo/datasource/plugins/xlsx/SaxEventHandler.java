package org.peakaboo.datasource.plugins.xlsx;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.peakaboo.datasource.plugins.xlsx.XlsxDataSourcePlugin.ReadAbortedException;
import org.peakaboo.datasource.plugins.xlsx.XlsxDataSourcePlugin.ScanProcessor;
import org.peakaboo.framework.cyclops.spectrum.ArraySpectrum;

public class SaxEventHandler implements SheetContentsHandler {


	// Tie-ins
	private final ScanProcessor outbox;
	private final Supplier<Boolean> abortFlag;
	
	// Local Data
	private float[] currentSpectrum = new float[0];
	private int currentRow;

	public SaxEventHandler(ScanProcessor outbox, Supplier<Boolean> abortFlag) {
		this.outbox = outbox;
		this.abortFlag = abortFlag;
	}

	@Override
	public void startRow(int rowNum) {
		currentRow = rowNum;
		if (currentRow > 0) {
			// We gave the old array to the outbox, so we can't use it anymore.
			// For the next row, we create a new, zero'ed-out array of the same size
			currentSpectrum = new float[currentSpectrum.length];
		}
	}

	@Override
	public void endRow(int rowNum) {

		try {
			// Try to submit the spectrum we have collected for this row now that it's done
			outbox.acceptScan(rowNum, new ArraySpectrum(currentSpectrum, false));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted");
		} catch (IOException e) {
			throw new RuntimeException("Error processing row " + (rowNum + 1), e);
		}
		
		if (this.abortFlag.get()) {
			throw new ReadAbortedException();
		}
		
		
	}

	@Override
	public void cell(String cellReference, String formattedValue, XSSFComment comment) {
		if (formattedValue == null) {
			return;
		}
		String trimmed = formattedValue.trim();
		if (trimmed.isEmpty()) {
			return;
		}
		
		// Doesn't seem to be a way to avoid constructing one of these each time we visit a cell.
		// Unless we want to parse the string ourselves, that is.
		CellReference ref = new CellReference(cellReference);
		int colNum = ref.getCol();
		
		if (currentRow == 0) {
			// Grow the array by one, since we clearly need the room but don't know how large it will be
			// This is inefficient, but only for the first line.
			currentSpectrum = Arrays.copyOf(currentSpectrum, colNum+1);
		}
		currentSpectrum[colNum] = Float.parseFloat(trimmed);
		
	}
	
}
