package com.huanle.service;

import com.alibaba.fastjson.JSONObject;
import com.huanle.Util.CommonUtil;
import com.huanle.Util.FileUtil;
import com.huanle.dao.*;
import com.huanle.entity.CollectionEntity;
import com.huanle.entity.ProductInfo;
import com.huanle.entity.UserInfo;
import com.huanle.vo.ResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ProductInfoService {

    private  static final Logger logger = LoggerFactory.getLogger(ProductInfoService.class);

    @Autowired
    ProductInfoMapper productInfoMapper;

    @Autowired
    UserInfoMapper userInfoMapper;

    @Autowired
    CollectionMapper collectionMapper;

    @Autowired
    OrdersMapper ordersMapper;

    /**
     *  添加商品
     * @param productInfo
     * @param files
     * @return
     */
    public Boolean addProductInfo(ProductInfo productInfo, MultipartFile[] files){
        String picture = FileUtil.uploadFile(files);
        if(picture == null){
            return false;
        }
        productInfo.setPicture(picture);

        if(productInfoMapper.insertSelective(productInfo) == 1){
            return true;
        }
        return false;
    }

    /**
     * 获取审核通过且已上架的商品
     * @return
     */
    public Map getProductList(){
        Map<String,Object> result = new HashMap();

        List<ProductInfo> productInfos = productInfoMapper.getList();     //获取商品信息
        result.put("total",productInfos.size());
        List<Map> data = formatProductList(productInfos);
//        for (ProductInfo productInfo : productInfos){
//            Map<String,Object> map = new HashMap<>();
//
//            String[] picture = CommonUtil.pictureToArr(productInfo.getPicture());
//            map.put("product",productInfo);
//            map.put("picture",picture);
//            data.add(map);
//        }
        result.put("productList",data);
        return result;
    }

    /**
     * 获取指定商品信息
     * @param pid
     * @return
     */
    public Map getProduct(Integer pid, Integer upId){
        Map data = new HashMap();
        ProductInfo productInfo = productInfoMapper.selectByPrimaryKey(pid);
        if(productInfo == null){
            data.put("message","无此商品");
            return null;

        }else {
            Integer uid = productInfo.getpUid();
            UserInfo userInfor = userInfoMapper.selectByPrimaryKey(uid);
            if(userInfor == null){
               // data.put("message","商品信息有误");
                return null;
            }else {
                //获取此商品收藏信息
                Integer ccId = 0;
                if(upId != null) {
                    CollectionEntity collectionEntity = collectionMapper.getCollectionByPidAndUid(pid,upId);
                    if(collectionEntity != null){
                        ccId = 1;
                    }
                }

                data.put("productInfo",productInfo);
                String[] picture = CommonUtil.pictureToArr(productInfo.getPicture());
                data.put("picture",picture);
                data.put("collection",ccId);
                data.put("uid",userInfor.getUid());
                data.put("nickName",userInfor.getNickName());
                data.put("email",userInfor.getEmail());
                data.put("message","查找成功");
            }
        }
        return data;
    }

    public Map getProductInfoByPid(Integer pid){
        Map<String,Object> result = new HashMap();
        ProductInfo productInfo =  productInfoMapper.selectByPrimaryKey(pid);
        String[] picture = CommonUtil.pictureToArr(productInfo.getPicture());
        result.put("productInfo",productInfo);
        result.put("picture",picture);
        return result;
    }

    /**
     * 删除指定商品信息
     * @param pid
     * @return
     */
    public Boolean deleteProduct(Integer pid){
        if(productInfoMapper.deleteByPrimaryKey(pid) != 0){
            return true;
        }else {
            return false;
        }
    }

    /**
     * 编辑商品信息
     * @param productInfo
     * @param files
     * @return
     */
    public Boolean updateProduct(ProductInfo productInfo, MultipartFile[] files){
        String picture = FileUtil.uploadFile(files);
        if(picture == null){
            return false;
        }
        productInfo.setPicture(picture);

        if(productInfo.getPid() != null){
            if(productInfoMapper.updateByPrimaryKeySelective(productInfo) != 0){
                return true;
            }
        }
        return false;

    }

    /**
     * 根据商品名称搜索
     * @param inputStr
     * @return
     */
    public Map getProductListByTitle(String inputStr){
        Map<String,Object> result = new HashMap();

        Set<String> inputs = new HashSet<>();
        inputStr = "%"+inputStr+"%";
        List<ProductInfo> productInfos = productInfoMapper.queryByTitle(inputStr);

        List<Map> data = new ArrayList<>();
        if(productInfos != null){
            for (ProductInfo productInfo:productInfos){
                Map<String,Object> map = new HashMap<>();
                //input框
                inputs.add(productInfo.getTitle());

                String[] picture = CommonUtil.pictureToArr(productInfo.getPicture());
                map.put("product",productInfo);
                map.put("picture",picture);
                data.add(map);

            }
            result.put("inputs",inputs);
            result.put("productList",data);
        }

        return result;
    }

    public Map getProductListType(String type){
        Map<String,Object> result = new HashMap();
        List<ProductInfo> productInfos = productInfoMapper.queryByType(type);
        List<Map> data = new ArrayList<>();
        if(productInfos != null) {
            for (ProductInfo productInfo : productInfos) {
                Map<String, Object> map = new HashMap<>();

                String[] picture = CommonUtil.pictureToArr(productInfo.getPicture());
                map.put("picture", picture);
                map.put("product", productInfo);
                data.add(map);

            }
        }
        return result;

    }

    public Map getStatisticsRecord(Integer uid){
        Map<String,Object> result = new HashMap<>();
        Integer totalProd = productInfoMapper.productCountByPuid(uid);
        Integer totalOrder = ordersMapper.getCountByUid(uid);
        Integer totalOfMy = ordersMapper.getCountByAUid(uid);
        Integer totalOfOther = ordersMapper.getCountByBUid(uid);

        if(totalProd == null || totalProd.equals(0)){
            return null;
        }else{
            result.put("totalProd",totalProd);
            result.put("totalOrder",totalOrder);
            result.put("totalOfMy",totalOfMy);
            result.put("totalOfOther",totalOfOther);
        }

        return result;
    }

    /**
     * admin
     * @return
     */
    public Map getAllProductList(JSONObject param){

        Map result = new HashMap();
        List<ProductInfo> productInfoList = productInfoMapper.getAllList(param);
        if(productInfoList == null){
            return null;
        }else{
            List<Map> data = formatProductList(productInfoList);
            result.put("productList",data);
            result.put("total",productInfoList.size());
            return result;
        }
    }

    private List<Map> formatProductList(List<ProductInfo> productInfoList){
        List<Map> data = new ArrayList<>();
        for (ProductInfo productInfo : productInfoList){
            Map<String,Object> map = new HashMap<>();

            String[] picture = CommonUtil.pictureToArr(productInfo.getPicture());
            map.put("product",productInfo);
            map.put("picture",picture);
            data.add(map);
        }
        return data;
    }

}
