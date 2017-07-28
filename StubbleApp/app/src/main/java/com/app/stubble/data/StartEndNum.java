package com.app.stubble.data;

/**
 * Created by halfapple on 17/7/23.
 **/
public class StartEndNum {

    private int start;
    private int end;
    private int unit;

    public StartEndNum(int start, int end, int unit) {
        this.start = start;
        this.end = end;
        this.unit = unit;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getUnit() {
        return unit;
    }

    public void setUnit(int unit) {
        this.unit = unit;
    }
}