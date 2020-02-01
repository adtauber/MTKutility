package com.adtdev.mtkutility;
/**
 * @author Alex Tauber
 * <p>
 * This file is part of the open source Android app MTKutility2. You can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 3 of the License. This extends to files included that were authored by
 * others and modified to make them suitable for this app. All files included were subject to
 * open source licensing.
 * <p>
 * MTKutility2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You can review a copy of the
 * GNU General Public License at http://www.gnu.org/licenses.
 * <p>
 * GetEPOFragment
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.adtdev.fileChooser.FileChooser;

import static android.app.Activity.RESULT_OK;


//import adtdev.com.mtkutility.R;

public class MakeGPXFragment extends Fragment {
    private static final int SIZEOF_SECTOR = 0x10000;
    // Log format is stored as a bitmask field.
    private static final int FORMAT_UTC = 0x00000001;
    private static final int FORMAT_VALID = 0x00000002;
    private static final int FORMAT_LATITUDE = 0x00000004;
    private static final int FORMAT_LONGITUDE = 0x00000008;
    private static final int FORMAT_HEIGHT = 0x00000010;
    private static final int FORMAT_SPEED = 0x00000020;
    private static final int FORMAT_HEADING = 0x00000040;
    private static final int FORMAT_DSTA = 0x00000080;
    private static final int FORMAT_DAGE = 0x00000100;
    private static final int FORMAT_PDOP = 0x00000200;
    private static final int FORMAT_HDOP = 0x00000400;
    private static final int FORMAT_VDOP = 0x00000800;
    private static final int FORMAT_NSAT = 0x00001000;
    private static final int FORMAT_SID = 0x00002000;
    private static final int FORMAT_ELEVATION = 0x00004000;
    private static final int FORMAT_AZIMUTH = 0x00008000;
    private static final int FORMAT_SNR = 0x00010000;
    private static final int FORMAT_RCR = 0x00020000;
    private static final int FORMAT_MILLISECOND = 0x00040000;
    private static final int FORMAT_DISTANCE = 0x00080000;

    private static final int VALID_NOFIX = 0x0001;
    private boolean HOLUX_M241 = false;
    private boolean M241_1_3_firmware = false;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private String wrkHeader = "UTC,valid,lat,lon,height,speed,heading,DSTA,DAGE,PDOP,HDOP,VDOP,sat,inuse,SID,RCR,miliSec,distance,rChk,cChk,trk,mask\n";
    private String csvHeader = "TRACK,INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT(m)," +
            "SPEED(km/h),HEADING,DSTA,DAGE,PDOP,HDOP,VDOP,NSAT (USED/VIEW),DISTANCE(m)," +
            "SAT INFO (SID-ELE-AZI-SNR)\n";
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final SimpleDateFormat fnFormatter = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
    private final SimpleDateFormat tnFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat wpFormatter = new SimpleDateFormat("dd-MMM-yy HH:mm:ss");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat csvDate = new SimpleDateFormat("M/d/yyyy");
    private final SimpleDateFormat csvTime = new SimpleDateFormat("HH:mm:ss");


    private String fileNamePrefix;
    private Date fileDate;
//    private static final int MakeGPX = 0;
//    private static final int MakeKML = 1;
//    private static final int MakeCSV = 2;

    private static final int GPXwpt = 1;
    private static final int GPXtrk = 2;
    private byte[] emptyseparator = new byte[0x10];
    private boolean gpx_in_trk;
    private boolean logStoped = false;
    private int formatMask = 0;
    private int record = 0;
    private int bytes_in_sector = 0;
    private int gpx_trk_number = 0;
    private long totalBytes = 0;
    private long sectorBytes = 0;
    private byte[] buffer = new byte[SIZEOF_SECTOR];
    private int count = 0;
    private java.util.Date mDate;
//    private String sDate;

    private String[] cells;
    private String mLine;
    private BufferedReader reader = null;

    private long utcTime = 0;
    private short valid = 0;
    private double lat = 0;
    private String latS;
    private double lon = 0;
    private String lonS;
    private float height = 0;
    private float heading = 0;
    private float speed = 0;
    private short dsta = 0;
    private int dage = 0;
    private short pdop = 0;
    private short hdop = 0;
    private short vdop = 0;
    private byte nsat = 0;
    private byte nsatInuse = 0;
    private String satElevation;
    private String satAzimuth;
    private String satSNR;
    private String SID;
    private String SIDdata;
    private short rcr = 0;
    private String rcrS = "";
    private boolean noRCR = false;
    private short millisecond = 0;
    private double distance = 0;
    private byte cChecksum = 0;
    private byte rChecksum = 0;
    private String tmpString;
    private String formattedDate;
    private int wayRec = 0;
    private int trkRec = 1;
    private int linesOut = 0;
    private String savedTrk = "0";
    private long lastUTC;

    private String in1 = "  ";
    private String in2 = "    ";
    private String in3 = "      ";
    private String in4 = "        ";

    //    private myLibrary mL;
    private boolean cbxone;
    private static final int doLOCAL = 0;
    private static String NL = System.getProperty("line.separator");

    private boolean logFileIsOpen;
    private SharedPreferences publicPrefs;
    private SharedPreferences.Editor publicPrefEditor;
    private SharedPreferences appPrefs;
    private SharedPreferences.Editor appPrefEditor;
    private static Context mContext = Main.mContext;
    private int debugLVL;
    private final int ABORT = Main.ABORT;
    private static final int noTrunc = Main.noTrunc;
    private int pct = 0;
    private File bin_file;
    private int binLength = 0;
    private long csvLength = 0;
    private long bytesRead;
    private long bytesToRead;
    private OutputStreamWriter logWriter = Main.logWriter;
    private BufferedWriter mWriter = null;
    private BufferedInputStream mReader = null;
    private File gpxFile;
    private File kmlFile;
    private File csvFile;

    private Button getfile;
    private Button makeGPX;
    private Button makeKML;
    private Button makeCSV;
    private TextView fileName;
    private CheckBox cbxOne;
    private EditText trkSecs;
    private View rootView;
    private ScrollView mSv;
    private TextView mTv;

    private ProgressDialog dialog;
    private String binPath;

    // Keys
    public static final int REQUEST_PATH = 1;
    public static final String KEY_TOAST = "toast";
    public static final String MESSAGEFIELD = "textSwitcher";
    public static final String KEY_PROGRESS = "progressCompleted";
    public static final String CLOSE_PROGRESS = "closeProgressDialog";
    public static final String CHANGE_DIALOG = "changeDialog";

    private String wF = "/trackpoints.csv";
    private File wrkFile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logFileIsOpen = Main.logFileIsOpen;
        mLog(0, "MakeGPXFragment.onCreateView()");
        publicPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        publicPrefEditor = publicPrefs.edit();
        appPrefs = mContext.getSharedPreferences("otherprefs", Context.MODE_PRIVATE);
        appPrefEditor = appPrefs.edit();

        wrkFile = new File(mContext.getFilesDir(), wF);
//        wrkFile = new File(appPrefs.getString("basePath", ""), wF);

        rootView = inflater.inflate(R.layout.makegpx, container, false);
        fileName = rootView.findViewById(R.id.fileName);
        cbxOne = rootView.findViewById(R.id.cbxOne);
        trkSecs = rootView.findViewById(R.id.trkSecs);
        mSv = rootView.findViewById(R.id.mSv);
        mTv = rootView.findViewById(R.id.mTv);

        trkSecs.setText(String.valueOf(appPrefs.getInt("trkSecs", 0) / 60));

        cbxOne.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mLog(0, "*** MakeGPXFragment.onCheckedChanged() *** allow insecure checkbox changed");
                cbxone = isChecked;
            }
        });

        //@formatter:off
        trkSecs.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (trkSecs.getText().length() > 0)
                    appPrefEditor.putInt("trkSecs", Integer.parseInt(trkSecs.getText().toString()) * 60).apply();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });//@formatter:on

        getfile = rootView.findViewById(R.id.getfile);
        getfile.setTransformationMethod(null);
        getfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "*** MakeGPXFragment.onCreateView() *** button " + getfile.getText() + " pressed");
                getfile();
            }
        });

        makeGPX = rootView.findViewById(R.id.makeGPX);
        makeGPX.setTransformationMethod(null);
        makeGPX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "*** MakeGPXFragment.onCreateView() *** button " + makeGPX.getText() + " pressed");
                new MakeGPXFragment.makeGPX(getActivity()).execute();
            }
        });

        makeKML = rootView.findViewById(R.id.makeKML);
        makeKML.setTransformationMethod(null);
        makeKML.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "*** MakeGPXFragment.onCreateView() *** button " + makeKML.getText() + " pressed");
                new MakeGPXFragment.makeKML(getActivity()).execute();
            }
        });

        makeCSV = rootView.findViewById(R.id.makeCSV);
        makeCSV.setTransformationMethod(null);
        makeCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLog(0, "*** MakeGPXFragment.onCreateView() *** button " + makeGPX.getText() + " pressed");
                new MakeGPXFragment.makeCSV(getActivity()).execute();
            }
        });

        return rootView;
    } //onCreateView()

    public void onViewCreated(View view, Bundle savedInstanceState) {
        mLog(0, "MakeGPXFragment.onViewCreated()");
        debugLVL = Integer.parseInt(publicPrefs.getString("debugLVL", "0"));
        makeGPX.setEnabled(false);
        makeKML.setEnabled(false);
        makeCSV.setEnabled(false);
        getfile.requestFocus();

        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        fnFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        tnFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        wpFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        csvDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        csvTime.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (int i = 0; i < 0x10; i++) {
            emptyseparator[i] = (byte) 0xFF;
        }
    } //onViewCreated()

    // Listen for results.
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // See which child activity is calling us back.
        if (requestCode == REQUEST_PATH) {
            if (resultCode == RESULT_OK) {
                String binFile = data.getStringExtra("GetFileName");
                if (binFile.contains(".bin")) {
                    binPath = data.getStringExtra("GetPath");
                    fileName.setText(binPath);
                    //short pause to let app finish file enquiry
                    goSleep(300);
                    mLog(0, "**** selected: " + binPath);
                    new MakeGPXFragment.makeWrkFile(getActivity()).execute();
                }
            }
        }
    } //onActivityResult()

    private java.util.Date add1024toDate(java.util.Date oldDate) {   //dyj
        mLog(3, "MakeGPXFragment.add1024toDate()");
        Calendar today = Calendar.getInstance();
        Calendar newDay = Calendar.getInstance();
        newDay.setTime(oldDate);
        newDay.add(Calendar.DATE, 7168); // add 7168 days
        if (newDay.after(today))
            return oldDate;
        else
            return newDay.getTime();
    } //add1024toDate()

    private byte calcCheckSum(byte[] array, int length) {
        mLog(3, "MakeGPXFragment.calcCheckSum()");
        byte check = 0;
        int i;

        for (i = 0; i < length; i++) {
            if (M241_1_3_firmware && i == 15) {
                continue;
            }
            check ^= array[i];
        }
        return check;
    } //calcCheckSum()

    private void fileWriter(String text) {
        mLog(3, "MakeGPXFragment.fileWriter()");
        try {
            mWriter.write(text);
            mWriter.flush();
        } catch (IOException e) {
            mLog(0, "**** wrkFileWriter.write() failed");
//            ReaderClose();
            Main.buildCrashReport(e);
        }
    } //fileWriter()

    private void getfile() {
        mLog(0, "MakeGPXFragment.getfile()");
        binPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        binPath = binPath + "/mtkutility/bin";
        Intent intent = new Intent(mContext, FileChooser.class);
        intent.putExtra("method", doLOCAL);
        intent.putExtra("root", "/storage");
        intent.putExtra("start", binPath);
        intent.putExtra("selfolders", false);
        intent.putExtra("selfiles", true);
        intent.putExtra("showhidden", false);
        startActivityForResult(intent, REQUEST_PATH);
    } //getfile()

    private String getGPXend(int type) {
        if (type == GPXwpt) {
            return in2 + "</wpt>\n";
        } else {
            return in2 + "</trkpt>\n";
        }
    } //getGPXend()

    private String getRCRs(short rcr) {
        switch (rcr) {
            case 1:
                return "T";
            case 2:
                return "S";
            case 3:
                return "TS";
            case 4:
                return "D";
            case 5:
                return "TD";
            case 6:
                return "SD";
            case 7:
                return "TSD";
            case 8:
                return "B";
            case 9:
                return "TB";
            case 10:
                return "SB";
            case 11:
                return "TSB";
            case 12:
                return "DB";
            case 13:
                return "TDB";
            case 14:
                return "SDB";
            case 15:
                return "TDSB";
            default:
                return String.valueOf(rcr);
        }
    } //getRCRs()

    private String getRecNum(int type) {
        mLog(3, "MakeGPXFragment.getRecNum()");
        if (type == GPXwpt) {
            wayRec++;
            return "WP" + String.format("%07d", wayRec);
        } else {
            trkRec++;
            return "TP" + String.format("%07d", trkRec);
        }
    } //getRecNum()

    private String getValid(short valid) {
        mLog(3, "MakeGPXFragment.getValid()");
        switch (valid) {
            case 1:
                return "nofix";
            case 2:
                return "SPS";
            case 4:
                return "DGPS";
            case 8:
                return "PPS";
            case 16:
                return "RTK";
            case 32:
                return "FRTK";
            case 64:
                return "est";
            default:
                return String.valueOf(valid);
        }
    } //getValid()

    public void goSleep(int mSec) {
        mLog(3, String.format("MakeGPXFragment.goSleep(%d)", mSec));
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
            Main.buildCrashReport(e);
        }
    } //goSleep()

    private void scrollDown() {
        mSv.post(new Runnable() {
            public void run() {
                mSv.fullScroll(View.FOCUS_DOWN);
            }
        });
    } //scrollDown()

    private void sendToast(final String msg) {
        mLog(3, "MakeGPXFragment.sendToast()");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            }
        });
    } //sendToast()

    private void mLog(int mode, String msg) {
        if (logFileIsOpen) {
            if (mode == ABORT) sendToast(msg);
            Main.mLog(mode, msg);
        }
    } //Log()

    private String bytesToHex(byte[] bytes) {
        mLog(3, "MakeGPXFragment.bytesToHex()");
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    } //bytesToHex()

    private class makeWrkFile extends AsyncTask<Void, Integer, Void> {
        private Context lContext;

        public makeWrkFile(Context context) {
            mLog(1, "MakeGPXFragment.makeWrkFile.makeWrkFile()");
            lContext = context;
        } //makeWrkFile()

        @Override
        protected void onPreExecute() {
            mLog(1, "MakeGPXFragment.makeWrkFile.onPreExecute()");
            dialog = new ProgressDialog(lContext);
            dialog.setCancelable(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(100);
            dialog.setTitle(getString(R.string.readBIN));
            dialog.show();
        } //onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            mLog(1, "MakeGPXFragment.makeWrkFile.doInBackground()");
            // Open an input stream for reading from the binary Log
            bin_file = new File(binPath);
            mLog(1, String.format("Reading bin file: %s", bin_file.toString()));
            mLog(1, "creating work file: " + wrkFile.toString());

            FileWriter mFileWriter;
            try {
                mFileWriter = new FileWriter(wrkFile);
                mWriter = new BufferedWriter(mFileWriter, SIZEOF_SECTOR / 8);
                FileInputStream mInputstream = new FileInputStream(bin_file);
                mReader = new BufferedInputStream(mInputstream, SIZEOF_SECTOR);
                fileWriter(wrkHeader);
                BinToCSVconvert();
            } catch (IOException e) {
                mLog(0, "**** csv pass failed");
                Main.buildCrashReport(e);
            }
            // Close files
            try {
                mLog(2, "closing files");
                mWriter.flush();
                mWriter.close();
                mReader.close();
            } catch (IOException e) {
                mLog(0, "**** makeWrkFile.doInBackground() IOException-files close");
                Main.buildCrashReport(e);
            }
            csvLength = wrkFile.length();
            return null;
        } //doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(1, "MakeGPXFragment.makeWrkFile.onPostExecute()");
            totalBytes = binLength = 0;
            if (dialog.isShowing()) dialog.dismiss();
            getfile.setEnabled(false);
            makeGPX.setEnabled(true);
            makeKML.setEnabled(true);
            makeCSV.setEnabled(true);
        } //onPostExecute()


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        } //onProgressUpdate

        private void BinToCSVconvert() throws IOException {
            mLog(1, "MakeGPXFragment.makeWrkFile.BinToCSVconvert()");
            gpx_trk_number = 0;
            binLength = (int) bin_file.length();

            while ((bytes_in_sector = mReader.read(buffer, 0, SIZEOF_SECTOR)) > 0) {
                ByteBuffer buf = ByteBuffer.wrap(buffer);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.position(0x0);

                formatMask = buf.getInt(2);
                BinSectorParse(buf);
                mLog(2, String.format("processed %d of %d bytes", totalBytes, binLength));

                if (bytes_in_sector < SIZEOF_SECTOR) {
                    // Reached the end of the file or something is wrong
                    mLog(1, "End of file!");
                    break;
                }
                // do a flush after each sector is read
                try {
                    mWriter.flush();
                } catch (IOException e) {
                    //do nothing
                }
            } // while mBufferedInput.read
        } //BinToCSVconvert()

        private void BinSectorParse(ByteBuffer buf) {
            mLog(2, "MakeGPXFragment.makeWrkFile.BinSectorParse()");
            String s, x;
            logStoped = false;
            // Skip the header (which is 0x200 bytes long)
            buf.position(0x200);
            sectorBytes = buf.position();
            while ((sectorBytes + 0x10) < bytes_in_sector) {
                int bytesRead = 0;
                byte[] tmp = new byte[0x10];
                // Test for record separators
                int separator_length = 0x10;
                buf.get(tmp);
                s = new String(tmp);
                x = bytesToHex(tmp);
                mLog(3, s);
                //check for a record separator - format:
                // AA AA AA AA AA AA AA 00 00 00 00 00 BB BB BB BB
                //record separator types - first byte after AA AA AA AA AA AA AA
                // LOG BITMASK    = 0x02;
                // LOG PERIOD     = 0x03;
                // LOG DISTANCE   = 0x04;
                // LOG SPEED      = 0x05;
                // OVERLAP/STOP   = 0x06;
                // START/STOP LOG = 0x07;
                if (tmp[0] == (byte) 0xAA && tmp[1] == (byte) 0xAA &&
                        tmp[2] == (byte) 0xAA && tmp[3] == (byte) 0xAA &&
                        tmp[12] == (byte) 0xBB && tmp[13] == (byte) 0xBB &&
                        tmp[14] == (byte) 0xBB && tmp[15] == (byte) 0xBB) {
                    // So we found a record separator..
                    byte separator_type = tmp[7];
                    mLog(2, String.format("Record separator %s", bytesToHex(tmp)));
                    //skip record when log is stopped - look for log start separator
                    if (tmp[7] == 0x07 && tmp[8] == 0x04) {
                        logStoped = true;
                        mLog(1, String.format("logStoped %b", logStoped));
                    }
                    if (tmp[7] == 0x07 && tmp[8] == 0x06) {
                        logStoped = false;
                        mLog(1, String.format("logStoped %b", logStoped));
                    }
                    if (!cbxone && gpx_in_trk && tmp[7] == 0x07 && tmp[8] == 0x06) {
                        //log start separator found - start a new track
                        gpx_in_trk = false;
                        //Holux M-241 sometimes logs a track point before the log start separator
                        if (HOLUX_M241 && record < 3) gpx_in_trk = true;
                    }

                    // It is possible that the formatMask have changed, parse
                    // out the new Log conditions
                    buf.position(buf.position() - 8);
                    if (separator_type == 0x02) {
                        formatMask = buf.getInt();
                        buf.position(buf.position() + 4);
                        mLog(2, String.format("Log format has changed to %x", formatMask));
                    } else {
                        buf.position(buf.position() + 8);
                    }
                    sectorBytes = buf.position();
                    continue;
                } else if (s.contains("HOLUXGR241LOGGER")) {
                    HOLUX_M241 = true;
                    mLog(2, "Found a HOLUX M241 separator!");
                    byte[] tmp4 = new byte[4];
                    buf.get(tmp4);
                    if (tmp4[0] == (byte) 0x20 && tmp4[1] == (byte) 0x20
                            && tmp4[2] == (byte) 0x20 && tmp4[3] == (byte) 0x20) {
                        mLog(2, "Found a HOLUX M241 1.3 firmware!");
                        M241_1_3_firmware = true;
                    } else {
                        buf.position(buf.position() - 4);
                    }
                    sectorBytes = buf.position();
                    continue;
                } else if (Arrays.equals(tmp, emptyseparator)) {
                    mLog(2, "Empty space, assume end of log");
                    //clear bytes_in_sector to exit BinToCSVconvert loop
//                    bytes_in_sector = 0;
                    break;

                } else {
                    if (logStoped) {
                        buf.position(buf.position() - separator_length + 1);
                        sectorBytes = buf.position();
                        continue;
                    } else
                        buf.position(buf.position() - separator_length);
                }

                sectorBytes = buf.position();
                // So this is not a separator but it is an actual record, read it!
                if (!gpx_in_trk) {
                    gpx_in_trk = true;
                    if (!cbxone) gpx_trk_number++;
                }

                // build csv work file line while parsing binary file
                StringBuilder csvLine = new StringBuilder(300);
                utcTime = 0;
                if ((formatMask & FORMAT_UTC) == FORMAT_UTC) {
                    bytesRead += 4;
                    utcTime = buf.getInt();
                    csvLine.append(String.format("%d", utcTime));
                }

                csvLine.append(",");
                valid = 0;
                if ((formatMask & FORMAT_VALID) == FORMAT_VALID) {
                    bytesRead += 2;
                    valid = buf.getShort();
                }
                csvLine.append(String.format("%d", valid)); //fix)

                csvLine.append(",");
                lat = 0;
                if ((formatMask & FORMAT_LATITUDE) == FORMAT_LATITUDE) {
                    if (HOLUX_M241) {
                        bytesRead += 4;
                        lat = buf.getFloat();
                    } else {
                        bytesRead += 8;
                        lat = buf.getDouble();
                    }
                    csvLine.append(String.format("%.9f", lat));
                }

                lon = 0;
                csvLine.append(",");
                if ((formatMask & FORMAT_LONGITUDE) == FORMAT_LONGITUDE) {
                    if (HOLUX_M241) {
                        bytesRead += 4;
                        lon = buf.getFloat();
                    } else {
                        bytesRead += 8;
                        lon = buf.getDouble();
                    }
                    csvLine.append(String.format("%.9f", lon));
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_HEIGHT) == FORMAT_HEIGHT) {
                    if (HOLUX_M241) {
                        bytesRead += 3;
                        byte[] tmp4 = new byte[4];
                        buf.get(tmp4, 1, 3);
                        ByteBuffer b = ByteBuffer.wrap(tmp4);
                        b.order(ByteOrder.LITTLE_ENDIAN);
                        height = b.getFloat();
                    } else {
                        bytesRead += 4;
                        height = buf.getFloat();
                    }
                    csvLine.append(String.format("%.9f", height));
                }

                if (M241_1_3_firmware) {
                    bytesRead += 1;
                    rChecksum = buf.get();
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_SPEED) == FORMAT_SPEED) {
                    bytesRead += 4;
                    if (M241_1_3_firmware) {
                        byte[] tmp4 = new byte[4];
                        buf.get(tmp4, 1, 3);
                        ByteBuffer b = ByteBuffer.wrap(tmp4);
                        b.order(ByteOrder.LITTLE_ENDIAN);
                        speed = b.getFloat() / 3.6f;
                        buf.get();
                    } else {
                        speed = buf.getFloat() / 3.6f;
                    }
                    csvLine.append(String.format("%.6f", speed));
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_HEADING) == FORMAT_HEADING) {
                    bytesRead += 4;
                    heading = buf.getFloat();
                    csvLine.append(String.format("%.6f", heading));
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_DSTA) == FORMAT_DSTA) {
                    bytesRead += 2;
                    dsta = buf.getShort();
                    csvLine.append(String.format("%d", dsta));
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_DAGE) == FORMAT_DAGE) {
                    bytesRead += 4;
                    dage = buf.getInt();
                    csvLine.append(String.format("%d", dage));
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_PDOP) == FORMAT_PDOP) {
                    bytesRead += 2;
                    pdop = buf.getShort();
                    csvLine.append(String.format("%d", pdop));
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_HDOP) == FORMAT_HDOP) {
                    bytesRead += 2;
                    hdop = buf.getShort();
                    csvLine.append(String.format("%d", hdop));
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_VDOP) == FORMAT_VDOP) {
                    bytesRead += 2;
                    vdop = buf.getShort();
                    csvLine.append(String.format("%d", vdop));
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_NSAT) == FORMAT_NSAT) {
                    bytesRead += 2;
                    nsat = buf.get();
                    nsatInuse = buf.get();
                    csvLine.append(String.format("%d", nsat));
                    csvLine.append(",");
                    csvLine.append(String.format("%d", nsatInuse));
                } else {
                    csvLine.append(",");
                }

                csvLine.append(",");
                boolean doSID = false;
                if ((formatMask & FORMAT_SID) == FORMAT_SID) {
                    // Large section to parse
                    //#05-18-280-22
                    int satCount = 0;
                    SIDdata = "";
                    while (true) {
                        satElevation = "";
                        satAzimuth = "";
                        satSNR = "";
                        bytesRead += 1;
                        SID = String.format("#%d", buf.get());
                        bytesRead += 1;
                        byte satdataInuse = buf.get();
                        bytesRead += 2;
                        short satdataInview = buf.getShort();
                        if (satdataInview > 0) {
                            doSID = true;
                            if ((formatMask & FORMAT_ELEVATION) == FORMAT_ELEVATION) {
                                bytesRead += 2;
                                satElevation = String.format("%s", Short.toString(buf.getShort()));
                            }
                            if ((formatMask & FORMAT_AZIMUTH) == FORMAT_AZIMUTH) {
                                bytesRead += 2;
                                satAzimuth = String.format("%s", Short.toString(buf.getShort()));
                            }
                            if ((formatMask & FORMAT_SNR) == FORMAT_SNR) {
                                bytesRead += 2;
                                satSNR = String.format("%s", Short.toString(buf.getShort()));
                            }
                            //#05-18-280-22
                            satCount++;
                            SIDdata = SIDdata + ";" + SID + "-" + satElevation + "-" + satAzimuth + "-" + satSNR;
                        }
                        if (satCount >= satdataInview) {
                            break;
                        }
                    }
                    if (SIDdata.length() > 8) {
                        SIDdata = SIDdata.substring(1);
                        csvLine.append(SIDdata);
                    }
                }

                csvLine.append(",");
                rcr = 0;
                if ((formatMask & FORMAT_RCR) == FORMAT_RCR) {
                    bytesRead += 2;
                    rcr = buf.getShort();
                }
                csvLine.append(String.format("%d", rcr));

                csvLine.append(",");
                if ((formatMask & FORMAT_MILLISECOND) == FORMAT_MILLISECOND) {
                    bytesRead += 2;
                    millisecond = buf.getShort();
                    csvLine.append(String.format("%d", millisecond));
                }

                csvLine.append(",");
                if ((formatMask & FORMAT_DISTANCE) == FORMAT_DISTANCE) {
                    bytesRead += 8;
                    distance = buf.getDouble();
                    csvLine.append(String.format("%.6f", distance));
                }

                record++;
                buf.position((buf.position() - bytesRead));
                byte[] tmp2 = new byte[bytesRead];
                buf.get(tmp2, 0, bytesRead);
                String str = bytesToHex(tmp2);
                mLog(3, String.valueOf(record) + ": " + str);
                cChecksum = calcCheckSum(tmp2, bytesRead);

                if (!HOLUX_M241) {
                    // Read the "*"
                    buf.get();
                    bytesRead += 1;
                }
                if (!M241_1_3_firmware) {
                    // And the final character is the cChecksum count
                    rChecksum = buf.get();
                    bytesRead += 1;
                }
                sectorBytes += bytesRead;
                totalBytes += bytesRead;
                csvLine.append(",");
                csvLine.append(String.format("%d", rChecksum));

                csvLine.append(",");
                csvLine.append(String.format("%d", cChecksum));

                csvLine.append(",");
                csvLine.append(String.format("%d", gpx_trk_number));

                csvLine.append(",");
                csvLine.append(String.format("%d", formatMask));

                //save date for file name
                if (record == 1) {
                    fileDate = new java.util.Date(utcTime * 1000);
                    fileNamePrefix = fnFormatter.format(add1024toDate(fileDate));    //dyj
                }

                pct = (int) ((totalBytes * 100) / binLength);
                if (pct > 100) pct = 100;
                dialog.setProgress(pct);
                mLog(3, String.format("bytesRead %d Checksum %x read cChecksum %x",
                        bytesRead, cChecksum, rChecksum));

                if (valid > 1024) {
                    sectorBytes = bytes_in_sector + 99;
                    continue;
                }
                if (rcr > 16) continue;
                if (utcTime == 0) continue;

                mLog(2, String.valueOf(record) + ": " + csvLine.toString());
                csvLine.append("\n");
                fileWriter(csvLine.toString());
            } // while (sectorrecordRead < log_count)
        } //BinSectorParse()
    }

    private class makeGPX extends AsyncTask<Void, Integer, Void> {

        private Context lContext;
        private int trksecs;

        public makeGPX(Context context) {
            mLog(2, "MakeGPXFragment.makeGPX.makeGPX()");
            lContext = context;
        } //makeGPX()

        @Override
        protected void onPreExecute() {
            mLog(2, "MakeGPXFragment.makeGPX.onPreExecute()");
            trksecs = appPrefs.getInt("trkSecs", 0);
            if (trksecs > 0) trksecs = trksecs * 60;
            dialog = new ProgressDialog(lContext);
            dialog.setCancelable(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(100);
            dialog.setTitle(getString(R.string.writeGPX));
            dialog.show();
            //set bytesToRead 2 csv file passes
            bytesToRead = csvLength * 2;
        } //onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            mLog(2, "MakeGPXFragment.makeGPX.doInBackground()");
            // Open an output for writing the gpx file
            String gpxName = fileNamePrefix + ".gpx"; // - date from first record in file
            gpxFile = new File(appPrefs.getString("gpxPath", ""), gpxName);
            mLog(1, "Creating GPX file: " + gpxFile.toString());
            FileWriter mFileWriter;
            try {
                mFileWriter = new FileWriter(gpxFile);
                mWriter = new BufferedWriter(mFileWriter, SIZEOF_SECTOR / 8);
            } catch (IOException e) {
                mLog(0, "**** makeGPX.doInBackground() IOException-mWriter create");
                Main.buildCrashReport(e);
            }
            bytesRead = 0;
            writeHeader();
            writeGPXfile(GPXwpt);
            linesOut = 0;
            savedTrk = "0";
            writeGPXfile(GPXtrk);
            fileWriter(in1 + "</trkseg>\n</trk>\n</gpx>\n");
            // Close files
            try {
                mLog(2, "closing files");
                mWriter.flush();
                mWriter.close();
                mReader.close();
            } catch (IOException e) {
                mLog(0, "**** makeGPX.doInBackground() IOException-files close");
                Main.buildCrashReport(e);
            }
            return null;
        } //doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(2, "MakeGPXFragment.makeGPX.onPostExecute()");
//            goSleep(2000);
            if (dialog.isShowing()) dialog.dismiss();
            mTv.append("created: " + gpxFile.toString() + "\n");
            scrollDown();
            makeGPX.setEnabled(false);
            wayRec = trkRec = 0;
        } //onPostExecute()

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        } //onProgressUpdate

        private void writeHeader() {
            mLog(2, "MakeGPXFragment.makeGPX.writeHeader()");
            fileWriter("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n");
            fileWriter("<gpx\n");
            fileWriter("    version=\"1.1\"\n");
            fileWriter("    creator=\"MTKutility - adt.androidapps@gmail.com\"\n");
            fileWriter("    xmlns=\"http://www.topografix.com/GPX/1/1\"\n");
            fileWriter("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            fileWriter("    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1");
            fileWriter(" http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");
        } //writeHeader()

        private void writeGPXfile(int type) {
            mLog(2, "MakeGPXFragment.makeGPX.writeGPXfile()");
            reader = null;
            try {
                reader = new BufferedReader(new FileReader(wrkFile));
                while ((mLine = reader.readLine()) != null) {
                    bytesRead += mLine.length();
                    writeGPXline(type, mLine);
                    pct = (int) ((bytesRead * 100) / bytesToRead);
                    if (pct > 100) pct = 100;
                    dialog.setProgress(pct);
                }
            } catch (FileNotFoundException e) {
                mLog(0, "**** makeGPX.writeGPXfile() FileNotFoundException-reader pass");
                Main.buildCrashReport(e);
            } catch (IOException e) {
                mLog(0, "**** makeGPX.writeGPXfile() IOException-reader pass");
                Main.buildCrashReport(e);
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    mLog(0, "**** makeGPX.writeGPXfile() IOException-reader.close");
                    Main.buildCrashReport(e);
                }
            }
        } //writeGPXfile()

        private void writeGPXline(int type, String csv) {
            mLog(3, "MakeGPXFragment.makeGPX.writeGPXline()");
            if (csv.contains("UTC")) return;
//UTC,valid,lat,lon,height,speed,heading,DSTA,DAGE,PDOP,HDOP,VDOP,sat,inuse,SID,RCR,miliSec,distance,rChk,cChk,trk,mask
// 0 ,  1  , 2 , 3 ,  4   ,  5  ,   6   , 7  , 8  , 9  , 10 , 11 , 12, 13  , 14, 15,  16   ,   17   , 18 , 19 , 20, 21
            cells = csv.split(",");
            valid = Short.parseShort(cells[1]);
            rcr = Short.parseShort(cells[15]);
            if (type == GPXwpt && !getRCRs(rcr).contains("B")) return;
            if (valid == 1 || rcr > 16) return;
            utcTime = Long.parseLong(cells[0]);
            lat = Double.parseDouble(cells[2]);
            lon = Double.parseDouble(cells[3]);
            if (lat == 0 || lon == 0) return;
            //make sure record has a good checksum
            if (!cells[18].equals(cells[19])) return;
            if (!cbxone) {
                String newTrk = cells[20];
                if (type == GPXtrk && !newTrk.equals(savedTrk)) {
                    savedTrk = newTrk;
                    if (linesOut < 2) {
                        writeTrkBgn(cells[20], Long.parseLong(cells[0]));
                    } else {
                        if ((utcTime - lastUTC) >= trksecs) {
                            fileWriter(in1 + "</trkseg>\n" + "</trk>\n");
                            writeTrkBgn(cells[20], Long.parseLong(cells[0]));
                        } else {
                            fileWriter(in1 + "</trkseg>\n" + in1 + "<trkseg>\n");
                        }
                    }
                }
            }
            formatMask = Integer.parseInt(cells[21]);
            // write XML - lat, lon, height, utcTime
            lastUTC = utcTime;
            writeLatLong(type, cells[2], cells[3], cells[4], utcTime);
            if ((formatMask & FORMAT_HEADING) == FORMAT_HEADING) fileWriter(
                    String.format("%s<course>%s</course>\n", in3, cells[6]));
            if ((formatMask & FORMAT_SPEED) == FORMAT_SPEED) fileWriter(
                    String.format("%s<speed>%s</speed>\n", in3, cells[5]));
            fileWriter(String.format("%s<name>%s</name>\n", in3, getRecNum(type)));
            if ((formatMask & FORMAT_RCR) == FORMAT_RCR) fileWriter(
                    String.format("%s<type>%s</type>\n", in3, getRCRs(rcr)));
            if ((formatMask & FORMAT_VALID) == FORMAT_VALID) fileWriter(
                    String.format("%s<fix>%s</fix>\n", in3, getValid(valid)));
            if ((formatMask & FORMAT_NSAT) == FORMAT_NSAT) fileWriter(
                    String.format("%s<sat>%s</sat>\n", in3, cells[13]));
            if ((formatMask & FORMAT_HDOP) == FORMAT_HDOP) fileWriter(
                    String.format("%s<hdop>%.6f</hdop>\n", in3, Float.parseFloat(cells[10]) / 100));
            if ((formatMask & FORMAT_PDOP) == FORMAT_PDOP) fileWriter(
                    String.format("%s<pdop>%.6f</pdop>\n", in3, Float.parseFloat(cells[9]) / 100));
            if ((formatMask & FORMAT_VDOP) == FORMAT_VDOP) fileWriter(
                    String.format("%s<vdop>%.6f</vdop>\n", in3, Float.parseFloat(cells[11]) / 100));
            fileWriter(getGPXend(type));
            linesOut++;
        } //writeGPXline()

        private void writeLatLong(int type, String lat, String lon, String height, long date) {
            mLog(3, "MakeGPXFragment.makeGPX.writeLatLong()");
            mDate = new java.util.Date(date * 1000);
            formattedDate = formatter.format(add1024toDate(mDate));    //dyj
            mLog(3, String.format("formattedDate %s", formattedDate));
            if (type == GPXwpt) {
                fileWriter(String.format("%s<wpt lat=\"%s\" lon=\"%s\">\n", in2, lat, lon));
            } else {
                fileWriter(String.format("%s<trkpt lat=\"%s\" lon=\"%s\">\n", in2, lat, lon));
            }
            fileWriter(String.format("%s<ele>%s</ele>\n", in3, height));
            fileWriter(String.format("%s<time>%s</time>\n", in3, formattedDate));
        } //writeLatLong()

        private void writeTrkBgn(String newTrk, long time) {
            mLog(2, "MakeGPXFragment.makeGPX.writeTrkBgn()");
            mLog(2, String.format("linesOut =%d", linesOut));
            Date mDate = new java.util.Date(time * 1000);
            formattedDate = tnFormatter.format(add1024toDate(mDate));    //dyj
            String s = "<trk>\n" + "   <name>%s</name>\n" + "   <number>%s</number>\n" + in1 + "<trkseg>\n";
            fileWriter(String.format(s, formattedDate, newTrk));
        } //writeTrkBgn()

    } //class makeGPX

    private class makeKML extends AsyncTask<Void, Integer, Void> {

        private Context lContext;
        private int trksecs;

        public makeKML(Context context) {
            mLog(2, "MakeGPXFragment.makeKML.makeKML()");
            lContext = context;
        } //makeGPX()

        @Override
        protected void onPreExecute() {
            mLog(2, "MakeGPXFragment.makeKML.onPreExecute()");
            trksecs = appPrefs.getInt("trkSecs", 0);
            dialog = new ProgressDialog(lContext);
            dialog.setCancelable(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(100);
            dialog.setTitle(getString(R.string.writeKML));
            dialog.show();
            //set bytesToRead for 1 xml file read and 3 csv file passes
            bytesToRead = 16569 + (csvLength * 3);
        } //onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            mLog(2, "MakeGPXFragment.makeKML.doInBackground()");
            // Open an output for writing the kml file
            String kmlPath = fileNamePrefix + ".kml"; // - date from first record in file
            kmlFile = new File(appPrefs.getString("kmlPath", ""), kmlPath);
            mLog(1, "Creating GPX file: " + kmlFile.toString());
            FileWriter mFileWriter;
            try {
                mFileWriter = new FileWriter(kmlFile);
                mWriter = new BufferedWriter(mFileWriter, SIZEOF_SECTOR / 8);
            } catch (IOException e) {
                mLog(0, "**** makeKML.doInBackground() IOException-mWriter create");
                Main.buildCrashReport(e);
            }
            bytesRead = 0;
            //copy XML style boilerplate
            StyleMapPass();

            //create way points XML
            count = 0;
            fileWriter("<Folder>\n");
            fileWriter(in1 + "<name>My Waypoints</name>\n");
            fileWriter(in1 + "<open>0</open>\n");

            //way point Placemarks pass
            trkPointPass(wayRec);

            fileWriter("</Folder>\n");
            fileWriter("<Folder>\n");
            fileWriter(in1 + "<name>My Tracks</name>\n");
            fileWriter(in1 + "<open>0</open>\n");
            tmpString = fileNamePrefix.substring(0, 10);
            fileWriter(in2 + String.format("<name>TRACK-%s</name>\n", tmpString.replace("-", "")));
            fileWriter(in1 + String.format("<Placemark><name>%s</name>\n", wpFormatter.format(fileDate)));
            fileWriter(in2 + "<Style>\n");
            fileWriter(in3 + "<LineStyle>\n");
            fileWriter(in4 + "<color>ffFF0000</color>\n");
            fileWriter(in4 + "<width>3.0</width>\n");
            fileWriter(in3 + "</LineStyle>\n");
            fileWriter(in2 + "</Style>\n");
            fileWriter(in1 + "<LineString>\n");
            fileWriter(in2 + "<extrude>1</extrude>\n");
            fileWriter(in2 + "<tessellate>1</tessellate>\n");
            fileWriter(in2 + "<altitudeMode>clampToGround</altitudeMode><coordinates>\n");

            //longitude, latitude, height records pass
            routePoints();

            fileWriter(in3 + "</coordinates>\n");
            fileWriter(in2 + "</LineString>\n");
            fileWriter(in1 + "</Placemark>\n");
            fileWriter("</Folder>\n");
            fileWriter("<Folder>\n");
            fileWriter(in1 + "<name>My Trackpoints</name>\n");
            fileWriter(in1 + "<Folder>\n");
            tmpString = fileNamePrefix.substring(0, 10);
            fileWriter(in2 + String.format("<name>Trackpoints-%s</name>\n", tmpString.replace("-", "")));
            fileWriter(in2 + "<open>0</open>\n");

            //track point Placemarks pass
            trkPointPass(trkRec);

            fileWriter(in1 + "</Folder>\n</Folder>\n\n</Document>\n</kml>");
            return null;
        } //doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(2, "MakeGPXFragment.makeKML.onPostExecute()");
            if (dialog.isShowing()) dialog.dismiss();
            mTv.append("created: " + kmlFile.toString() + "\n");
            scrollDown();
            makeKML.setEnabled(false);
        } //onPostExecute()

        private void StyleMapPass() {
            mLog(2, "MakeGPXFragment.makeKML.StyleMapPass()");
            reader = null;
            linesOut = 0;
            try {
                reader = new BufferedReader(new InputStreamReader
                        (lContext.getAssets().open("kmlHeader.xml"), "UTF-8"));
                // do reading, usually loop until end of file reading
                while ((mLine = reader.readLine()) != null) {
                    linesOut++;
                    bytesRead += mLine.length();
                    if (linesOut == 4)
                        fileWriter(String.format("<name>%s</name>\n", fileNamePrefix));
                    fileWriter(mLine + "\n");
                    pct = (int) ((bytesRead * 100) / bytesToRead);
                    if (pct > 100) pct = 100;
                    dialog.setProgress(pct);
                }
            } catch (IOException e) {
                mLog(0, "**** makeKML.StyleMapPass() IOException-reader loop");
                Main.buildCrashReport(e);
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    mLog(0, "**** makeKML.StyleMapPass() IOException-reader.close");
                    Main.buildCrashReport(e);
                }
            }
        } //StyleMapPass()

        private void trkPointPass(int type) {
            String sType = (type == wayRec) ? "wayRec" : "trkRec";
            mLog(2, String.format("MakeGPXFragment.makeKML.trkPointPass(%s)", sType));
            reader = null;
            linesOut = 0;
            count = 0;
            try {
                reader = new BufferedReader(new FileReader(wrkFile));
                while ((mLine = reader.readLine()) != null) {
                    linesOut++;
                    bytesRead += mLine.length();
                    if (linesOut == 1) continue;
                    trkRecLine(type, mLine);
                    pct = (int) ((bytesRead * 100) / bytesToRead);
                    if (pct > 100) pct = 100;
                    dialog.setProgress(pct);
                }
                if (count > 0) fileWriter(in1 + "</Folder>\n");
            } catch (FileNotFoundException e) {
                mLog(0, "**** makeKML.WayPointPass() FileNotFoundException-reader pass");
                Main.buildCrashReport(e);
            } catch (IOException e) {
                mLog(0, "**** makeKML.WayPointPass() IOException-reader pass");
                Main.buildCrashReport(e);
            } finally {
                try {
//                    if (reader != null) reader.close();
                    reader.close();
                } catch (IOException e) {
                    mLog(0, "**** makeGPX.WayPointPass() IOException-reader.close");
                    Main.buildCrashReport(e);
                }
            }
        } //WayPointPass()

        private void trkRecLine(int type, String csv) {
            if (csv.contains("UTC")) return;
            String sType = (type == wayRec) ? "wayRec" : "trkRec";
            mLog(3, String.format("MakeGPXFragment.makeKML.trkRecLine(%s)", sType));
//UTC,valid,lat,lon,height,speed,heading,DSTA,DAGE,PDOP,HDOP,VDOP,sat,inuse,SID,RCR,miliSec,distance,rChk,cChk,trk,mask
// 0 ,  1  , 2 , 3 ,  4   ,  5  ,   6   , 7  , 8  , 9  , 10 , 11 , 12, 13  , 14, 15,  16   ,   17   , 18 , 19 , 20, 21
            cells = csv.split(",");
            valid = Short.parseShort(cells[1]);
            rcr = Short.parseShort(cells[15]);
            if (type == wayRec && !getRCRs(rcr).contains("B")) return;
            if (valid == 1 || rcr > 16) return;
            lat = Double.parseDouble(cells[2]);
            lon = Double.parseDouble(cells[3]);
            if (lat == 0 || lon == 0) return;
            utcTime = Long.parseLong(cells[0]);
            mDate = new java.util.Date(utcTime * 1000);
            if (count == 0) {
                fileWriter(in1 + String.format("<Folder><name>%s</name>\n", dateFormatter.format(add1024toDate(mDate))));
            }
            count++;
            fileWriter(in2 + "<Placemark>\n");
            fileWriter(in3 + String.format("<name>TIME: %s</name>\n", timeFormatter.format(add1024toDate(mDate))));
            if (type == trkRec) fileWriter(in3 + "<visibility>0</visibility>\n");

            fileWriter(in3 + "<description><![CDATA[<table width=400>");
            fileWriter(String.format("<tr><td>TIME:</td><td>%s</td></tr>", wpFormatter.format(add1024toDate(mDate))));
            tmpString = (getRCRs(rcr).length() > 1) ? "MixStamp" : "ButtonStamp";
            fileWriter(String.format("<tr><td>RCR:</td><td>%s <b>(%s)</b></td></tr>", getRCRs(rcr), tmpString));
            fileWriter(String.format("<tr><td>VALID:</td><td>%s</td></tr>", getValid(valid)));
            latS = (Double.parseDouble(cells[2]) < 0) ? cells[2].substring(1) : cells[2];
            fileWriter(String.format("<tr><td>LATITUDE:</td><td>%s %s</td></tr>", latS, (Double.parseDouble(cells[2]) < 0) ? "S" : "N"));
            lonS = (Double.parseDouble(cells[3]) < 0) ? cells[3].substring(1) : cells[3];
            fileWriter(String.format("<tr><td>LONGITUDE:</td><td>%s %s</td></tr>", lonS, (Double.parseDouble(cells[3]) < 0) ? "W" : "E"));
            fileWriter(String.format("<tr><td>HEIGHT:</td><td>%s m</td></tr></table>]]>", cells[4]));
            fileWriter(String.format("</description><TimeStamp><when>%s</when></TimeStamp>\n", formatter.format(add1024toDate(mDate))));

            fileWriter(in3 + String.format("<styleUrl>#Style%s</styleUrl>\n", tmpString.substring(0, 1)));
            fileWriter(in3 + "<Point>\n");
            fileWriter(in3 + String.format("<coordinates>%s,%s,%s</coordinates></Point>\n", cells[3], cells[2], cells[4]));
            fileWriter(in2 + "</Placemark>\n");
        } //trkRecLine()

        private void routePoints() {
            mLog(2, "MakeGPXFragment.makeKML.routePoints()");
            reader = null;
            linesOut = 0;
//UTC,valid,lat,lon,height,speed,heading,DSTA,DAGE,PDOP,HDOP,VDOP,sat,inuse,SID,RCR,miliSec,distance,rChk,cChk,trk,mask
// 0 ,  1  , 2 , 3 ,  4   ,  5  ,   6   , 7  , 8  , 9  , 10 , 11 , 12, 13  , 14, 15,  16   ,   17   , 18 , 19 , 20, 21
            try {
                reader = new BufferedReader(new FileReader(wrkFile));
                while ((mLine = reader.readLine()) != null) {
                    linesOut++;
                    bytesRead += mLine.length();
                    if (linesOut == 1) continue;
                    cells = mLine.split(",");
                    valid = Short.parseShort(cells[1]);
                    rcr = Short.parseShort(cells[15]);
                    if (valid == 1 || rcr > 16) continue;
                    lat = Double.parseDouble(cells[2]);
                    lon = Double.parseDouble(cells[3]);
                    if (lat == 0 || lon == 0) continue;
                    fileWriter(in4 + String.format("%s,%s,%s\n", cells[3], cells[2], cells[4]));
                    pct = (int) ((bytesRead * 100) / bytesToRead);
                    if (pct > 100) pct = 100;
                    dialog.setProgress(pct);
                }
            } catch (FileNotFoundException e) {
                mLog(0, "**** makeKML.WayPointPass() FileNotFoundException-reader pass");
                Main.buildCrashReport(e);
            } catch (IOException e) {
                mLog(0, "**** makeKML.WayPointPass() IOException-reader pass");
                Main.buildCrashReport(e);
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    mLog(0, "**** makeGPX.WayPointPass() IOException-reader.close");
                    Main.buildCrashReport(e);
                }
            }
        } //routePoints()

    } // class makeKML

    private class makeCSV extends AsyncTask<Void, Integer, Void> {

        private Context lContext;
        private int index = 0;

        public makeCSV(Context context) {
            mLog(2, "MakeGPXFragment.makeCSV.makeCSV()");
            lContext = context;
        } //makeCSV()

        @Override
        protected void onPreExecute() {
            mLog(1, "MakeGPXFragment.makeCSV.onPreExecute()");
            dialog = new ProgressDialog(lContext);
            dialog.setCancelable(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(100);
            dialog.setTitle(getString(R.string.writCSV));
            dialog.show();
            //set bytesToRead for 1 csv file pass
            bytesToRead = csvLength;
        } //onPreExecute()

        @Override
        protected Void doInBackground(Void... params) {
            mLog(1, "MakeGPXFragment.makeCSV.doInBackground()");
            // Open csv output file
            String csvName = fileNamePrefix + ".csv"; // - date from first record in file
            csvFile = new File(appPrefs.getString("csvPath", ""), csvName);
            mLog(1, "Creating CSV file: " + csvFile.toString());
            FileWriter mFileWriter;
            try {
                mFileWriter = new FileWriter(csvFile);
                mWriter = new BufferedWriter(mFileWriter, SIZEOF_SECTOR / 8);
            } catch (IOException e) {
                mLog(0, "**** makeCSV.doInBackground() IOException-mWriter create");
                Main.buildCrashReport(e);
            }
            bytesRead = 0;
            linesOut = 0;
            //write header row
            fileWriter(csvHeader);
            try {
                reader = new BufferedReader(new FileReader(wrkFile));
                while ((mLine = reader.readLine()) != null) {
                    linesOut++;
                    bytesRead += mLine.length();
                    if (linesOut == 1) continue;
                    csvRecLine(mLine);
                    pct = (int) ((bytesRead * 100) / bytesToRead);
                    if (pct > 100) pct = 100;
                    dialog.setProgress(pct);
                }
            } catch (FileNotFoundException e) {
                mLog(0, "**** makeKML.doInBackground() FileNotFoundException-reader loop");
                Main.buildCrashReport(e);
            } catch (IOException e) {
                mLog(0, "**** makeKML.doInBackground() IOException-reader loop");
                Main.buildCrashReport(e);
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    mLog(0, "**** makeKML.doInBackground() IOException-reader.close");
                    Main.buildCrashReport(e);
                }
            }
            return null;
        } //doInBackground()

        @Override
        protected void onPostExecute(Void param) {
            mLog(1, "MakeGPXFragment.makeCSV.onPostExecute()");
            if (dialog.isShowing()) dialog.dismiss();
            mTv.append("created: " + csvFile.toString() + "\n");
            scrollDown();
            makeCSV.setEnabled(false);
        } //onPostExecute()

        private void csvRecLine(String csv) {
            mLog(2, "MakeGPXFragment.makeCSV.csvRecLine()");
//UTC,valid,lat,lon,height,speed,heading,DSTA,DAGE,PDOP,HDOP,VDOP,sat,inuse,SID,RCR,miliSec,distance,rChk,cChk,trk,mask
// 0 ,  1  , 2 , 3 ,  4   ,  5  ,   6   , 7  , 8  , 9  , 10 , 11 , 12, 13  , 14, 15,  16   ,   17   , 18 , 19 , 20, 21
            cells = csv.split(",");
            valid = Short.parseShort(cells[1]);
            rcr = Short.parseShort(cells[15]);

            index++;
            formatMask = Integer.parseInt(cells[21]);
            fileWriter(cells[20]);

            fileWriter(",");
            fileWriter(String.valueOf(index));

            fileWriter(",");
            if ((formatMask & FORMAT_RCR) == FORMAT_RCR) {
                fileWriter(getRCRs(rcr));
            }

            fileWriter(",");
            if ((formatMask & FORMAT_UTC) == FORMAT_UTC) {
                utcTime = Long.parseLong(cells[0]);
                mDate = new java.util.Date(utcTime * 1000);
                fileWriter(csvDate.format(add1024toDate(mDate)));
                fileWriter(",");
                formattedDate = String.format("%s", csvTime.format(add1024toDate(mDate)));
                if ((formatMask & FORMAT_MILLISECOND) == FORMAT_MILLISECOND) {
                    tmpString = String.valueOf((int) Math.round(Integer.parseInt(cells[16]) / 100.0));
                    formattedDate = String.format("%s.%s", formattedDate, tmpString);
                }
                fileWriter(formattedDate);
            } else fileWriter(",");

            fileWriter(",");
            if ((formatMask & FORMAT_VALID) == FORMAT_VALID) fileWriter(getValid(valid));

            fileWriter(",");
            if ((formatMask & FORMAT_LATITUDE) == FORMAT_LATITUDE) {
                fileWriter(cells[2]);
                fileWriter(",");
                fileWriter(Double.parseDouble(cells[2]) < 0 ? "S" : "N");
            } else fileWriter(",");

            fileWriter(",");
            if ((formatMask & FORMAT_LONGITUDE) == FORMAT_LONGITUDE) {
                fileWriter(cells[3]);
                fileWriter(",");
                fileWriter(Double.parseDouble(cells[3]) < 0 ? "W" : "E");
            } else fileWriter(",");

            fileWriter(",");
            if ((formatMask & FORMAT_HEIGHT) == FORMAT_HEIGHT) fileWriter(cells[4]);

            fileWriter(",");
            if ((formatMask & FORMAT_SPEED) == FORMAT_SPEED)
                fileWriter(String.format("%.4f", Float.parseFloat(cells[5]) * 3.6f));

            fileWriter(",");
            if ((formatMask & FORMAT_HEADING) == FORMAT_HEADING) fileWriter(cells[6]);

            fileWriter(",");
            if ((formatMask & FORMAT_DSTA) == FORMAT_DSTA) fileWriter(cells[7]);

            fileWriter(",");
            if ((formatMask & FORMAT_DAGE) == FORMAT_DAGE) fileWriter(cells[8]);

            fileWriter(",");
            if ((formatMask & FORMAT_PDOP) == FORMAT_PDOP)
                fileWriter(String.format("%.2f", Float.parseFloat(cells[9]) / 100f));

            fileWriter(",");
            if ((formatMask & FORMAT_HDOP) == FORMAT_HDOP)
                fileWriter(String.format("%.2f", Float.parseFloat(cells[10]) / 100f));

            fileWriter(",");
            if ((formatMask & FORMAT_VDOP) == FORMAT_VDOP)
                fileWriter(String.format("%.2f", Float.parseFloat(cells[11]) / 100f));

            fileWriter(",");
            if ((formatMask & FORMAT_NSAT) == FORMAT_NSAT)
                fileWriter(String.format("%s(%s)", cells[13], cells[12]));

            fileWriter(",");
            if ((formatMask & FORMAT_DISTANCE) == FORMAT_DISTANCE) fileWriter(cells[17]);

            fileWriter(",");
            fileWriter(cells[14]);
            fileWriter("\n");
//UTC,valid,lat,lon,height,speed,heading,DSTA,DAGE,PDOP,HDOP,VDOP,sat,inuse,SID,RCR,miliSec,distance,rChk,cChk,trk,mask
// 0 ,  1  , 2 , 3 ,  4   ,  5  ,   6   , 7  , 8  , 9  , 10 , 11 , 12, 13  , 14, 15,  16   ,   17   , 18 , 19 , 20, 21
//"INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT(m),SPEED(km/h),HEADING,
//   DSTA,DAGE,PDOP,HDOP,VDOP,NSAT (USED/VIEW),DISTANCE(m),SAT INFO (SID-ELE-AZI-SNR)

        } //csvRecLine()
    } // class makeCSV

} // class MakeGPXFragment

