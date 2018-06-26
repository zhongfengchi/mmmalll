package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by 钟奉池 on 2018/6/22.
 */
public interface IFileService {
    String upload(MultipartFile multipartFile, String path);
}
