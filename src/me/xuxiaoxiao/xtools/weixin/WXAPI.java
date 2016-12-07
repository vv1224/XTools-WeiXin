package me.xuxiaoxiao.xtools.weixin;

import me.xuxiaoxiao.xtools.common.XHttpTools;
import me.xuxiaoxiao.xtools.common.XHttpTools.XBody;
import me.xuxiaoxiao.xtools.common.XHttpTools.XBody.Type;
import me.xuxiaoxiao.xtools.common.XHttpTools.XUrl;
import me.xuxiaoxiao.xtools.common.XStrTools;
import me.xuxiaoxiao.xtools.weixin.communicate.*;
import me.xuxiaoxiao.xtools.weixin.communicate.ReqBatchGetContact.Chatroom;
import me.xuxiaoxiao.xtools.weixin.communicate.ReqSendMsg.Msg;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

class WXAPI {
    public static final String HOST_V1 = "wx.qq.com";
    public static final String HOST_V2 = "wx2.qq.com";
    public final long TIME_INIT = System.currentTimeMillis();
    public AtomicBoolean firstLogin = new AtomicBoolean(true);
    public long lastNotify = 0;
    public long time = TIME_INIT;
    public String host;

    public String uuid;
    public String uin;
    public String sid;
    public String skey;
    public String passticket;
    public RspInit.SyncKey synckey;

    public void jslogin() {
        XUrl xUrl = XUrl.base("https://login.wx.qq.com/jslogin");
        xUrl.param("_", time++);
        xUrl.param("appid", "wx782c26e4c19acffb");
        xUrl.param("fun", "new");
        xUrl.param("lang", "zh_CN");
        xUrl.param("redirect_uri", "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage");
        String rspStr = WXTools.request(xUrl, null, "\".+\"");
        this.uuid = rspStr.substring(rspStr.indexOf('"') + 1, rspStr.lastIndexOf('"'));
    }

    public String qrcode() {
        return "https://login.weixin.qq.com/qrcode/" + uuid;
    }

    public RspLogin login() {
        XUrl xUrl = XUrl.base("https://login.wx.qq.com/cgi-bin/mmwebwx-bin/login");
        xUrl.param("_", time++);
        xUrl.param("loginicon", true);
        xUrl.param("r", (int) (~(System.currentTimeMillis())));
        xUrl.param("tip", firstLogin.getAndSet(false) ? 1 : 0);
        xUrl.param("uuid", uuid);
        RspLogin rspLogin = new RspLogin(WXTools.request(xUrl, null, "window"));
        if (!XStrTools.isEmpty(rspLogin.redirectUri)) {
            if (rspLogin.redirectUri.contains(HOST_V1)) {
                this.host = HOST_V1;
            } else {
                this.host = HOST_V2;
            }
        }
        return rspLogin;
    }

    public void webwxnewloginpage(String url) {
        String rspStr = XHttpTools.request(XUrl.base(url), null).string();
        if (!XStrTools.isEmpty(rspStr) && Pattern.compile("<error>.+</error>").matcher(rspStr).find()) {
            this.uin = rspStr.substring(rspStr.indexOf("<wxuin>") + "<wxuin>".length(), rspStr.indexOf("</wxuin>"));
            this.sid = rspStr.substring(rspStr.indexOf("<wxsid>") + "<wxsid>".length(), rspStr.indexOf("</wxsid>"));
            this.skey = rspStr.substring(rspStr.indexOf("<skey>") + "<skey>".length(), rspStr.indexOf("</skey>"));
            this.passticket = rspStr.substring(rspStr.indexOf("<pass_ticket>") + "<pass_ticket>".length(), rspStr.indexOf("</pass_ticket>"));
        }
    }

    public RspInit webwxinit() {
        XUrl xUrl = XUrl.base(String.format("https://%s/cgi-bin/mmwebwx-bin/webwxinit", host));
        xUrl.param("r", (int) (~(this.TIME_INIT)));
        if (!XStrTools.isEmpty(this.passticket)) {
            xUrl.param("pass_ticket", this.passticket);
        }
        XBody body = XBody.type(Type.JSON).param(WXTools.GSON.toJson(new ReqInit(new BaseRequest(uin, sid, skey))));
        RspInit rspInit = WXTools.GSON.fromJson(WXTools.request(xUrl, body, "\\{"), RspInit.class);
        this.skey = rspInit.SKey;
        this.synckey = rspInit.SyncKey;
        return rspInit;
    }

