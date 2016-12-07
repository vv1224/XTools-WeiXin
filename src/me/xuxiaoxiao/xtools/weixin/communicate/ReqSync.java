package me.xuxiaoxiao.xtools.weixin.communicate;

import me.xuxiaoxiao.xtools.weixin.communicate.RspInit.SyncKey;

public class ReqSync {
    public final BaseRequest BaseRequest;
    public final SyncKey SyncKey;
    public final int rr;

    public ReqSync(BaseRequest BaseRequest, SyncKey SyncKey) {
        this.BaseRequest = BaseRequest;
        this.SyncKey = SyncKey;
        this.rr = (int) (~(System.currentTimeMillis()));
    }
}
