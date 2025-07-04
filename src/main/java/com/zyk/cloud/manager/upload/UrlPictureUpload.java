package com.zyk.cloud.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.zyk.cloud.exception.BusinessException;
import com.zyk.cloud.exception.ErrorCode;
import com.zyk.cloud.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {


    @Override
    protected void precessFile(Object inputSource, File file) throws IOException {
        String fileUrl= (String) inputSource;
        HttpUtil.downloadFile(fileUrl,file);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl= (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl= (String) inputSource;
        //校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "图片为空");
        //校验url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        //校验url协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://")&&!fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "图片格式错误");
        //发送head请求验证文件是否存在
        HttpResponse httpResponse=null;

        try {
            httpResponse= HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (!httpResponse.isOk()){
                return;
            }
            //文件类型校验
            String header = httpResponse.header("Content-Type");
            if (StrUtil.isNotBlank( header)){
                final List<String> ALLOW_CONTENT_TYPE= Arrays.asList("image/jpeg", "image/png", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPE.contains(header), ErrorCode.PARAMS_ERROR, "图片格式错误");
            }
            //文件大小校验
            String contentLength = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength)){
                try {
                    long parsed = Long.parseLong(contentLength);
                    final long ONE_M=1024*1024;
                    ThrowUtils.throwIf(parsed>2*ONE_M, ErrorCode.PARAMS_ERROR, "图片大小不能超过2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小错误");
                }
            }
        } finally {
            if (httpResponse!=null){
                httpResponse.close();
            }
        }
    }
}
