package com.mmall.dao;

import com.mmall.pojo.Shipping;
import com.mmall.pojo.ShippingExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShippingMapper {
    int countByExample(ShippingExample example);

    int deleteByExample(ShippingExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(Shipping record);

    int insertSelective(Shipping record);

    List<Shipping> selectByExample(ShippingExample example);

    Shipping selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") Shipping record, @Param("example") ShippingExample example);

    int updateByExample(@Param("record") Shipping record, @Param("example") ShippingExample example);

    int updateByPrimaryKeySelective(Shipping record);

    int updateByPrimaryKey(Shipping record);

    int deleteByShippingIdAndUserId(@Param("shippingId") Integer shippingId,@Param("userId") Integer userId);

    int updateShippingByIdAndUserId(Shipping shipping);

    Shipping selectShippingByIdAndUserId(@Param("shippingId") Integer shippingId,@Param("userId") Integer userId);

    List<Shipping> selectShippingsByUserId(Integer userId);
}