package com.zyk.cloud.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zyk.cloud.config.CosClientConfig;
import com.zyk.cloud.exception.BusinessException;
import com.zyk.cloud.exception.ErrorCode;
import com.zyk.cloud.exception.ThrowUtils;
import com.zyk.cloud.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
@Deprecated
public class FileManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    CosManager cosManager;

    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPrefix) {
        //校验图片
        validPicture(multipartFile);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPrefix, uploadFileName);
        File file=null;
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            String format = imageInfo.getFormat();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            int quality = imageInfo.getQuality();
            String ave = imageInfo.getAve();
            int orientation = imageInfo.getOrientation();
            int frameCount = imageInfo.getFrameCount();
            double picScale = NumberUtil.round(width*1.0/height,2).doubleValue();
                        //封装返回结果
            UploadPictureResult uploadPictureResult=new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(imageInfo.getWidth());
            uploadPictureResult.setPicHeight(imageInfo.getHeight());
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            //返回可访问地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error(uploadPath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            //临时文件清理
            deleteTempFile(file);
        }
        //解析结果并返回

    }

    private static void deleteTempFile(File file) {
        if (file !=null){
            boolean delete = file.delete();
            if (!delete){
                log.error("删除临时文件失败");
            }
        }
    }

    /**
     * 校验图片
     */
    public void validPicture(MultipartFile multipartFile){
        ThrowUtils.throwIf(multipartFile==null, ErrorCode.PARAMS_ERROR, "图片为空");
        //校验文件大小
        ThrowUtils.throwIf(multipartFile.getSize()>1024*1024*2, ErrorCode.PARAMS_ERROR, "图片大小不能超过2M");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!"png".equals(suffix)&&!"jpg".equals(suffix)&&!"jpeg".equals(suffix), ErrorCode.PARAMS_ERROR, "图片格式错误");
    }


    public UploadPictureResult uploadPictureByUrl(String fileUrl,String uploadPrefix) {
        //校验图片
//        validPicture(multipartFile);
        validPicture(fileUrl);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
//        String originalFilename = multipartFile.getOriginalFilename();
        String originalFilename = FileUtil.mainName(fileUrl);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPrefix, uploadFileName);
        File file=null;
        try {
            file = File.createTempFile(uploadPath, null);
            HttpUtil.downloadFile(fileUrl,file);
//            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            double picScale = NumberUtil.round(width*1.0/height,2).doubleValue();
            //封装返回结果
            UploadPictureResult uploadPictureResult=new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(imageInfo.getWidth());
            uploadPictureResult.setPicHeight(imageInfo.getHeight());
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            //返回可访问地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error(uploadPath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            //临时文件清理
            deleteTempFile(file);
        }
        //解析结果并返回

    }

    private void validPicture(String fileUrl) {
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
            httpResponse=HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
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
