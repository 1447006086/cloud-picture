package com.zyk.cloud.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zyk.cloud.config.CosClientConfig;
import com.zyk.cloud.exception.BusinessException;
import com.zyk.cloud.exception.ErrorCode;
import com.zyk.cloud.manager.CosManager;
import com.zyk.cloud.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;

@Component
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    CosManager cosManager;

    public UploadPictureResult uploadPicture(Object inputSource,String uploadPrefix) {
        //校验图片
        validPicture(inputSource);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPrefix, uploadFileName);
        File file=null;
        try {
            file = File.createTempFile(uploadPath, null);
//            multipartFile.transferTo(file);
            //处理文件来源
            precessFile(inputSource,file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            return buildResult(originalFilename, file,uploadPath,imageInfo);
        } catch (Exception e) {
            log.error(uploadPath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            //临时文件清理
            deleteTempFile(file);
        }
        //解析结果并返回

    }

    private UploadPictureResult buildResult(String originalFilename, File file,String uploadPath, ImageInfo imageInfo) {
        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        double picScale = NumberUtil.round(width*1.0/height,2).doubleValue();
        //封装返回结果
        UploadPictureResult uploadPictureResult=new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+ uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(imageInfo.getWidth());
        uploadPictureResult.setPicHeight(imageInfo.getHeight());
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        //返回可访问地址
        return uploadPictureResult;
    }

    protected abstract void precessFile(Object inputSource, File file) throws IOException;

    protected abstract String getOriginalFilename(Object inputSource);

    protected abstract void validPicture(Object inputSource);
    private static void deleteTempFile(File file) {
        if (file !=null){
            boolean delete = file.delete();
            if (!delete){
                log.error("删除临时文件失败");
            }
        }
    }




}
