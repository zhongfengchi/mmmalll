package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Shipping;

/**
 * Created by 钟奉池 on 2018/6/23.
 */
public interface IShippingService {
    ServerResponse add(Integer userId, Shipping shipping);
    ServerResponse<String> del(Integer shippingId,Integer userId);
    ServerResponse<String> update(Shipping shipping,Integer userId);
    ServerResponse<Shipping> select(Integer shippingId,Integer userId);
    ServerResponse<PageInfo> list(Integer userId, int pageNum, int pageSize);
}
