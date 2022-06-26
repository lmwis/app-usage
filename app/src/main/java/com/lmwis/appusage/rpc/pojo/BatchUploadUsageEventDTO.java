package com.lmwis.appusage.rpc.pojo;

import java.util.List;

public class BatchUploadUsageEventDTO {

    List<UsageEventDTO> list;

    public List<UsageEventDTO> getList() {
        return list;
    }

    public void setList(List<UsageEventDTO> list) {
        this.list = list;
    }

}
