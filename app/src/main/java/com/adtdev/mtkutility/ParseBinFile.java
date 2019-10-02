/**
 * @author Bastiaan Naber (AndroidMTK app - GNU GPL v3 license)
 * https://code.google.com/p/androidmtk/
 * 
 * modified by Al Tauber
 * - changed source of one track setting from sharedPref to passed parameter
 * = implemented MTKutility2 logging
 * 
 * This file is part of the Android app mtkutility.
 * 
 * mtkutility is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License. This extends to files
 * included that were authored by others and modified to make them suitable for
 * mtkutility. All files included were subject to open source licensing.
 * 
 * mtkutility is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You can review a copy of the GNU General Public License
 * at http://www.gnu.org/licenses.
 *
 */
package com.adtdev.mtkutility;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar; //dyj

public class ParseBinFile implements Runnable {
	public static final String TAG = "ParseBinFile";
	private static final int SIZEOF_SECTOR = 0x10000;
//	private myLibrary mL;

	// Log format is stored as a bitmask field.
	private static final int LOG_FORMAT_UTC = 0x00000001;
	private static final int LOG_FORMAT_VALID = 0x00000002;
	private static final int LOG_FORMAT_LATITUDE = 0x00000004;
	private static final int LOG_FORMAT_LONGITUDE = 0x00000008;
	private static final int LOG_FORMAT_HEIGHT = 0x00000010;
	private static final int LOG_FORMAT_SPEED = 0x00000020;
	private static final int LOG_FORMAT_HEADING = 0x00000040;
	private static final int LOG_FORMAT_DSTA = 0x00000080;
	private static final int LOG_FORMAT_DAGE = 0x00000100;
	private static final int LOG_FORMAT_PDOP = 0x00000200;
	private static final int LOG_FORMAT_HDOP = 0x00000400;
	private static final int LOG_FORMAT_VDOP = 0x00000800;
	private static final int LOG_FORMAT_NSAT = 0x00001000;
	private static final int LOG_FORMAT_SID = 0x00002000;
	private static final int LOG_FORMAT_ELEVATION = 0x00004000;
	private static final int LOG_FORMAT_AZIMUTH = 0x00008000;
	private static final int LOG_FORMAT_SNR = 0x00010000;
	private static final int LOG_FORMAT_RCR = 0x00020000;
	private static final int LOG_FORMAT_MILLISECOND = 0x00040000;
	private static final int LOG_FORMAT_DISTANCE = 0x00080000;

	private static final int VALID_NOFIX = 0x0001;
	private boolean LOG_IS_HOLUX_M241 = false;

