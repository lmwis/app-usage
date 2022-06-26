package com.lmwis.appusage.rpc.pojo;

import java.util.List;

public class BatchUploadIphonePostureDTO {

    List<IphonePostureDTO> list;

    @Override
    public String toString() {
        return "BatchUploadIphonePostureDTO{" +
                "list=" + list +
                '}';
    }

    public List<IphonePostureDTO> getList() {
        return list;
    }

    public void setList(List<IphonePostureDTO> list) {
        this.list = list;
    }
}
