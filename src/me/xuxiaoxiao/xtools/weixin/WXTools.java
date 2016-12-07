package me.xuxiaoxiao.xtools.weixin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.xuxiaoxiao.xtools.common.XHttpTools;
import me.xuxiaoxiao.xtools.common.XHttpTools.XBody;
import me.xuxiaoxiao.xtools.common.XHttpTools.XUrl;
import me.xuxiaoxiao.xtools.common.XStrTools;
import me.xuxiaoxiao.xtools.weixin.communicate.RspSync.AddMsg;

import java.util.regex.Pattern;

public class WXTools {
    public static final Gson GSON = new GsonBuilder().create();
    public static final int TYPE_TEXT = 1;

    private WXTools() {
    }

    public static boolean isGroupMsg(AddMsg addMsg) {
        return addMsg.FromUserName.startsWith("@@");
    }

    public static String textMsgSender(AddMsg addMsg) {
        if (addMsg.AppMsgType == TYPE_TEXT && isGroupMsg(addMsg)) {
            return addMsg.Content.substring(0, addMsg.Content.indexOf(':'));
        } else {
            return addMsg.FromUserName;
        }
    }

    public static String textMsgContent(AddMsg addMsg) {
        if (addMsg.AppMsgType == TYPE_TEXT && isGroupMsg(addMsg)) {
            return addMsg.Content.substring(addMsg.Content.indexOf("<br/>") + "<br/>".length());
        } else {
            return addMsg.Content;
        }
    }


    public static String request(XUrl url, XBody body, String regex) {
        for (int i = 0; i < 5; i++) {
            String respStr = XHttpTools.request(new XHttpTools.XConfig("utf-8", 60 * 1000, 60 * 1000), url, body).string();
            if (!XStrTools.isEmpty(respStr) && Pattern.compile(regex).matcher(respStr).find()) {
                return respStr;
            }
        }
        return null;
    }
}
