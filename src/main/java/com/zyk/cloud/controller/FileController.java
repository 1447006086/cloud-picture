package com.zyk.cloud.controller;

import cn.hutool.core.io.IoUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.zyk.cloud.annotation.AuthCheck;
import com.zyk.cloud.common.BaseResponse;
import com.zyk.cloud.common.ResultUtils;
import com.zyk.cloud.exception.BusinessException;
import com.zyk.cloud.exception.ErrorCode;
import com.zyk.cloud.manager.CosManager;
import com.zyk.cloud.model.constant.UserConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    // 测试上传文件
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestParam("file") MultipartFile multipartFile){
        String fileName=multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);
        File file=null;
        try {
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filePath,file);
            //返回可访问地址
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error(filePath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            if (file!=null){
                boolean delete = file.delete();
                if (!delete){
                    log.error("删除临时文件失败");
                }
            }
        }
    }


    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void testDownload(String filePath, HttpServletResponse response) throws IOException {
        COSObjectInputStream objectContent=null;
        try {
            COSObject object = cosManager.getObject(filePath);
            objectContent = object.getObjectContent();
            byte[] byteArray = IOUtils.toByteArray(objectContent);
            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filePath );
            //写入响应
            response.getOutputStream().write(byteArray);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error(filePath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"下载失败");
        }finally {
            if (objectContent!=null){
                objectContent.close();
            }
        }

    }
}
