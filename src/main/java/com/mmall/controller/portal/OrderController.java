package com.mmall.controller.portal;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by 钟奉池 on 2018/6/25.
 */
@Controller
@RequestMapping("/order/")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    @Autowired
    private IOrderService iOrderService;
    //预下单（提交订单生成供客户付款的二维码图片）
    @RequestMapping("pay.do")
    @ResponseBody
    public ServerResponse pay(HttpSession session, long orderNo, HttpServletRequest request){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        String path = request.getSession().getServletContext().getRealPath("upload");
        return iOrderService.pay(orderNo,user.getId(),path);
    }
    //支付宝回调方法
    @RequestMapping("alipay_callback.do")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request){
        Map<String ,String> map = Maps.newHashMap();
        //支付宝会将回调封装为一个Map并存入request中
        Map<String, String[]> parameterMap = request.getParameterMap();
        for(Iterator iterator = parameterMap.keySet().iterator();iterator.hasNext();){
            String name = (String)iterator.next();
            String[] values = parameterMap.get(name);
            String valueStr = "";
            for(int i = 0;i < values.length;i++){
                //当遍历到最后一个元素时直接拼接，如果遍历最后一个元素之前就用逗号拼接
                valueStr = (i == values.length - 1)?valueStr + values[i]:valueStr + values[i] + ",";
            }
            //将key和拼接好的value存入自定义的Map中
            map.put(name,valueStr);
        }
        //在日志中显示回调的信息
        logger.info("支付宝回调，签名sign:{},交易状态trade_status:{},所有参数:{}",map.get("sign"),map.get("trade_status"),map.toString());
        //验证回调信息是否为支付宝所发送的，避免重复通知
        map.remove("sign_type");//在通知返回参数列表中，除去sign、sign_type两个参数外，凡是通知返回回来的参数皆是待验签的参数
        try {// 验签
            boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(map, Configs.getAlipayPublicKey(),
                    "utf-8", Configs.getSignType());
            if (!alipayRSACheckedV2) {
                return ServerResponse.createByErrorMessage("非法请求,验证不通过,再恶意请求我就报警找网警了");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝回调异常", e);
        }

        /* todo 商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
        并判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），同时需要校验通知中的
        seller_id（或者seller_email) 是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户
        可能有多个seller_id/seller_email），上述有任何一个验证不通过，则表明本次通知是异常通知，务必
        忽略。在上述验证通过后商户必须根据支付宝不同类型的业务通知，正确的进行不同的业务处理，并且过滤重
        复的通知结果数据。在支付宝的业务通知中，只有交易通知状态为TRADE_SUCCESS或TRADE_FINISHED时，
        支付宝才会认定为买家付款成功*/

        ServerResponse serverResponse = iOrderService.alipayCallback(map);
        if (serverResponse.isSuccess()) {
            //向支付宝返回回调处理结果
            return Const.AlipayClallback.RESPONSE_SUCCESS;
        }
        //向支付宝返回回调处理结果
        return Const.AlipayClallback.RESPONSE_FAILED;
    }
    //轮询支付结果（当二维码生成后展示给用户付款时，轮番查看支付情况以返回给前端使之做下一步处理）
    @RequestMapping("query_order_pay_status.do")
    @ResponseBody//与前端约定布尔值
    public ServerResponse<Boolean> queryOrderPayStatus(HttpSession session, long orderNo){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        ServerResponse serverResponse = iOrderService.queryOrderPayStatus(user.getId(),orderNo);
        if(serverResponse.isSuccess()){
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }
    //创建订单
    @RequestMapping("create.do")
    @ResponseBody
    public ServerResponse create(HttpSession session,Integer shippingId){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.create(user.getId(),shippingId);
    }
    //未付款时取消订单
    @RequestMapping("cancel.do")
    @ResponseBody
    public ServerResponse cancel(HttpSession session,long orderNo){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.cancel(user.getId(),orderNo);
    }
    //获取购物车中已选中的产品信息,主要服务于订单确认页面
    @RequestMapping("get_order_cart_product.do")
    @ResponseBody
    public ServerResponse getOrderCartProduct(HttpSession session){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderCartProduct(user.getId());
    }
    //用户个人中心的订单详情
    @RequestMapping(value = "detail.do")
    @ResponseBody
    public ServerResponse detail(HttpSession httpSession, Long orderNo) {// 订单填写页面
        User user = (User) httpSession.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderDetail(user.getId(), orderNo);
    }
    //个人中心订单列表
    @RequestMapping(value = "list.do")
    @ResponseBody
    public ServerResponse list(HttpSession httpSession,
                               @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                               @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        User user = (User) httpSession.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderList(user.getId(), pageNum, pageSize);
    }
}
