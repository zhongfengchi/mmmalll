package com.mmall.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.mmall.common.ServerResponse;
import com.mmall.dao.ShippingMapper;
import com.mmall.pojo.Shipping;
import com.mmall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * Created by 钟奉池 on 2018/6/23.
 */
@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService {
    @Autowired
    private ShippingMapper shippingMapper;
    //增加收货地址
    public ServerResponse add(Integer userId,Shipping shipping){
        shipping.setUserId(userId);
        int count = shippingMapper.insert(shipping);
        if(count > 0){
            Map result = Maps.newHashMap();
            result.put("shippingId",shipping.getId());
            return ServerResponse.createBySuccess(result,"新增地址成功");
        }
        return ServerResponse.createByErrorMessage("新增地址失败");
    }
    //删除收货地址
    public ServerResponse<String> del(Integer shippingId,Integer userId){
        //此时会有横向越权问题，即用户删除不是本用户的收货地址
        //int count = shippingMapper.deleteByPrimaryKey(shippingId);
        int count = shippingMapper.deleteByShippingIdAndUserId(shippingId,userId);
        if(count > 0){
            return ServerResponse.createBySuccess("删除收货成功");
        }
        return ServerResponse.createByErrorMessage("删除收货失败");
    }
    //更新收货地址
    public ServerResponse<String> update(Shipping shipping,Integer userId){
        shipping.setUserId(userId);//此处再次将userId（当前登录用户的）赋予shipping是为了防止从其他途径传入userId
        int count = shippingMapper.updateShippingByIdAndUserId(shipping);
        if(count > 0){
            return ServerResponse.createBySuccess("更新收货地址成功");
        }
        return ServerResponse.createByErrorMessage("更新收货地址失败");
    }
    //查询收货地址详情
    public ServerResponse<Shipping> select(Integer shippingId,Integer userId){
        Shipping shipping = shippingMapper.selectShippingByIdAndUserId(shippingId,userId);
        if(shipping != null){
            return ServerResponse.createBySuccess(shipping,"查询成功");
        }
        return ServerResponse.createByErrorMessage("无法查询到该地址");
    }
    //收货地址分页列表
    public ServerResponse<PageInfo> list(Integer userId,int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Shipping> shippingList = shippingMapper.selectShippingsByUserId(userId);
        PageInfo pageInfo = new PageInfo(shippingList);
        return ServerResponse.createBySuccess(pageInfo);
    }
}