    public RspStatusNotify webwxstatusnotify(String MyName) {
        XUrl xUrl = XUrl.base(String.format("https://%s/cgi-bin/mmwebwx-bin/webwxstatusnotify", host));
        if (!XStrTools.isEmpty(this.passticket)) {
            xUrl.param("pass_ticket", this.passticket);
        }
        XBody body = XBody.type(Type.JSON).param(WXTools.GSON.toJson(new ReqStatusNotify(new BaseRequest(uin, sid, skey), lastNotify == 0 ? 3 : 1, MyName)));
        return WXTools.GSON.fromJson(WXTools.request(xUrl, body, "\\{"), RspStatusNotify.class);
    }

    public RspGetContact webwxgetcontact() {
        XUrl xUrl = XUrl.base(String.format("https://%s/cgi-bin/mmwebwx-bin/webwxgetcontact", host));
        xUrl.param("r", System.currentTimeMillis());
        xUrl.param("seq", 0);
        xUrl.param("skey", this.skey);
        if (!XStrTools.isEmpty(this.passticket)) {
            xUrl.param("pass_ticket", this.passticket);
        }
        return WXTools.GSON.fromJson(WXTools.request(xUrl, null, "\\{"), RspGetContact.class);
    }

    public RspBatchGetContact webwxbatchgetcontact(ArrayList<Chatroom> chatRoomList) {
        XUrl xUrl = XUrl.base(String.format("https://%s/cgi-bin/mmwebwx-bin/webwxbatchgetcontact", host));
        xUrl.param("r", System.currentTimeMillis());
        xUrl.param("type", "ex");
        if (!XStrTools.isEmpty(this.passticket)) {
            xUrl.param("pass_ticket", this.passticket);
        }
        XBody body = XBody.type(Type.JSON).param(WXTools.GSON.toJson(new ReqBatchGetContact(new BaseRequest(uin, sid, skey), chatRoomList)));
        return WXTools.GSON.fromJson(WXTools.request(xUrl, body, "\\{"), RspBatchGetContact.class);
    }

    public RspSyncCheck synccheck() {
        XUrl xUrl = XUrl.base(String.format("https://webpush.%s/cgi-bin/mmwebwx-bin/synccheck", host));
        xUrl.param("uin", this.uin);
        xUrl.param("sid", this.sid);
        xUrl.param("skey", this.skey);
        xUrl.param("deviceId", BaseRequest.deviceId());
        xUrl.param("synckey", this.synckey);
        xUrl.param("r", System.currentTimeMillis());
        xUrl.param("_", time++);
        return new RspSyncCheck(WXTools.request(xUrl, null, "\\{(.|\\s)+\\}"));
    }

    public RspSync webwxsync() {
        XUrl xUrl = XUrl.base(String.format("https://%s/cgi-bin/mmwebwx-bin/webwxsync", host));
        xUrl.param("sid", this.sid);
        xUrl.param("skey", this.skey);
        if (!XStrTools.isEmpty(this.passticket)) {
            xUrl.param("pass_ticket", this.passticket);
        }
        XBody body = XBody.type(Type.JSON).param(WXTools.GSON.toJson(new ReqSync(new BaseRequest(uin, sid, skey), this.synckey)));
        RspSync rspSync = WXTools.GSON.fromJson(WXTools.request(xUrl, body, "\\{"), RspSync.class);
        this.synckey = rspSync.SyncKey;
        return rspSync;
    }

    public RspSendMsg webwxsendmsg(Msg msg) {
        XUrl xUrl = XUrl.base(String.format("https://%s/cgi-bin/mmwebwx-bin/webwxsendmsg", host));
        if (!XStrTools.isEmpty(this.passticket)) {
            xUrl.param("pass_ticket", passticket);
        }
        XBody body = XBody.type(Type.JSON).param(WXTools.GSON.toJson(new ReqSendMsg(new BaseRequest(uin, sid, skey), msg)));
        return WXTools.GSON.fromJson(WXTools.request(xUrl, body, "\\{"), RspSendMsg.class);
    }
}