package com.adtdev.fileChooser;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.io.FileUtils;

import static org.apache.commons.io.FileUtils.forceDelete;

public class FileChooser extends ListActivity {

    private static final int REQUEST_READ = 0;
    private static final int LOCALsel = 0;
    private static final int FTPsel = 1;
    private static final int doLOCfile = 0;
    private static final int doFTPfile = 1;
    private static final int doFTPdnld = 2;
    private final String fileActions[] = {"View","Rename","Delete"};
//    private final String fileActions[] = {"View","Rename","Delete","Copy to","Move to"};
    AlertDialog.Builder fileAction;
    private static CustomAdapter adapter;
    private ProgressDialog dialog;
    private File currentDir;
    private int method;
    private String rootPath;
    private String strtPath;
    private String ftpFile;
    private String ftpPath = "/";
    private String lclFile;
    private String ftpURL;
    private String ftpName;
    private String ftpPswd;
    private int ftpPort;
    private boolean ftpAdd;
    public boolean selfolders = false;
    public boolean selfiles = true;
    public boolean showhidden = false;
    private boolean doWrite = false;
    private String sTitle;
    private String[] sArr;
    private boolean ok;
    private Context context;
    private  Intent intent;
    private DataModel objSelected;

    private List<DataModel> dir = new ArrayList<DataModel>();
    private List<DataModel> fls = new ArrayList<DataModel>();
    private String parent;
    private int source;
    String bacK = "";
    String fn;
    File fN, fff;

    public FTPClient mFTPClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        //check for write to external storage permission and request if not present

