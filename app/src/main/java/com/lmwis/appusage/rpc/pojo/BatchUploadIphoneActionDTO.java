package com.lmwis.appusage.rpc.pojo;

import java.util.List;

public class BatchUploadIphoneActionDTO {

    List<IphoneActionDTO> list;

    @Override
    public String toString() {
        return "BatchUploadIphoneActionDTO{" +
                "list=" + list +
                '}';
    }

    public List<IphoneActionDTO> getList() {
        return list;
    }

    public void setList(List<IphoneActionDTO> list) {
        this.list = list;
    }
}
