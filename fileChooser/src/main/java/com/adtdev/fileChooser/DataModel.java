package com.adtdev.fileChooser;

public class DataModel implements Comparable<DataModel>{

    //    String name;
//    String type;
//    String version_number;
//    String feature;
    private String name;
    private String data;
    private String date;
    private String path;
    private String image;


    public DataModel(String name, String data, String date, String path, String image) {
        this.name = name;
        this.data = data;
        this.date = date;
        this.path = path;
        this.image = image;
    }

    public String getimage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public String getdata() {
        return data;
    }

    public String getdate() {
        return date;
    }

    public String getpath() {
        return path;
    }

    public int compareTo(DataModel o) {
        if(this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }

}
