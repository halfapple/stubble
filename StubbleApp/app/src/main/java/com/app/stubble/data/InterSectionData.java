package com.app.stubble.data;

/**
 * Created by halfapple on 17/7/20.
 **/
public class InterSectionData implements Comparable<InterSectionData> {

    //二维数组中的元数据
    private int i; //行
    private int j; //列
    private int v; //差值，取绝对值

    private int ArrayLength; //一维数组长度

    private int iBegin;
    private int iEnd;
    private int jBegin;
    private int jEnd;

    private int area; //面积
    private double diffRate; //差率 = 差值 / 面积

    public InterSectionData(int i, int j, int argValue, int argLength) {
        this.i = i;
        this.j = j;
        this.v = argValue;
        this.ArrayLength = argLength;

        if (i < j) {
            iBegin = 0;
            iEnd = i;
            jBegin = ArrayLength - i - 1;
            jEnd = ArrayLength - 1;
            area = (i + 1) * (i + 1);

        } else {
            iBegin = ArrayLength - j - 1;
            iEnd = ArrayLength - 1;
            jBegin = 0;
            jEnd = j;
            area = (j + 1) * (j + 1);
        }
        diffRate = v * 1.0 / area; ////比率＝差值／面积
    }

    @Override
    public int compareTo(InterSectionData o) {
        if (this.diffRate < o.diffRate) {
            return -1;
        } else if (this.diffRate == o.diffRate) {
            return 0;
        } else {
            return 1;
        }
    }

}