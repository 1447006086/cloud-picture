package com.zyk.cloud.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyk.cloud.model.dto.PictureReviewRequest;
import com.zyk.cloud.model.dto.picture.PictureQueryRequest;
import com.zyk.cloud.model.dto.picture.PictureUploadRequest;
import com.zyk.cloud.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zyk.cloud.model.entity.User;
import com.zyk.cloud.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author mechrevo
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-07-03 00:50:36
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile
     * @param uploadPictureResult
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);

    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);


    void  fillReviewParams(Picture picture, User loginUser);
}
