package com.app.stubble.data;

/**
 * Created by halfapple on 17/7/20.
 **/
public class InterSectionData implements Comparable<InterSectionData> {

    //二维数组中的元数据
    private int i; //行
    private int j; //列
    public int v; //差值

    public int iBegin;
    public int iEnd;
    public int jBegin;
    public int jEnd;

    public InterSectionData(int i, int j, int argValue) {
        this.i = i;
        this.j = j;
        this.v = argValue;

        iBegin = i - this.v;
        iEnd = i;
        jBegin = j - this.v;
        jEnd = j;
    }

    @Override
    public int compareTo(InterSectionData o) {

        if (this.v < o.v) {
            return -1;
        } else if (this.v == o.v) {
            return 0;
        } else {
            return 1;
        }
    }

}