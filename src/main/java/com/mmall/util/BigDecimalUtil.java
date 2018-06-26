package com.mmall.util;

import java.math.BigDecimal;

/**
 * 使用BigDecimal的String构造器才能用于准确的商业计算以避免丢失精度
 * Created by 钟奉池 on 2018/6/23.
 */
public class BigDecimalUtil {
    private BigDecimalUtil(){}//使外部不能实例化该工具类
    public static BigDecimal add0(double v1,double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.add(b2);
    }
    public static BigDecimal sub(double v1,double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.subtract(b2);
    }
    public static BigDecimal mul(double v1, double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.multiply(b2);
    }
    public static BigDecimal div(double v1,double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2,2,BigDecimal.ROUND_HALF_UP);//保留两位小数，四舍五入
    }
}
