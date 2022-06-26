package com.lmwis.appusage.rpc;

public enum ActionEnum {

    SCREEN_OPEN(1,"屏幕打开"),
    SCREEN_CLOSE(2,"屏幕关闭"),
    SCREEN_LOCK(3,"屏幕解锁")

    ;

    ActionEnum(int actionCode, String actionName) {
        this.actionCode = actionCode;
        this.actionName = actionName;
    }

    int actionCode;

    String actionName;

    public int getActionCode() {
        return actionCode;
    }

    public void setActionCode(int actionCode) {
        this.actionCode = actionCode;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
}
