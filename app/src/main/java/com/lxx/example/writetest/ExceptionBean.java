package com.lxx.example.writetest;

import java.io.Serializable;
import java.util.List;

public class ExceptionBean implements Serializable {
    private int id;
    private String time;
    private String content;
    private static List<ExceptionBean> exceptionList;

    ExceptionBean(int id,String time,String content){
        this.id = id;
        this.time =time;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static List<ExceptionBean> getExceptionList() {
        return exceptionList;
    }

    public static void setExceptionList(List<ExceptionBean> exceptionList) {
        ExceptionBean.exceptionList = exceptionList;
    }

    public static void clearExceptionList(){
        exceptionList.clear();
    }
}
