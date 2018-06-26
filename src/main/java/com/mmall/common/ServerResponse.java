package com.mmall.common;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

/**
 * 高复用的服务器端响应对象
 * Created by 钟奉池 on 2018/6/10.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)//null的key会消失
public class ServerResponse<T> implements Serializable {
    private int status;
    private String msg;
    private T data;

    private ServerResponse(int status) {
        this.status = status;
    }
    private ServerResponse(T data, int status) {
        this.data = data;
        this.status = status;
    }
    private ServerResponse(T data, String msg, int status) {
        this.data = data;
        this.msg = msg;
        this.status = status;
    }
    private ServerResponse(int status,String msg) {
        this.msg = msg;
        this.status = status;
    }
    @JsonIgnore//使之不在json序列化结果中
    public boolean isSuccess(){
        return this.status == ResponseCode.SUCCESS.getCode();
    }

    public T getData() {
        return data;
    }
    public String getMsg() {
        return msg;
    }
    public int getStatus() {
        return status;
    }

    public static <T> ServerResponse<T> createBySuccess(){
        return new ServerResponse<>(ResponseCode.SUCCESS.getCode());
    }
    public  static <T> ServerResponse<T> createBySuccessMessage(String msg){
        return new ServerResponse<>(ResponseCode.SUCCESS.getCode(),msg);
    }
    public  static <T> ServerResponse<T> createBySuccess(T data){
        return new ServerResponse<>(data,ResponseCode.SUCCESS.getCode());
    }
    public  static <T> ServerResponse<T> createBySuccess(T data,String msg){
        return new ServerResponse<>(data,msg,ResponseCode.SUCCESS.getCode());
    }

    public static <T> ServerResponse<T> createByError(){
        return new ServerResponse<>(ResponseCode.ERROR.getCode(),ResponseCode.ERROR.getDesc());
    }
    public static <T> ServerResponse<T> createByErrorMessage(String msg){
        return new ServerResponse<>(ResponseCode.ERROR.getCode(),msg);
    }

    public static <T> ServerResponse<T> createByErrorCodeMessage(int errorCode, String msg){
        return new ServerResponse<>(errorCode, msg);
    }
}
