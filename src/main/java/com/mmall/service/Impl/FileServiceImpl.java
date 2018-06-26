package com.mmall.service.Impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by 钟奉池 on 2018/6/22.
 */
@Service("iFileService")
public class FileServiceImpl implements IFileService {
    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
    public String upload(MultipartFile multipartFile,String path){
        String fileName = multipartFile.getOriginalFilename();//获取文件的原始名称
        String fileExtensionName = fileName.substring(fileName.lastIndexOf(".") + 1);//获取文件扩展名
        //文件上传到服务器后的名称
        String uploadFileName = UUID.randomUUID().toString() + "." + fileExtensionName;
        logger.info("开始上传文件，上传文件的文件名为{},上传路径为{},新文件名为{}",fileName,path,uploadFileName);
        File fileDir = new File(path);//创建文件对象
        if(!fileDir.exists()){
            fileDir.setWritable(true);//设置文件的可写权限
            fileDir.mkdirs();
        }
        File targetFile = new File(path,uploadFileName);
        try {
            multipartFile.transferTo(targetFile);//上传文件到tomcat服务器
            // 上传文件到ftp服务器
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));
            //删除upload下的文件
            targetFile.delete();
        } catch (IOException e) {
            logger.error("上传文件异常",e);
            return null;
        }
        return targetFile.getName();
    }
}
