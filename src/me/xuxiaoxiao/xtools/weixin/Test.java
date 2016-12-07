package me.xuxiaoxiao.xtools.weixin;

import me.xuxiaoxiao.xtools.weixin.communicate.RspSync;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class Test {
    public static final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    public static WXChat wxChat = new WXChat(cookieManager, new WXChat.WXChatListener() {
        @Override
        public void onQRCode(String qrCode) {
            System.out.println("onQRCode:" + qrCode);
        }

        @Override
        public void onAvatar(String base64Avatar) {
        }

        @Override
        public void onFailure(String reason) {
            System.out.println("onFailure:" + reason);

        }

        @Override
        public void onLogin() {
            System.out.println("onLogin");
        }

        @Override
        public void onMessage(RspSync.AddMsg addMsg) {
            System.out.println("onMessage:" + addMsg.Content);
        }

        @Override
        public void onLogout() {
            System.out.println("onLogout");
        }
    });

    public static void main(String[] args) {
        CookieHandler.setDefault(cookieManager);
        wxChat.startup();
    }
}
