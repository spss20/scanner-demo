package maulik.barcodescanner.modals;

public class BarcodeData {
    private String result;
    private String address;
    private long time;
    private String lat;
    private String lon;


    public BarcodeData() {
    }

    public BarcodeData(String result, String address, long time, String lat, String lon) {
        this.result = result;
        this.address = address;
        this.time = time;
        this.lat = lat;
        this.lon = lon;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }


    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }
}
