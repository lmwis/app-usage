package com.lmwis.appusage.rpc.pojo;

public class AppUsagesDTO {

    String packageName;

    long lastTimeStamp;

    long lastTimeUsed;

    long lastTimeVisible;

    long totalTimeInForeground;

    long totalTimeVisible;

    long lastTimeForegroundServiceUsed;

    long totalTimeForegroundServiceUsed;

    int describeContents;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getLastTimeStamp() {
        return lastTimeStamp;
    }

    public void setLastTimeStamp(long lastTimeStamp) {
        this.lastTimeStamp = lastTimeStamp;
    }

    public long getLastTimeUsed() {
        return lastTimeUsed;
    }

    public void setLastTimeUsed(long lastTimeUsed) {
        this.lastTimeUsed = lastTimeUsed;
    }

    public long getLastTimeVisible() {
        return lastTimeVisible;
    }

    public void setLastTimeVisible(long lastTimeVisible) {
        this.lastTimeVisible = lastTimeVisible;
    }

    public long getTotalTimeInForeground() {
        return totalTimeInForeground;
    }

    public void setTotalTimeInForeground(long totalTimeInForeground) {
        this.totalTimeInForeground = totalTimeInForeground;
    }

    public long getTotalTimeVisible() {
        return totalTimeVisible;
    }

    public void setTotalTimeVisible(long totalTimeVisible) {
        this.totalTimeVisible = totalTimeVisible;
    }

    public long getLastTimeForegroundServiceUsed() {
        return lastTimeForegroundServiceUsed;
    }

    public void setLastTimeForegroundServiceUsed(long lastTimeForegroundServiceUsed) {
        this.lastTimeForegroundServiceUsed = lastTimeForegroundServiceUsed;
    }

    public long getTotalTimeForegroundServiceUsed() {
        return totalTimeForegroundServiceUsed;
    }

    public void setTotalTimeForegroundServiceUsed(long totalTimeForegroundServiceUsed) {
        this.totalTimeForegroundServiceUsed = totalTimeForegroundServiceUsed;
    }

    public int getDescribeContents() {
        return describeContents;
    }

    public void setDescribeContents(int describeContents) {
        this.describeContents = describeContents;
    }
}