        boolean hasWrite = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasWrite) {
            // ask the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }

        // Get the transferred data from source activity.
        intent = getIntent();
        method = intent.getIntExtra("method", 0);
        switch (method) {
            case doLOCfile:
                runLocal();
                break;
            case doFTPfile:
                runFTPsel();
                break;
            case doFTPdnld:
                runFTPdl();
                break;
        }
    }//onCreate()

    private void runLocal() {
        rootPath = intent.getStringExtra("root");
        strtPath = intent.getStringExtra("start");
        selfolders = intent.getBooleanExtra("selfolders", false);
        selfiles = intent.getBooleanExtra("selfiles", true);
        showhidden = intent.getBooleanExtra("showhidden", false);
        currentDir = new File(strtPath);
        doWrite = false;
        fill(currentDir);
    }//runLocal()

    private void runFTPsel() {
        ftpURL = intent.getStringExtra("ftpURL").trim();
        ftpName = intent.getStringExtra("ftpName").trim();
        ftpPswd = intent.getStringExtra("ftpPswd").trim();
        selfolders = false;
        ftpPort = Integer.parseInt(intent.getStringExtra("ftpPort").trim());
        this.setTitle(ftpURL);
        source = FTPsel;
        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.ftpconnect));
        dialog.show();
        new Thread(new Runnable() {
            public void run() {
                ok = ftpConnect();
                if (ok) {
                    handler.sendEmptyMessage(0);
                } else {
                    handler.sendEmptyMessage(-1);
                }
            }
        }).start();
    }//runFTPsel()

    private void runFTPdl() {
        ftpURL = intent.getStringExtra("ftpURL").trim();
        ftpName = intent.getStringExtra("ftpName").trim();
        ftpPswd = intent.getStringExtra("ftpPswd").trim();
        ftpPort = Integer.parseInt(intent.getStringExtra("ftpPort").trim());
        ftpFile = intent.getStringExtra("srceFN").trim();
        lclFile = intent.getStringExtra("destFN").trim();
        ftpAdd = intent.getBooleanExtra("append", false);
        this.setTitle("FTP Download in Progress");
        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.working));
        dialog.show();
        new Thread(new Runnable() {
            public void run() {
                if (ftpConnect()) {
                    if (ftpDownload()) {
                        handler.sendEmptyMessage(2);
                    }else {
                        handler.sendEmptyMessage(-2);
                    }
                } else {
                    handler.sendEmptyMessage(-1);
                }
            }
        }).start();
    }//runFTPdl()

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {

//            if (pd != null && pd.isShowing()) {
//                pd.dismiss();
//            }
            if (msg.what == -1) {
                if (dialog.isShowing()) dialog.dismiss();
                Toast.makeText(context, "connect failed - " + ftpURL, Toast.LENGTH_LONG).show();
                finish();
            } else if (msg.what == -2) {
                if (dialog.isShowing()) dialog.dismiss();
                Toast.makeText(context, "download failed - " + ftpURL, Toast.LENGTH_LONG).show();
                finish();
            } else if (msg.what == 0) {
                if (dialog.isShowing()) dialog.dismiss();
                Toast.makeText(context, "connected to " + ftpURL, Toast.LENGTH_SHORT).show();
                ftpPath = "/";
                fillFTP(ftpPath);
            } else if (msg.what == 1) {
                if (dialog.isShowing()) dialog.dismiss();
                adapter = new CustomAdapter(FileChooser.this, R.layout.file_view, dir);
                FileChooser.this.setListAdapter(adapter);
            } else if (msg.what == 2) {
                if (dialog.isShowing()) dialog.dismiss();
                Toast.makeText(context, ftpFile + " download completed", Toast.LENGTH_LONG).show();
                finish();
            }

        }

    };

    private void fileAction(int sel) {
        //fileActions={"Rename","Copy to","Move to","Delete"};
        switch (sel) {
            case 0:
                viewFile();
                break;
            case 1:
                //Rename
                renameFile();
                break;
            case 2:
                //Delete
                deleteFile();
                break;
            case 3:
                //Copu to
                Toast.makeText(this, "Copy to selected", Toast.LENGTH_LONG).show();
                break;
            case 4:
                //Move to
                Toast.makeText(this, "Move to selected", Toast.LENGTH_LONG).show();
                break;
        }
    }//fileAction()

    private void fill(File f) {
        if (f.getName().equalsIgnoreCase("emulated")) {
            f = new File("/storage/emulated/0");
        }
        sTitle = f.getPath();
        sArr = sTitle.split("/");
        if (sArr.length > 4)
            sTitle = "/" + sArr[1] + "/" + sArr[2] + "/.../" + sArr[sArr.length - 1];
        this.setTitle(sTitle);
        File[] dirs = f.listFiles();
        List<DataModel> dir = new ArrayList<DataModel>();
        List<DataModel> fls = new ArrayList<DataModel>();
        String bacK = "";
        String fn;
        File fN, fff;
        try {
            for (File ff : dirs) {
                fn = ff.getName();
                if (fn.startsWith(".") && !showhidden) continue;
                if (fn.equalsIgnoreCase("emulated")) {
                    fff = new File("/storage/emulated/0");
                } else {
                    fff = ff;
                }
                if (fff.canRead() || (doWrite && fff.canWrite())) {
                    String s = fff.getAbsolutePath();
                    Date lastModDate = new Date(fff.lastModified());
                    DateFormat formater = DateFormat.getDateTimeInstance();
                    String date_modify = formater.format(lastModDate);
                    if (ff.isDirectory()) {
                        File[] fbuf = fff.listFiles();
                        int buf = 0;
                        if (fbuf != null) {
                            buf = fbuf.length;
                        } else buf = 0;
                        String num_item = String.valueOf(buf);
                        if (buf == 0) num_item = num_item + " item";
                        else num_item = num_item + " items";

                        //String formated = lastModDate.toString();
                        if (selfolders) {
                            dir.add(new DataModel(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), "folder"));
                        }else {
                            dir.add(new DataModel(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), "nofolder"));
                        }
                    } else {
                        if (selfiles) {
                            fls.add(new DataModel(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), "file"));
                        }else{
                            fls.add(new DataModel(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), "nofile"));
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        String ff = f.getAbsolutePath();
        if (!f.getAbsolutePath().equalsIgnoreCase(rootPath)) {
            parent = f.getParent();
            fN = new File(parent);
            parent = fN.getAbsolutePath();
            String fldr[] = parent.split("/");
            if (fldr.length > 2) parent = "../" + fldr[fldr.length-2]+"/"+fldr[fldr.length-1];
            if (parent.equalsIgnoreCase("/storage/emulated")) parent = "/storage";
            dir.add(0, new DataModel("<<- " + parent, ff, "", f.getParent(), "directory_up"));
        }
        adapter = new CustomAdapter(FileChooser.this, R.layout.file_view, dir);
        this.setListAdapter(adapter);
    }//fill()

    private void fillFTP(String path) {
        final String lpath = path;
        dir = new ArrayList<DataModel>();
        fls = new ArrayList<DataModel>();
        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.reading));
        dialog.show();
        new Thread(new Runnable() {
            public void run() {
                try {
                    FTPFile[] files = mFTPClient.listFiles(lpath);
                    for (FTPFile file : files) {
                        fillFTPdisplayItem(file);
                    }
                    Collections.sort(dir);
                    Collections.sort(fls);
                    dir.addAll(fls);
                    if (lpath.equalsIgnoreCase("/")){
                        parent = "";
                    }else {
                        int lix = lpath.lastIndexOf('/');
                        parent = (lix == 0) ?  lpath.substring(0, 1): lpath.substring(0, lix);
                    }
                    if (!parent.equalsIgnoreCase("")) {
                        dir.add(0, new DataModel("<<- " + parent, lpath, "", parent, "directory_up"));
                    }
                    handler.sendEmptyMessage(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }//fillFTP()

    private void fillFTPdisplayItem(FTPFile ff) {
        Date lastModDate = new Date(ff.getTimestamp().getTimeInMillis());
        DateFormat formater = DateFormat.getDateTimeInstance();
        String date_modify = formater.format(lastModDate);
        String filepath = ftpPath + "/" + ff.getName();
        filepath = filepath.replace("//", "/");
        String s = ff.getRawListing();
        if (ff.isDirectory()) {
            int buf = 0;
            try {
                buf = mFTPClient.listFiles(ftpPath + "/" + ff.getName()).length;
            } catch (IOException e) {
                e.printStackTrace();
            }
            String num_item = String.valueOf(buf);
            if (buf == 0) num_item = num_item + " item";
            else num_item = num_item + " items";
            dir.add(new DataModel(ff.getName(), num_item, date_modify, filepath, "folder"));
        } else {
            fls.add(new DataModel(ff.getName(), ff.getSize() + " Byte", date_modify, filepath, "file"));
        }
    }

    private boolean ftpConnect() {
        try {
            mFTPClient = new FTPClient();
            mFTPClient.setConnectTimeout(5000);
            // connecting to the host
            mFTPClient.connect(ftpURL, ftpPort);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        // now check the reply code, if positive mean connection success
        if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {
            // login using username & password
            boolean isOK = false;
            try {
                isOK = mFTPClient.login(ftpName, ftpPswd);
                mFTPClient.setDataTimeout(20000);
                mFTPClient.setConnectTimeout(20000);
                mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
                mFTPClient.enterLocalPassiveMode();
                return isOK;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }//ftpConnect()

    private boolean ftpDownload() {
        boolean isOK = false;
        OutputStream outputStream = null;
        FileOutputStream out = null;
        File localPath = new File(lclFile);

        try {
            out = FileUtils.openOutputStream(localPath, ftpAdd);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outputStream = new BufferedOutputStream(out);
            isOK = mFTPClient.retrieveFile(ftpFile, outputStream);
            if (outputStream != null) outputStream.close();
        } catch (FileNotFoundException e) {
            isOK = false;
            e.printStackTrace();
        } catch (IOException e) {
            isOK = false;
            e.printStackTrace();
        }

        if (mFTPClient != null && mFTPClient.isConnected()) {
            try {
                mFTPClient.logout();
                mFTPClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isOK;
    }//ftpDownload()

    public void longpress(DataModel obj){
        if (source == FTPsel) return;
        objSelected = obj;
        int setChk = -1;
        String ns = "long press on " + obj.getName();
//        Toast.makeText(this,ns,Toast.LENGTH_LONG).show();

        fileAction=new AlertDialog.Builder(context);
//        fileAction.setTitle(R.string.fileAction)
        fileAction.setTitle(obj.getName())
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int sel) {}})
                .setItems(fileActions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int sel) {
                        dialog.dismiss();
                        fileAction(sel);
                    }
                })
                .show();
    }//longpress()

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id);
        DataModel object = adapter.getItem(position);

        if (object.getimage().contains("folder") || object.getimage().equalsIgnoreCase("directory_up")) {
//            String path = o.getPath();

            switch (method) {
                case doLOCfile:
                    if (object.getpath().equalsIgnoreCase("/storage/emulated") && object.getimage().equalsIgnoreCase("directory_up")) {
                        currentDir = new File("/storage");
                    } else {
                        currentDir = new File(object.getpath());
                    }
                    fill(currentDir);
                    break;
                case doFTPfile:
                    ftpPath = object.getpath();
                    fillFTP(ftpPath);
                    break;
                case doFTPdnld:
                    // no action
                    break;
            }
        } else {
//            sendBack(object);
        }
    }

    private void renameFile() {
        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setText(objSelected.getName());

        fileAction = new AlertDialog.Builder(this);
        fileAction.setMessage("enter new name")
            .setView(input)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    File df = new File(objSelected.getpath());
                    File pp = new File(df.getParent());
                    File nf = new File(pp + "/" + input.getText().toString());
                    ok = df.renameTo(nf);
                    if (ok) {
                        Toast.makeText(getBaseContext(), "renamed to " + nf.getName(), Toast.LENGTH_LONG).show();
                        fill(pp);
                    }
                }})
            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }})
            .show();
    }//renameFile()


    private void deleteFile() {
        final TextView input = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setText(objSelected.getName());

        fileAction = new AlertDialog.Builder(this);
        fileAction.setMessage("confirm delete")
            .setView(input)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    File df = new File(objSelected.getpath());
                    File pp = new File(df.getParent());
                    try {
                        forceDelete(df);
                        Toast.makeText(getBaseContext(), objSelected.getName() + " deleted", Toast.LENGTH_SHORT).show();
                        fill(pp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }})
            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }})
            .show();
    }//deleteFile()

    public void sendBack(DataModel object) {

        Intent intent = new Intent();
        intent.putExtra("GetPath", object.getpath());
        intent.putExtra("GetFileName", object.getName());
        setResult(RESULT_OK, intent);
        finish();
    }//sendBack()

    public void startsendBack(DataModel object) {
        String ss = object.getimage();
        if (object.getimage().contains("file")) {
            if (selfiles) sendBack(object);
        } else {
            if (selfolders) sendBack(object);
        }
    }//startsendBack()

    private void viewFile() {
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        String fn = objSelected.getName();
        String ext = fn.substring(fn.lastIndexOf(".") + 1);
        String mimeType = myMime.getMimeTypeFromExtension(ext);
        File file = new File(objSelected.getpath());
        //note: the path for the authority must match the AndroidManifest authorities
        // a string is being used as this.getApplicationContext().getPackageName()
        // does not provide the authority location
        Uri uri = FileProvider.getUriForFile(this,
//                "adtdev.com.fileChooser.provider", file);
                "com.adtdev.mtkutility.provider", file);

        Intent newIntent = new Intent(Intent.ACTION_VIEW);
//        newIntent.setDataAndType(uri,"application/octet-stream");
        newIntent.setDataAndType(uri, mimeType);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        this.startActivity(newIntent);
    }//viewFile()
}
