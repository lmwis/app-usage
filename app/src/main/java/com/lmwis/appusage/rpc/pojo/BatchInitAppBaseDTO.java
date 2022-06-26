package com.lmwis.appusage.rpc.pojo;

import java.util.List;

public class BatchInitAppBaseDTO {
    List<AppBaseDTO> list;

    public List<AppBaseDTO> getList() {
        return list;
    }

    public void setList(List<AppBaseDTO> list) {
        this.list = list;
    }
}