	private int gpx_trk_number = 0;
	private byte[] buffer = new byte[SIZEOF_SECTOR];
	private byte[] emptyseparator = new byte[0x10];
	private BufferedWriter log_writer = null;
	private final Handler parseHandler;
	private boolean gpx_in_trk;
	private final boolean one_trk;
	private final String binPath;
//	private final String gpxPath;
	private String gpxPath;
	private String msg;
//	private File log_file = null;
	private String oldToastmessage = "";
	private int oldPercentage = 0;
	private String GPXdir = "";
	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);

	public ParseBinFile(File BINpath, File GPXpath, Handler parseHandler, boolean one_trk) {
		this.parseHandler = parseHandler;
		this.binPath = BINpath.toString();
		this.gpxPath = GPXpath.toString();
		this.one_trk = one_trk;

//		mL = Main.mL;
//		mL.mLog(mL.VB0, "ParseBinFile.ParseBinFile()");
//		GPXdir = myLib.gpxPathName;

		for (int i = 0; i < 0x10; i++) {
			emptyseparator[i] = (byte) 0xFF;
		}

		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public void doConvert() throws IOException {
//        mL.mLog(mL.VB1, "ParseBinFile.doConvert()");
		// Open an input stream for reading from the binary Log
		File bin_file = new File(binPath);
		BufferedInputStream reader;
		try {
			FileInputStream freader = new FileInputStream(bin_file);
			Log(String.format("Reading bin file: %s", bin_file.toString()));
			reader = new BufferedInputStream(freader, SIZEOF_SECTOR);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;}

		// Open an output for writing the gpx file
//		gpxPath = gpxPath.toLowerCase(Locale.CANADA);
//		gpxPath = gpxPath.replace(".bin", ".gpx");
		File gpx_file = new File(gpxPath);
		Log("Creating GPX file: "+gpx_file.toString());
//		sendMessageToMessageField("Creating GPX file: "+gpx_file.toString());
		BufferedWriter gpx_writer = null;
		try {
			FileWriter fwriter = null;
			fwriter = new FileWriter(gpx_file);
			gpx_writer = new BufferedWriter(fwriter, SIZEOF_SECTOR);
			WriteHeader(gpx_writer);
		} catch (IOException e) {
			e.printStackTrace();
			reader.close();
			return;}

		int bytes_in_sector = 0;
		int sector_count = 0;
		int log_count_fullywrittensector=-1;

		int totalNrOfSectors = Double.valueOf(bin_file.length() / SIZEOF_SECTOR).intValue();
		if (bin_file.length() % SIZEOF_SECTOR != 0){
			totalNrOfSectors++;}
		Log("totalNrOfSectors: "+totalNrOfSectors);
		String formattedDate = "";
		int record_count_total=0;
		while ((bytes_in_sector = reader.read(buffer, 0, SIZEOF_SECTOR)) > 0) {
			sector_count++;
			ByteBuffer buf = ByteBuffer.wrap(buffer);
			buf.order(ByteOrder.LITTLE_ENDIAN);

			short nrOfRecordsInSector = buf.getShort(0);
			// -1 is used if a sector is not fully written
			if (nrOfRecordsInSector == -1) {
				nrOfRecordsInSector = 5000;
			} else {
				log_count_fullywrittensector=nrOfRecordsInSector;}
			int log_format = buf.getInt(2);

			// Skip the header (which is 0x200 bytes long)
			buf.position(0x200);

			Log(String.format("Read %d bytes from bin file", bytes_in_sector));
			Log(String.format("Reading sector"));
			Log(String.format("Log format %x", log_format));
			Log(String.format("Nr of sector records: %d", nrOfRecordsInSector));

			int record_count_sector = 0;
			while (record_count_sector < nrOfRecordsInSector) {
				byte[] tmp = new byte[0x10];
				// Test for record separators
				int seperator_length = 0x10;
				buf.get(tmp);
				if (!LOG_IS_HOLUX_M241 && tmp[0] == (byte) 0xAA && tmp[1] == (byte) 0xAA
						&& tmp[2] == (byte) 0xAA && tmp[3] == (byte) 0xAA
						&& tmp[4] == (byte) 0xAA && tmp[5] == (byte) 0xAA
						&& tmp[6] == (byte) 0xAA && tmp[15] == (byte) 0xBB
						&& tmp[14] == (byte) 0xBB && tmp[13] == (byte) 0xBB
						&& tmp[12] == (byte) 0xBB){
					// So we found a record separator..
					Log("Found a record separator");

					// if open, close the current trk section
					try {if (!one_trk && gpx_in_trk) {
						WriteTrackEnd(gpx_writer);
						gpx_in_trk = false;}
					} catch (IOException e) {e.printStackTrace();}
					// It is possible that the log_format have changed, parse
					// out the new Log conditions
					buf.position(buf.position() - 9);
					byte seperator_type = buf.get();
					if (seperator_type == 0x02) {
						log_format = buf.getInt();
						buf.position(buf.position() + 4);
						Log(String.format("Log format has changed to %x", log_format));
					} else {
						buf.position(buf.position() + 8);}
					continue;
				} else if (String.valueOf(tmp).contains("HOLUXGR241LOGGER")) {
					LOG_IS_HOLUX_M241 = true;
					Log("Found a HOLUX M241 separator!");
					byte[] tmp4 = new byte[4];
					buf.get(tmp4);
					if (tmp4[0] == (byte) 0x20 && tmp4[1] == (byte) 0x20
							&& tmp4[2] == (byte) 0x20 && tmp4[3] == (byte) 0x20){
						Log("Found a HOLUX M241 1.3 firmware!");
					} else {
						buf.position(buf.position() - 4);}
					continue;
				}else if (Arrays.equals(tmp, emptyseparator)) {
					Log("Empty space, assume end of sector");
					break;
				} else {
					buf.position(buf.position() - seperator_length);}

				// So this is not a separator but it is an actual record, read it!
				record_count_sector++;

				int bytes_read = 0;
				long utc_time = 0;
				short valid = 0;
				double lat = 0;
				double lon = 0;
				float height = 0;
				float speed = 0;
				Log(String.format("Read record: %d of %d position %x", record_count_sector, nrOfRecordsInSector, buf.position()));
				if ((log_format & LOG_FORMAT_UTC) == LOG_FORMAT_UTC) {
					bytes_read += 4;
					utc_time = buf.getInt();
					Log(String.format("UTC time %d", utc_time));}
				if ((log_format & LOG_FORMAT_VALID) == LOG_FORMAT_VALID) {
					bytes_read += 2;
					valid = buf.getShort();
					Log(String.format("Valid %d", valid));}
				if ((log_format & LOG_FORMAT_LATITUDE) == LOG_FORMAT_LATITUDE) {
					if (LOG_IS_HOLUX_M241) {
						bytes_read += 4;
						lat = buf.getFloat();
					} else {
						bytes_read += 8;
						lat = buf.getDouble();}
					Log(String.format("Latitude %f", lat));}

				if ((log_format & LOG_FORMAT_LONGITUDE) == LOG_FORMAT_LONGITUDE) {
					if (LOG_IS_HOLUX_M241) {
						bytes_read += 4;
						lon = buf.getFloat();
					} else {
						bytes_read += 8;
						lon = buf.getDouble();}
					Log(String.format("Longitude %f", lon));}
				if ((log_format & LOG_FORMAT_HEIGHT) == LOG_FORMAT_HEIGHT) {
					if (LOG_IS_HOLUX_M241) {
						bytes_read += 3;
						byte[] tmp4 = new byte[4];
						buf.get(tmp4, 1, 3);
						ByteBuffer b = ByteBuffer.wrap(tmp4);
						b.order(ByteOrder.LITTLE_ENDIAN);
						height = b.getFloat();
					} else {
						bytes_read += 4;
						height = buf.getFloat();}
					Log(String.format("Height %f m", height));}
				if ((log_format & LOG_FORMAT_SPEED) == LOG_FORMAT_SPEED) {
					bytes_read += 4;
					speed = buf.getFloat() / 3.6f;
					Log(String.format("Speed %f m/s", speed));}
				if ((log_format & LOG_FORMAT_HEADING) == LOG_FORMAT_HEADING) {
					bytes_read += 4;
					float heading = buf.getFloat();
					Log(String.format("Heading %f", heading));}
				if ((log_format & LOG_FORMAT_DSTA) == LOG_FORMAT_DSTA) {
					bytes_read += 2;
					short dsta = buf.getShort();
					Log(String.format("DSTA %d", dsta));}
				if ((log_format & LOG_FORMAT_DAGE) == LOG_FORMAT_DAGE) {
					bytes_read += 4;
					int dage = buf.getInt();
					Log(String.format("DAGE %d", dage));}
				if ((log_format & LOG_FORMAT_PDOP) == LOG_FORMAT_PDOP) {
					bytes_read += 2;
					short pdop = buf.getShort();
					Log(String.format("PDOP %d", pdop));}
				if ((log_format & LOG_FORMAT_HDOP) == LOG_FORMAT_HDOP) {
					bytes_read += 2;
					short hdop = buf.getShort();
					Log(String.format("HDOP %d", hdop));}
				if ((log_format & LOG_FORMAT_VDOP) == LOG_FORMAT_VDOP) {
					bytes_read += 2;
					short vdop = buf.getShort();
					Log(String.format("VDOP %d", vdop));}
				if ((log_format & LOG_FORMAT_NSAT) == LOG_FORMAT_NSAT) {
					bytes_read += 2;
					byte nsat = buf.get();
					byte nsat_in_use = buf.get();
					Log(String.format("NSAT %d %d", (int) nsat, (int) nsat_in_use));}
				if ((log_format & LOG_FORMAT_SID) == LOG_FORMAT_SID) {
					// Large section to parse
					int satdata_count = 0;
					while (true) {
						bytes_read += 1;
						byte satdata_sid = buf.get();			
						bytes_read += 1;
						byte satdata_inuse = buf.get();
						bytes_read += 2;
						short satdata_inview = buf.getShort();

						Log(String.format("SID %d", (int) satdata_sid));
						Log(String.format("SID in use %d", (int) satdata_inuse));
						Log(String.format("SID in view %d", (int) satdata_inview));

						if (satdata_inview > 0) {
							if ((log_format & LOG_FORMAT_ELEVATION) == LOG_FORMAT_ELEVATION) {
								bytes_read += 2;
								short sat_elevation = buf.getShort();
								Log(String.format("Satellite ELEVATION %d", (int) sat_elevation));}
							if ((log_format & LOG_FORMAT_AZIMUTH) == LOG_FORMAT_AZIMUTH) {
								bytes_read += 2;
								short sat_azimuth = buf.getShort();
								Log(String.format("Satellite AZIMUTH %d", (int) sat_azimuth));}
							if ((log_format & LOG_FORMAT_SNR) == LOG_FORMAT_SNR) {
								bytes_read += 2;
								short sat_snr = buf.getShort();
								Log(String.format("Satellite SNR %d", (int) sat_snr));}
							satdata_count++;}
						if (satdata_count >= satdata_inview) {
							break;}
					}}
				if ((log_format & LOG_FORMAT_RCR) == LOG_FORMAT_RCR) {
					bytes_read += 2;
					short rcr = buf.getShort();
					Log(String.format("RCR %d", rcr));}
				if ((log_format & LOG_FORMAT_MILLISECOND) == LOG_FORMAT_MILLISECOND) {
					bytes_read += 2;
					short millisecond = buf.getShort();
					Log(String.format("Millisecond %d", millisecond));}
				if ((log_format & LOG_FORMAT_DISTANCE) == LOG_FORMAT_DISTANCE) {
					bytes_read += 8;
					double distance = buf.getDouble();
					Log(String.format("Distance %f", distance));}

				buf.position((buf.position() - bytes_read));
				byte[] tmp2 = new byte[bytes_read];
				buf.get(tmp2, 0, bytes_read);
				byte checksum = packet_checksum(tmp2, bytes_read);

				if (!LOG_IS_HOLUX_M241) {
					// Read the "*"
					buf.get();}
				// And the final character is the checksum count
				byte read_checksum = buf.get();
				Log(String.format("bytes_read %d Checksum %x read checksum %x", bytes_read, checksum, read_checksum));

				try {if (valid != VALID_NOFIX && checksum == read_checksum) {
					if (!gpx_in_trk) {
						WriteTrackBegin(gpx_writer, utc_time);
						gpx_in_trk = true;}
					formattedDate = WriteTrackPoint(gpx_writer, lat, lon, height, utc_time,	speed);}
				} catch (IOException e) {e.printStackTrace();}

				Double percentageCompleteSector = ( Double.valueOf(record_count_sector) / Double.valueOf(nrOfRecordsInSector)) * 100.0;
				if (sector_count > totalNrOfSectors) { //apparently this might happen..
					totalNrOfSectors=sector_count;}
				Double percentageCompleteTotal = 0.0;
				if (totalNrOfSectors>1) {
					Double totalNrOfRecords = (Double.valueOf(totalNrOfSectors))*log_count_fullywrittensector;
					percentageCompleteTotal=100.0 * Double.valueOf(record_count_total+record_count_sector)/totalNrOfRecords;}
				else if (totalNrOfSectors==1) {
					Double someTuning=0.5;
					percentageCompleteTotal=percentageCompleteSector / someTuning;}
				if (percentageCompleteTotal > 98.0) {
					percentageCompleteTotal = 99.0;}

//				Log.v(mtkutility.TAG,
				msg = "+++ ON Parse " + record_count_sector + " of " + nrOfRecordsInSector
						+ ". PercSect " + percentageCompleteSector.intValue() + ". Sector "
						+ sector_count + " of " + totalNrOfSectors + ". PercTot "
						+ percentageCompleteTotal.intValue() + " Track:" + gpx_trk_number;

				if (formattedDate.length() > 10 && oldPercentage != percentageCompleteTotal.intValue()) {
					sendPercentageConverted(percentageCompleteTotal.intValue());
//					sendTOAST(String.format("Sector %d of %d | Track %d | %s", sector_count, totalNrOfSectors, gpx_trk_number, formattedDate.substring(0, 10)));
					oldPercentage = percentageCompleteSector.intValue();}
			} // while (record_count_sector < log_count)

			record_count_total = record_count_total+nrOfRecordsInSector;
			if (bytes_in_sector < SIZEOF_SECTOR) {
				// Reached the end of the file or something is wrong
				Log(String.format("End of file!"));
				break;}
			// do a flush after each sector is read
			try {
				gpx_writer.flush();
				if (log_writer != null) {
					log_writer.flush();
				}
			} catch (IOException e) {
				sendTOAST("exception while flushing");
			}

		} // while reader.read

		// Close GPX file
		try {if (gpx_in_trk) {
			WriteTrackEnd(gpx_writer);
			gpx_in_trk = false;}
		WriteFooter(gpx_writer);
		gpx_writer.flush();
		gpx_writer.close();
		reader.close();
		} catch (IOException e) {e.printStackTrace();
		return;}

		// Close the Log file
		if (log_writer != null) {
			try {log_writer.flush();
			log_writer.close();
			} catch (IOException e) {e.printStackTrace();}}

		sendPercentageConverted(100);
//		sendMessageToMessageField("Finished converting to GPX");
		sendCloseProgress();

		Log.v(TAG, "+++ GPS converting finished");
		return;
	}

	private void WriteHeader(BufferedWriter writer) throws IOException {
//        mL.mLog(mL.VB1, "ParseBinFile.WriteHeader()");
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n"
				+ "<gpx\n"
				+ "    version=\"1.1\"\n"
				+ "    creator=\"AndroidMTK - http://www.bastiaannaber.com\"\n"
				+ "    xmlns=\"http://www.topografix.com/GPX/1/1\"\n"
				+ "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
				+ "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");
	}

	private void WriteFooter(BufferedWriter writer) throws IOException {
//        mL.mLog(mL.VB1, "ParseBinFile.WriteFooter()");
		writer.write("</gpx>\n");
	}

	private void WriteTrackBegin(BufferedWriter writer, long time) throws IOException {
//        mL.mLog(mL.VB1, "ParseBinFile.WriteTrackBegin()");
		java.util.Date date = new java.util.Date(time * 1000);

//		writer.write("<trk>\n <name>" + formatter.format(date)
		writer.write("<trk>\n <name>" + formatter.format(add1024w(date))    //dyj
				+ " </name>\n <number>" + gpx_trk_number + "</number>\n<trkseg>\n");
		gpx_trk_number++;
	}

	private String WriteTrackPoint(BufferedWriter writer, double lat, double lon, double height, long time, double speed) throws IOException {
//        mL.mLog(mL.VB1, "ParseBinFile.WriteTrackPoint()");
		java.util.Date date = new java.util.Date(time * 1000);
//		String formattedDate = formatter.format(date);
		String formattedDate = formatter.format(add1024w(date));    //dyj
		Log(String.format("formattedDate %s", formattedDate));
		writer.write(String
				.format(Locale.US,
						"<trkpt lat=\"%.9f\" lon=\"%.9f\">\n  <ele>%.6f</ele>\n  <time>%s</time>\n  <speed>%.6f</speed>\n</trkpt>\n",
						lat, lon, height, formattedDate, speed));
		return formattedDate;
	}

	private void WriteTrackEnd(BufferedWriter writer) throws IOException {
//        mL.mLog(mL.VB1, "ParseBinFile.WriteTrackEnd()");
		writer.write("</trkseg>\n</trk>\n");
	}

	private void Log(String text) {
//        mL.mLog(mL.VB1, "ParseBinFile.Log() +++ " + text);
//		if (log_writer != null) {
//			try {log_writer.append(text);
//			log_writer.append('\n');
//			log_writer.flush();
//			} catch (IOException e) {e.printStackTrace();}}
	}

	private byte packet_checksum(byte[] array, int length) {
		byte check = 0;
		int i;

		for (i = 0; i < length; i++) {
			check ^= array[i];}
		return check;
	}

	private void sendPercentageConverted(int percentageComplete) {
//        mL.mLog(mL.VB1, "ParseBinFile.sendPercentageConverted()");
//        mL.mLog(mL.VB1, "GetLogFragment.onViewCreated()");
		Message msg = parseHandler.obtainMessage();
		Bundle b = new Bundle();
		b.putInt(MakeGPXFragment.KEY_PROGRESS, percentageComplete);
		msg.setData(b);
		parseHandler.sendMessage(msg);
	}

	private void sendTOAST(String message) {
//        mL.mLog(mL.VB1, "ParseBinFile.sendTOAST()");
		if (message.equals(oldToastmessage)) {
			return;}
		oldToastmessage = message;
		Message msg = parseHandler.obtainMessage();
		Bundle b = new Bundle();
//		b.putString(logDnldFragment.KEY_TOAST, message);
		msg.setData(b);
		parseHandler.sendMessage(msg);
	}

	private void sendMessageToMessageField(String message) {
//        mL.mLog(mL.VB1, "ParseBinFile.sendMessageToMessageField()");
		Message msg = parseHandler.obtainMessage();
		Bundle b = new Bundle();
//		b.putString(logDnldFragment.MESSAGEFIELD, message);
		msg.setData(b);
		parseHandler.sendMessage(msg);
	}

	private void sendCloseProgress() {
//        mL.mLog(mL.VB1, "ParseBinFile.sendCloseProgress()");
		Message msg = parseHandler.obtainMessage();
		Bundle b = new Bundle();
		b.putInt(MakeGPXFragment.CLOSE_PROGRESS, 1);
		msg.setData(b);
		parseHandler.sendMessage(msg);
	}

	@Override
    public void run() {
//        mL.mLog(mL.VB1, "ParseBinFile.run()");
        try {
            doConvert();
        } catch (IOException e) {
            e.printStackTrace();
//            sendMessageToMessageField("Couldn't convert GPS Log to gpx");
        }
    }

	private java.util.Date add1024w(java.util.Date oldDate)    //dyj
	{
		Calendar today = Calendar.getInstance();
		Calendar newCal= Calendar.getInstance();
		newCal.setTime(oldDate);
		newCal.add(Calendar.DATE, 7168); // add 7168 days
		if(newCal.after(today))
			return oldDate;
		else
			return newCal.getTime();
	}
}
