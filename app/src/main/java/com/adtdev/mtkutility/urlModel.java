package com.adtdev.mtkutility;

public class urlModel {
    String ftpDesc;
    String ftpURL;
    String ftpUSER;
    String ftpPSWD;


    public urlModel(String desc, String url, String user, String pswd ) {
        this.ftpDesc=desc;
        this.ftpURL=url;
        this.ftpUSER=user;
        this.ftpPSWD=pswd;
    }

    public String getDesc() {
        return ftpDesc;
    }
    public String getURL() {
        return ftpURL;
    }
    public String getUSER() {
        return ftpUSER;
    }
    public String getPSWD() {
        return ftpPSWD;
    }
}
