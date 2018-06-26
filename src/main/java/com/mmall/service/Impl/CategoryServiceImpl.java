package com.mmall.service.Impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.pojo.Category;
import com.mmall.service.ICategoryService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Created by 钟奉池 on 2018/6/21.
 */
@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService{
    private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);
    @Autowired
    private CategoryMapper categoryMapper;
    //添加分类
    public ServerResponse addCategory(String categoryName,Integer parentId){
        if(parentId == null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("添加分类参数错误");
        }
        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);
        category.setStatus(true);
        int count = categoryMapper.insert(category);
        if(count > 0){
            return ServerResponse.createBySuccess("添加分类成功");
        }
        return ServerResponse.createByErrorMessage("添加分类失败");
    }
    //更新分类名称
    public ServerResponse setCategoryName(Integer categoryId,String categoryName){
        if(categoryId == null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("更新分类名称参数错误");
        }
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        int count =categoryMapper.updateByPrimaryKeySelective(category);
        if(count > 0){
            return ServerResponse.createBySuccess("更新分类名称成功");
        }
        return  ServerResponse.createByErrorMessage("更新分类名称失败");
    }
    //查询节点的第一级子分类
    public ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId){
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        if(CollectionUtils.isEmpty(categoryList)){
            logger.info("未找到当前分类的子分类");
        }
        return ServerResponse.createBySuccess(categoryList);
    }
    //查询节点的子孙分类
    public ServerResponse<List<Integer>> getCategoryAndDeepChildrenCategory(Integer categoryId){
        Set<Category> categorySet = Sets.newHashSet();//guava提供的初始化集合方法
        findCategoryChildren(categorySet,categoryId);//调用递归算法获取到子孙分类集合
        List<Integer> categoryIdList = Lists.newArrayList();//初始化一个List集合以存储子孙分类的id
        if(categoryId != null){
            for(Category categoryItem : categorySet){
                categoryIdList.add(categoryItem.getId());
            }
        }
        return ServerResponse.createBySuccess(categoryIdList);

    }
    //递归查询分类算法
    private Set<Category> findCategoryChildren(Set<Category> categorySet,Integer categoryId){
        //4、将查询到的每一个子分类
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category != null){
            categorySet.add(category);
        }
        //mybatis查询返回集合，即便没有查找到，也不会返回空，所以无需对categoryList做空判断
        //1、利用传入的categoryId查询下一级子分类
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        //2、遍历查询到的子分类集合
        for(Category categoryItem : categoryList){
            categorySet.add(categoryItem);
            //3、利用子分类的categoryId再次调用自身方法
            findCategoryChildren(categorySet,categoryItem.getId());
        }
        return categorySet;
    }
}
