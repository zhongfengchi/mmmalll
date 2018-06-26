package com.mmall.service.Impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.*;
import com.mmall.pojo.*;
import com.mmall.service.IOrderService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.FTPUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.OrderItemVo;
import com.mmall.vo.OrderProductVo;
import com.mmall.vo.OrderVo;
import com.mmall.vo.ShippingVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;


/**
 * Created by 钟奉池 on 2018/6/25.
 */
@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;
    public ServerResponse pay(long orderNo,Integer userId,String path){
        //定义一个Map用于向前端返回生成的二维码图片地址和订单编号
        Map<String,String> resultMap = Maps.newHashMap();
        //根据userId和orderNo查询订单
        Order order = orderMapper.selectOrderByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        //将待支付的订单编号存入Map中
        resultMap.put("orderNo",order.getOrderNo().toString());

        //获取支付请求对象builder各成员变量的值以封装该对象

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = "付款给HappyMmall";

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单号：").append(outTradeNo).append(",合计：" + totalAmount + "元").toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "商户操作员编号";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_商户门店编号_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        //根据userId和orderNo查询出订单明细列表
        List<OrderItem> orderItems = orderItemMapper.getOrderItemByOrderNoAndUserId(order.getOrderNo(),userId);
        if(orderItems == null){
            return ServerResponse.createByErrorMessage("该订单为空");
        }
        for(OrderItem orderItem : orderItems){
            GoodsDetail goods1 = GoodsDetail.newInstance(orderItem.getProductId().toString(),orderItem.getProductName(),
                    BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(),new Double(100).doubleValue()).longValue(),orderItem.getQuantity());
            // 创建好一个商品后添加至商品明细列表
            // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
            goodsDetailList.add(goods1);
        }

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);

        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         *  AlipayTradeService提供当面付支付、查询、退款和预下单生成二维码的功能
         */
        AlipayTradeService tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();

        //调用AlipayTradeServiceImpl中tradePrecreate实现当面付2.0预下单(生成二维码)，返回预下单结果对象
        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);

        switch (result.getTradeStatus()) {//判断月下单结果对象中的交易状态
            case SUCCESS:
                logger.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse1(response);
                //利用path创建用于上传二维码图片的文件对象
                File folder = new File(path);
                if (!folder.exists()) {
                    folder.setWritable(true);
                    folder.mkdirs();
                }
                // 需要修改为运行机器上的路径（QR Code ：二维码）
                String qrPath = String.format(path + "/qr-%s.png", response.getOutTradeNo());
                //response.getOutTradeNo()将替换%
                String qrFileName = String.format("qr-%s.png", response.getOutTradeNo());
                /*利用google的zxing二维码生成工具将response.getQrCode()在qrPath（tomcat服务器上）
                中生成长宽均为256的二维码图片*/
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);

                File tartgetFile = new File(path, qrFileName);
                try {
                    //将tomcat服务器上的二维码图片上传至FTP图片服务器上
                    FTPUtil.uploadFile(Lists.newArrayList(tartgetFile));
                } catch (IOException e) {
                    logger.error("上传二维码图片失败",e);
                }
                logger.info("qrPath:" + qrPath);
                String qrUrl = "ftp://" + PropertiesUtil.getProperty("ftp.server.ip") + "/" + tartgetFile.getName();
                //将二维码图片的FTP地址存入resultMap中以返回给前端
                resultMap.put("qrUrl", qrUrl);
                return ServerResponse.createBySuccess(resultMap);

            case FAILED:
                logger.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败");

            case UNKNOWN:
                logger.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知");
            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常");
        }
    }
    // 简单打印应答
    private void dumpResponse1(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }
    // 回调方法：根据支付宝回调参数更新数据库中的订单信息
    public ServerResponse alipayCallback(Map<String, String> params) {
        //获取支付宝回调参数
        Long orderNo = Long.parseLong(params.get("out_trade_no"));//商户订单号
        String tradeNo = params.get("trade_no");// 支付宝交易号
        //交易状态：WAIT_BUYER_PAY 交易创建，等待买家付款，TRADE_CLOSED 未付款交易超时关闭，或支付完成后全额退款
        //TRADE_SUCCESS 交易支付成功，TRADE_FINISHED 交易结束，不可退款
        String tradeStatus = params.get("trade_status");
        //根据支付宝回调的商户订单号orderNo在数据库中查询是否有对应的订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("非本商城订单，回调忽略");
        }
        //根据支付宝回调得到的订单状态判断当前数据库中的订单状态
        if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {//此时无需处理回调，返回成功
            return ServerResponse.createBySuccess("支付宝重复调用");
        }
        //此时数据库中的订单状态为已取消或未支付状态，判断支付宝回调的支付状态是否为成功
        if (Const.AlipayClallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)) {
            //将付款时间设置为回调的付款时间
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            //将订单状态置为已付款状态
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());
            //将重置为已付款交易状态的订单更新回数据库
            orderMapper.updateByPrimaryKeySelective(order);
        }
        //组装payInfo对象以映射数据库中的mmall_pay_info表
        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformNumber(tradeNo);
        //支付宝回调交易状态为WAIT_BUYER_PAY时，不会进行更新mmall_order表的更新，但仍会
        //向mmall_pay_info表中插入数据，此时交易状态为等待买家付款
        payInfo.setPlatformStatus(tradeStatus);
        //将payInfo插入数据库
        payInfoMapper.insert(payInfo);

        return ServerResponse.createBySuccess();
    }

    //轮询支付结果（当二维码生成后展示给用户付款时，轮番查看支付情况以返回给前端使之做下一步处理）
    public ServerResponse queryOrderPayStatus(Integer userId,long orderId){
        Order order = orderMapper.selectOrderByUserIdAndOrderNo(userId,orderId);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        //根据支付宝回调得到的订单状态判断当前数据库中的订单状态
        if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {//此时无需处理回调，返回成功
            return ServerResponse.createBySuccess();
        }else {
            return ServerResponse.createByError();
        }
    }
    //创建订单
    public ServerResponse create(Integer userId,Integer shippingId){
        //从购物车中获取订单数据
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        //计算订单总价
        ServerResponse listServerResponse = this.getCartOrderItem(userId,cartList);
        if(!listServerResponse.isSuccess()){
            return listServerResponse;
        }
        List<OrderItem> orderItemList = (List<OrderItem>) listServerResponse.getData();
        if(CollectionUtils.isEmpty(orderItemList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }
        BigDecimal orderTotalPrice = this.getOrderTotalPrice(orderItemList);
        //生成订单
        Order order = this.assembleOrder(userId,shippingId,orderTotalPrice);
        if(order == null){
            return ServerResponse.createByErrorMessage("生成订单错误");
        }
        //设置子订单的订单编号
        for(OrderItem orderItem : orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
        }
        //mybatis批量操作插入mmall_order_item
        orderItemMapper.batchInsert(orderItemList);
        //减少产品库存
        this.reduceProductStock(orderItemList);
        //清空购物车
        this.cleanCart(cartList);
        OrderVo orderVo = this.assembleOrderVo(order, orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }
    private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItemList) {
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());
        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());

        orderVo.setShippingId(order.getShippingId());
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
        if (shipping != null) {
            orderVo.setReceiverName(shipping.getReceiverName());
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.ip"));
        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        for (OrderItem orderItem : orderItemList) {
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;
    }
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem) {
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());
        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));
        return orderItemVo;
    }

    private ShippingVo assembleShippingVo(Shipping shipping) {
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverPhone(shipping.getReceiverPhone());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        return shippingVo;
    }
    //清空购物车
    private void cleanCart(List<Cart> cartList){
        for (Cart cart : cartList){
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }
    //减少产品库存
    private void reduceProductStock(List<OrderItem> orderItemList){
        for(OrderItem orderItem : orderItemList){
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            product.setStock(product.getStock()-orderItem.getQuantity());
            productMapper.updateByPrimaryKeySelective(product);
        }
    }
    //组装订单对象
    private Order assembleOrder(Integer userId,Integer shippingId,BigDecimal orderTotalPrice){
        Order order = new Order();
        //生成订单号
        long orderNo = this.generateOrderNo();
        order.setOrderNo(orderNo);
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
        order.setPostage(0);
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
        order.setPayment(orderTotalPrice);
        order.setUserId(userId);
        order.setShippingId(shippingId);
        int count = orderMapper.insert(order);
        if(count > 0){
            return order;
        }
        return null;
    }
    //生成订单号(简易版)
    private long generateOrderNo(){
        long currentTime = System.currentTimeMillis();
        return currentTime + new Random().nextInt(100);
    }

    //计算订单总价
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList){
        BigDecimal orderTotalPrice = new BigDecimal("0");
        if(orderItemList == null){
            return new BigDecimal("0");
        }
        for(OrderItem orderItem : orderItemList){
            orderTotalPrice = BigDecimalUtil.add0(orderTotalPrice.doubleValue(),orderItem.getTotalPrice().doubleValue());
        }
        return orderTotalPrice;
    }
    //取出购物车中的每一条记录组装成一个子订单集合
    private ServerResponse getCartOrderItem(Integer userId,List<Cart> cartList){
        List<OrderItem> orderItemList = Lists.newArrayList();
        if(CollectionUtils.isEmpty(cartList)){
            return ServerResponse.createByErrorMessage("购物车中无订单");
        }
        //校验购物车中的数据（产品状态和数量等）
        for(Cart cart : cartList){
            OrderItem orderItem = new OrderItem();
            Product product = productMapper.selectByPrimaryKey(cart.getProductId());
            //校验产品状态
            if(Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()){
                return ServerResponse.createByErrorMessage("产品已下架或删除");
            }
            //校验产品库存
            if(cart.getQuantity() > product.getStock()){
                return ServerResponse.createByErrorMessage("产品" + product.getName() + "库存不足");
            }
            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cart.getQuantity()));
            orderItemList.add(orderItem);
        }
        return ServerResponse.createBySuccess(orderItemList);
    }
    //未付款时取消订单
    public ServerResponse<String> cancel(Integer userId,long orderNo){
        Order order = orderMapper.selectOrderByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("订单不存在或该用户不存在此订单");
        }
        if(order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()){
            return ServerResponse.createByErrorMessage("已付款订单，不可取消（一期）");
        }
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(Const.OrderStatusEnum.CANCELED.getCode());
        int count = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if(count > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }
    //获取购物车中已选中的产品信息,主要服务于 订单确认页面
    public ServerResponse getOrderCartProduct(Integer userId){
        OrderProductVo orderProductVo = new OrderProductVo();

        List<Cart> cartList = cartMapper.selectCartByUserId(userId);
        ServerResponse serverResponse = this.getCartOrderItem(userId, cartList);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }
        List<OrderItem> orderItemList = (List<OrderItem>) serverResponse.getData();

        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        BigDecimal payment = new BigDecimal("0");
        for (OrderItem orderItem : orderItemList) {
            payment = BigDecimalUtil.add0(payment.doubleValue(), orderItem.getTotalPrice().doubleValue());
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderProductVo.setTotalPrice(payment);
        orderProductVo.setOrderItemVoList(orderItemVoList);
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.ip"));
        return ServerResponse.createBySuccess(orderProductVo);
    }
    //用户个人中心的订单详情
    public ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo) {
        Order order = orderMapper.selectOrderByUserIdAndOrderNo(userId, orderNo);
        if (order != null) {
            List<OrderItem> orderItemList = orderItemMapper.getOrderItemByOrderNoAndUserId(order.getOrderNo(), userId);
            OrderVo orderVo = assembleOrderVo(order, orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("没有找到该订单");
    }
    //个人中心订单列表
    public ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Order> orderList = orderMapper.selectByUserId(userId);
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList, userId);
        PageInfo pageInfo = new PageInfo(orderList);
        pageInfo.setList(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    private List<OrderVo> assembleOrderVoList(List<Order> orderList, Integer userId) {
        List<OrderVo> orderVoList = Lists.newArrayList();
        for (Order order : orderList) {
            List<OrderItem> orderItemList = Lists.newArrayList();
            if (userId != null) {
                orderItemList = orderItemMapper.getOrderItemByOrderNoAndUserId(order.getOrderNo(), userId);
            } else {
                //todo 管理员查询的时候不需要传入userId
                orderItemList = orderItemMapper.selectByOrderNo(order.getOrderNo());
            }
            OrderVo orderVo = assembleOrderVo(order, orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }

    //后台-------------------------------------------------------------------------------------------------
    public ServerResponse<PageInfo> manageList(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Order> orderList = orderMapper.selectAllOrder();
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList, null);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }
    public ServerResponse<OrderVo> manageDetail(Long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("找不到该订单");
        }
        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNo(order.getOrderNo());
        OrderVo orderVo = assembleOrderVo(order, orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }
    public ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("找不到该订单");
        }
        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNo(order.getOrderNo());
        OrderVo orderVo = assembleOrderVo(order, orderItemList);

        PageInfo pageResult = new PageInfo(Lists.newArrayList(order));
        pageResult.setList(Lists.newArrayList(orderVo));
        return ServerResponse.createBySuccess(pageResult);
    }

   
    public ServerResponse<String> manageSendGoods(Long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null) {
            if (order.getStatus() == Const.OrderStatusEnum.PAID.getCode()) {
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
                order.setSendTime(new Date());
                orderMapper.updateByPrimaryKeySelective(order);
                return ServerResponse.createBySuccess("发货成功");
            }
        }
        return ServerResponse.createByErrorMessage("该订单不存在或未付款");
    }










}
