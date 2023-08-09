package com.sky.controller.admin;


import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    @Resource
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传");

        //原始文件名
        String originalFilename = file.getOriginalFilename();

        //截取原始文件名的后缀
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        String newFileName = UUID.randomUUID().toString() + extension;
        try {
            //文件的请求路径
            String filepath = aliOssUtil.upload(file.getBytes(), newFileName);
            return Result.success(filepath);
        } catch (IOException e) {
            log.info("文件上传失败 , {}", e);
        }


        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
