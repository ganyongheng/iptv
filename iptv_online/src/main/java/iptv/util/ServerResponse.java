package iptv.util;


import java.io.Serializable;


/**
 * YouKuRequstUtils使用
 * @param <T>
 */
public class ServerResponse<T> implements Serializable {

    private int status;

    private String msg;

    private T data;


    public ServerResponse(int status){
        this.status=status;
    }

    public ServerResponse(int status ,String msg){
        this.status=status;
        this.msg=msg;
    }

    public ServerResponse(int status ,String msg,T data){
        this.status=status;
        this.msg=msg;
        this.data=data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
