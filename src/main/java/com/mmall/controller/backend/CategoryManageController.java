package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.ICategoryService;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Created by 钟奉池 on 2018/6/21.
 */
@Controller
@RequestMapping("/manage/category")
public class CategoryManageController {
    @Autowired
    private IUserService iUserService;
    @Autowired
    private ICategoryService iCategoryService;

    //添加分类
    @RequestMapping("add_category.do")
    @ResponseBody
    public ServerResponse addCategory(HttpSession session,String categoryName,@RequestParam(value="parentId",defaultValue = "0") int parentId){
        //校验登录状态
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        //校验用户角色
        if(iUserService.checkAdminRole(user).isSuccess()){//是管理员
            //分类处理逻辑
            return iCategoryService.addCategory(categoryName, parentId);
        }else {
            return ServerResponse.createByErrorMessage("无管理员权限");
        }
    }
    //设置分类名称
    @RequestMapping("set_category_name.do")
    @ResponseBody
    public ServerResponse setCategoryName(HttpSession session,Integer categoryId,String categoryName){
        //校验登录状态
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        //校验用户角色
        if(iUserService.checkAdminRole(user).isSuccess()){//是管理员
            //更新分类名称
            return iCategoryService.setCategoryName(categoryId,categoryName);
        }else {
            return ServerResponse.createByErrorMessage("无管理员权限");
        }
    }
    //查询节点的第一级子分类
    @RequestMapping("get_category.do")
    @ResponseBody
    public ServerResponse getChildrenParallelCategory(HttpSession session,@RequestParam(value="categoryId",defaultValue = "0") Integer categoryId){
        //校验登录状态
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        //校验用户角色
        if(iUserService.checkAdminRole(user).isSuccess()){//是管理员
            return iCategoryService.getChildrenParallelCategory(categoryId);
        }else {
            return ServerResponse.createByErrorMessage("无管理员权限");
        }
    }
    //查询节点的子孙分类
    @RequestMapping("get_deep_category.do")
    @ResponseBody
    public ServerResponse getCategoryAndDeepChildrenCategory(HttpSession session,@RequestParam(value="categoryId",defaultValue = "0") Integer categoryId){
        //校验登录状态
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        //校验用户角色
        if(iUserService.checkAdminRole(user).isSuccess()){//是管理员
            return iCategoryService.getCategoryAndDeepChildrenCategory(categoryId);
        }else {
            return ServerResponse.createByErrorMessage("无管理员权限");
        }
    }
}
