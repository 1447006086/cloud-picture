package com.zyk.cloud.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.zyk.cloud.exception.ErrorCode;
import com.zyk.cloud.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class FilePictureUpload extends PictureUploadTemplate {


    @Override
    protected void precessFile(Object inputSource, File file) throws IOException {
        MultipartFile multipartFile= (MultipartFile) inputSource;
        multipartFile.transferTo( file);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile= (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile= (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile==null, ErrorCode.PARAMS_ERROR, "图片为空");
        //校验文件大小
        ThrowUtils.throwIf(multipartFile.getSize()>1024*1024*2, ErrorCode.PARAMS_ERROR, "图片大小不能超过2M");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!"png".equals(suffix)&&!"jpg".equals(suffix)&&!"jpeg".equals(suffix), ErrorCode.PARAMS_ERROR, "图片格式错误");
    }
}
