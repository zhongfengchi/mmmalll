package com.mmall.service.Impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Cart;
import com.mmall.pojo.Product;
import com.mmall.service.ICartService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.CartProductVo;
import com.mmall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by 钟奉池 on 2018/6/23.
 */
@Service("iCartService")
public class CartServiceImpl implements ICartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    //添加到购物车
    public ServerResponse<CartVo> add(Integer userId,Integer productId,Integer count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCartByUserIdAndProductId(userId,productId);
        Product product = productMapper.selectByPrimaryKey(productId);
        if(cart == null){//此时该用户对应的该产品不再购物车中
            if(product != null && product.getStatus().intValue() == 1){
                Cart cartItem = new Cart();
                if(product.getStock() >= count){
                    cartItem.setQuantity(count);
                }else {
                    cartItem.setQuantity(product.getStock());
                }
                cartItem.setChecked(Const.Cart.CHECKED);//加入购物车的产品默认为选中状态
                cartItem.setProductId(productId);
                cartItem.setUserId(userId);
                cartMapper.insert(cartItem);
            }else {// 如果该商品不存在
                return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),
                        "当前商品不存在或已下架");
            }
        }else {
            if(product != null && product.getStatus().intValue() == 1){
                if(product.getStock() >= cart.getQuantity()+count){
                    count = cart.getQuantity() + count;
                    cart.setQuantity(count);
                }else {
                    cart.setQuantity(product.getStock());
                }
                cartMapper.updateByPrimaryKeySelective(cart);
            }else {
                return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),
                        "当前商品不存在或已下架");
            }
        }
        return this.list(userId);
    }

    //更新购物车(产品数量)
    public ServerResponse<CartVo> update(Integer userId,Integer productId,Integer count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //查询出购物车中特定产品
        Cart cart = cartMapper.selectCartByUserIdAndProductId(userId,productId);
        if(cart != null){
            cart.setQuantity(count);//更新该产品的数量
        }
        cartMapper.updateByPrimaryKeySelective(cart);//更新该产品的数量到数据库
        return this.list(userId);
    }
    //删除购物车中的产品
    public ServerResponse<CartVo> deleteProduct(Integer userId,String productIds){
        //guava提供的分割字符串并转换成集合的方法
        List<String> productList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productList)){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        cartMapper.deleteByUserIdAndProductIds(userId,productList);
        return this.list(userId);
    }
    //查询购物车产品列表
    public ServerResponse<CartVo> list(Integer userId){
        CartVo cartVo = this.getCartVoLimit(userId);
        return ServerResponse.createBySuccess(cartVo);
    }
    //全选/全不选/单独选/单独反选
    public ServerResponse<CartVo> selectOrUnselect(Integer userId,Integer checked,Integer productId){
        cartMapper.checkedOrUncheckedProduct(userId,checked,productId);
        return this.list(userId);
    }
    //查询当前用户购物车中的各种产品数量的和（按产品数量计算）
    public ServerResponse<Integer> getCartProductCount(Integer userId){
        if(userId == null){
            return ServerResponse.createBySuccess(0);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }
    //组装CartVo对象
    public CartVo getCartVoLimit(Integer userId){
        CartVo cartVo = new CartVo();
        List<Cart> cartList = cartMapper.selectCartByUserId(userId);
        List<CartProductVo> cartProductVoList = Lists.newArrayList();
        BigDecimal cartTotalPrice = new BigDecimal("0");
        if(CollectionUtils.isNotEmpty(cartList)){//该用户购物车中有产品
            for(Cart cart :cartList){//遍历购物车中的产品
                CartProductVo cartProductVo = new CartProductVo();
                cartProductVo.setId(cart.getId());
                cartProductVo.setUserId(userId);
                cartProductVo.setProductId(cart.getProductId());
                //根据遍历到的当前产品的id查找产品信息
                Product product = productMapper.selectByPrimaryKey(cart.getProductId());
                if(product != null){//如果找到了对应的产品就把产品的一些字段赋予cartProductVo对象
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    cartProductVo.setProductStatus(product.getStatus());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStock(product.getStock());
                    int buyLimitCount;//初始化实际能购买到的数量
                    if(product.getStock() >= cart.getQuantity()){//如果该产品的库存大于等于购买的数量
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                        buyLimitCount = cart.getQuantity();
                    }else {
                        buyLimitCount = product.getStock();//将购买数量限制为该产品库存
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        Cart cartForQuantity = new Cart();
                        cartForQuantity.setId(cart.getId());
                        cartForQuantity.setQuantity(buyLimitCount);
                        //更新该条购物车记录（购物车中的产品），将该产品在购物车中的数量设置为该产品的库存
                        cartMapper.updateByPrimaryKeySelective(cartForQuantity);
                    }
                    cartProductVo.setQuantity(buyLimitCount);
                    //计算该产品的总价（单价*购物车中该产品的数量）
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(cartProductVo.getProductPrice().doubleValue(),cartProductVo.getQuantity().doubleValue()));
                    cartProductVo.setProductChecked(cart.getChecked());
                }
                if(cart.getChecked() == Const.Cart.CHECKED){//如果该产品被选中
                    //将当前产品的总价纳入购物车总价计算中
                    cartTotalPrice = BigDecimalUtil.add0(cartTotalPrice.doubleValue(),cartProductVo.getProductTotalPrice().doubleValue());
                }
                cartProductVoList.add(cartProductVo);//当前cartProductVo组装完成
            }
        }
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        cartVo.setAllChecked(this.getAllCheckedStatus(userId));
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.ip"));
        return cartVo;
    }
    //判断用户购物车中的产品是否全选中
    public boolean getAllCheckedStatus(Integer userId){
        if(userId == null){
            return false;
        }
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;
    }

}
