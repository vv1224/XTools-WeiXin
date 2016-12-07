package me.xuxiaoxiao.xtools.weixin.communicate;

import java.util.ArrayList;

public class ReqBatchGetContact {
    public final BaseRequest BaseRequest;
    public final int Count;
    public final ArrayList<Chatroom> List;

    public ReqBatchGetContact(BaseRequest BaseRequest, ArrayList<Chatroom> List) {
        this.BaseRequest = BaseRequest;
        this.Count = List.size();
        this.List = List;
    }

    public static class Chatroom {
        public final String UserName;
        public final String ChatRoomId;

        public Chatroom(String UserName, String ChatRoomId) {
            this.UserName = UserName;
            this.ChatRoomId = ChatRoomId;
        }
    }
}
