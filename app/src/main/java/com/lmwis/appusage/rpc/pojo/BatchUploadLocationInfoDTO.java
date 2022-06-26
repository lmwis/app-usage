package com.lmwis.appusage.rpc.pojo;

import java.util.List;

public class BatchUploadLocationInfoDTO {

    List<LocationInfoDTO> list;

    public List<LocationInfoDTO> getList() {
        return list;
    }

    public void setList(List<LocationInfoDTO> list) {
        this.list = list;
    }
}
