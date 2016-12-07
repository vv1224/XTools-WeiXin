package me.xuxiaoxiao.xtools.weixin.communicate;

import me.xuxiaoxiao.xtools.weixin.communicate.RspInit.User;

import java.util.ArrayList;

public class RspGetContact {
    public BaseResponse BaseResponse;
    public int MemberCount;
    public ArrayList<User> MemberList;
    public int Seq;
}
