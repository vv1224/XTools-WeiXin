package me.xuxiaoxiao.xtools.weixin;

import me.xuxiaoxiao.xtools.common.XStrTools;
import me.xuxiaoxiao.xtools.weixin.communicate.ReqBatchGetContact.Chatroom;
import me.xuxiaoxiao.xtools.weixin.communicate.ReqSendMsg.Msg;
import me.xuxiaoxiao.xtools.weixin.communicate.*;
import me.xuxiaoxiao.xtools.weixin.communicate.RspInit.User;
import me.xuxiaoxiao.xtools.weixin.communicate.RspSync.AddMsg;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;

public final class WXChat {
    public static final String LOGIN_TIMEOUT = "登陆超时";
    public static final String LOGIN_EXCEPTION = "登陆异常";
    public static final String INIT_EXCEPTION = "初始化异常";
    public static final String LISTEN_EXCEPTION = "监听异常";

    private final WXAPI wxAPI = new WXAPI();
    private final WXThread wxThread = new WXThread();
    private final CookieManager cookieManager;
    private final WXChatListener wxChatListener;
    private User me;
    private HashMap<String, User> contacts = new HashMap<>();

    public WXChat(CookieManager cookieManager, WXChatListener wxChatListener) {
        this.cookieManager = cookieManager;
        this.wxChatListener = wxChatListener;
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    public void startup() {
        wxThread.start();
    }

    public boolean isWorking() {
        return !wxThread.isInterrupted();
    }

    public void shutdown() {
        wxThread.interrupt();
    }

    private void loadChatrooms(ArrayList<Chatroom> chatrooms) {
        RspBatchGetContact rspBatchGetContact = wxAPI.webwxbatchgetcontact(chatrooms);
        for (User user : rspBatchGetContact.ContactList) {
            this.contacts.put(user.UserName, user);
            if (user.UserName.startsWith("@@")) {
                for (User member : user.MemberList) {
                    this.contacts.put(member.UserName, member);
                }
            }
        }
    }

    public User me() {
        return this.me;
    }

    public HashMap<String, User> contacts() {
        return this.contacts;
    }

    public void sendTextMsg(String toUserName, String msgContent) {
        wxAPI.webwxsendmsg(new Msg(WXTools.TYPE_TEXT, msgContent, this.me.UserName, toUserName));
    }

    public interface WXChatListener {
        void onQRCode(String qrCode);

        void onAvatar(String base64Avatar);

        void onFailure(String reason);

        void onLogin();

        void onMessage(AddMsg addMsg);

        void onLogout();
    }

    private class WXThread extends Thread {

        @Override
        public void run() {
            String loginErr = login();
            if (!XStrTools.isEmpty(loginErr)) {
                wxChatListener.onFailure(loginErr);
                return;
            }
            String initErr = initial();
            if (!XStrTools.isEmpty(initErr)) {
                wxChatListener.onFailure(initErr);
                return;
            }
            wxChatListener.onLogin();
            String listenErr = listen();
            if (!XStrTools.isEmpty(listenErr)) {
                wxChatListener.onFailure(listenErr);
                return;
            }
            wxChatListener.onLogout();
        }

        private String login() {
            try {
                wxAPI.jslogin();
                wxChatListener.onQRCode(wxAPI.qrcode());
                while (true) {
                    RspLogin rspLogin = wxAPI.login();
                    switch (rspLogin.code) {
                        case 200:
                            wxAPI.webwxnewloginpage(rspLogin.redirectUri);
                            return null;
                        case 201:
                            wxChatListener.onAvatar(rspLogin.userAvatar);
                            break;
                        case 408:
                            break;
                        default:
                            return LOGIN_TIMEOUT;
                    }
                }
            } catch (Exception e) {
                return LOGIN_EXCEPTION;
            }
        }

        private String initial() {
            try {
                for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
                    if (cookie.getName().equalsIgnoreCase("wxsid")) {
                        wxAPI.sid = cookie.getValue();
                    }
                    if (cookie.getName().equalsIgnoreCase("wxuin")) {
                        wxAPI.uin = cookie.getValue();
                    }
                }

                RspInit rspInit = wxAPI.webwxinit();
                WXChat.this.me = rspInit.User;

                RspGetContact rspGetContact = wxAPI.webwxgetcontact();
                for (User user : rspGetContact.MemberList) {
                    WXChat.this.contacts.put(user.UserName, user);
                }

                ArrayList<Chatroom> chatrooms = new ArrayList<>();
                if (rspInit.ContactList != null) {
                    for (User user : rspInit.ContactList) {
                        if (user.UserName.startsWith("@@")) {
                            Chatroom item = new Chatroom(user.UserName, "");
                            chatrooms.add(item);
                        }
                    }
                }
                loadChatrooms(chatrooms);
                return null;
            } catch (Exception e) {
                return INIT_EXCEPTION;
            }
        }

        private String listen() {
            try {
                while (!isInterrupted()) {
                    RspSyncCheck rspSyncCheck = wxAPI.synccheck();
                    if (rspSyncCheck.retcode > 0) {
                        return null;
                    } else if (rspSyncCheck.selector > 0) {
                        RspSync rspSync = wxAPI.webwxsync();
                        if (rspSync.AddMsgList != null) {
                            for (AddMsg addMsg : rspSync.AddMsgList) {
                                if (addMsg.FromUserName.startsWith("@@") && !WXChat.this.contacts.containsKey(addMsg.FromUserName)) {
                                    ArrayList<Chatroom> chatRooms = new ArrayList<>();
                                    chatRooms.add(new Chatroom(addMsg.FromUserName, ""));
                                    loadChatrooms(chatRooms);
                                }
                                wxChatListener.onMessage(addMsg);
                            }
                        }
                        if (rspSync.ModContactList != null) {
                            for (User user : rspSync.ModContactList) {
                                WXChat.this.contacts.put(user.UserName, user);
                            }
                        }
                        if (rspSync.DelContactList != null) {
                            for (User user : rspSync.DelContactList) {
                                WXChat.this.contacts.remove(user.UserName);
                            }
                        }
                        if (rspSync.ModChatRoomMemberList != null) {
                            for (User user : rspSync.ModChatRoomMemberList) {
                                WXChat.this.contacts.put(user.UserName, user);
                            }
                        }
                    }
                    if (System.currentTimeMillis() - wxAPI.lastNotify > 5 * 60 * 1000) {
                        wxAPI.webwxstatusnotify(WXChat.this.me.UserName);
                        wxAPI.lastNotify = System.currentTimeMillis();
                    }
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return LISTEN_EXCEPTION;
            }
        }
    }
}
