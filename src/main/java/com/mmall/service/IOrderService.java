package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.vo.OrderVo;

import java.util.Map;

/**
 * Created by 钟奉池 on 2018/6/25.
 */

public interface IOrderService {
    ServerResponse pay(long orderNo, Integer userId, String path);
    ServerResponse alipayCallback(Map<String, String> params);
    ServerResponse queryOrderPayStatus(Integer userId,long orderId);
    ServerResponse create(Integer userId,Integer shippingId);
    ServerResponse<String> cancel(Integer userId,long orderNo);
    ServerResponse getOrderCartProduct(Integer userId);
    ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo);
    ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize);
    ServerResponse<PageInfo> manageList(int pageNum, int pageSize);
    ServerResponse<OrderVo> manageDetail(Long orderNo);
    ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize);
    ServerResponse<String> manageSendGoods(Long orderNo);
}
