package com.lmwis.appusage.rpc.pojo;

import java.util.List;

public class BatchUploadAppUsagesDTO {

    List<AppUsagesDTO> list;

    long startTime;

    long endTime;

    public List<AppUsagesDTO> getList() {
        return list;
    }

    public void setList(List<AppUsagesDTO> list) {
        this.list = list;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
