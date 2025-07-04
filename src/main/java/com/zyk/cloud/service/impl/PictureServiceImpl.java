package com.zyk.cloud.service.impl;

import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyk.cloud.exception.BusinessException;
import com.zyk.cloud.exception.ErrorCode;
import com.zyk.cloud.exception.ThrowUtils;
import com.zyk.cloud.manager.FileManager;
import com.zyk.cloud.manager.upload.FilePictureUpload;
import com.zyk.cloud.manager.upload.PictureUploadTemplate;
import com.zyk.cloud.manager.upload.UrlPictureUpload;
import com.zyk.cloud.model.dto.PictureReviewRequest;
import com.zyk.cloud.model.dto.file.UploadPictureResult;
import com.zyk.cloud.model.dto.picture.PictureQueryRequest;
import com.zyk.cloud.model.dto.picture.PictureUploadRequest;
import com.zyk.cloud.model.entity.Picture;
import com.zyk.cloud.model.entity.User;
import com.zyk.cloud.model.enums.PictureReviewStatusEnum;
import com.zyk.cloud.model.vo.PictureVO;
import com.zyk.cloud.model.vo.UserVO;
import com.zyk.cloud.service.PictureService;
import com.zyk.cloud.mapper.PictureMapper;
import com.zyk.cloud.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author mechrevo
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-07-03 00:50:36
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //判断新增还算删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新,判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture!=null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            //仅管理员或本人可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        //上传图片
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        //根据inputSource获取文件
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //构建picture
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //补充审核参数
        this.fillReviewParams(picture, loginUser);
        //操作数据库
        //如果pictureId不为空，则更新
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "上传图片失败");
        return PictureVO.objToVo(picture);
    }




    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }



    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {


        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        if (StrUtil.isNotBlank(searchText)){
            queryWrapper.and(qw->qw.like("name", searchText)
                    .or().like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty( id), "id", id);
        queryWrapper.like(ObjUtil.isNotEmpty( name), "name", name);
        queryWrapper.like(ObjUtil.isNotEmpty( introduction), "introduction", introduction);
        queryWrapper.like(ObjUtil.isNotEmpty( category), "category", category);
        queryWrapper.like(ObjUtil.isNotEmpty( pictureQueryRequest.getReviewMessage()), "reviewMessage", pictureQueryRequest.getReviewMessage());
        queryWrapper.eq(ObjUtil.isNotEmpty( picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty( picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty( picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty( picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty( picFormat), "picFormat", picFormat);
        queryWrapper.eq(ObjUtil.isNotEmpty( userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty( pictureQueryRequest.getReviewStatus()), "reviewStatus", pictureQueryRequest.getReviewStatus());
        queryWrapper.eq(ObjUtil.isNotEmpty( pictureQueryRequest.getReviewerId()), "reviewerId", pictureQueryRequest.getReviewerId());
        queryWrapper.eq(ObjUtil.isNotEmpty( userId), "userId", userId);
        if (CollUtil.isNotEmpty(tags)){
            for (String tag : tags){
                queryWrapper.like("tags","\""+ tag+"\"");
            }
        }
        queryWrapper.orderBy(StrUtil.isNotBlank(    sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null || loginUser == null, new BusinessException(ErrorCode.PARAMS_ERROR));
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id==null || reviewStatus==null || PictureReviewStatusEnum.REVIEWING.equals(reviewEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        //校验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片已审核");
        }
        //数据库操作
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setUpdateTime(new Date());
        boolean result= this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "审核失败");
//        Picture oldPicture = pictureDAO.getByPictureId(id);
//        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "图片已审核");
//        ThrowUtils.throwIf(ObjectUtils.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
//        Picture updatePicture = buildUpdateReviewPicture(loginUser, oldPicture, reviewStatus, reviewMessage);
//        ThrowUtils.throwIf(!pictureDAO.updateById(updatePicture), ErrorCode.SYSTEM_ERROR, "审核失败");
    }

    private Picture buildUpdateReviewPicture(User loginUser, Picture oldPicture, Integer reviewStatus, String reviewMessage) {
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(oldPicture, updatePicture);
        updatePicture.setReviewStatus(reviewStatus);
        updatePicture.setReviewMessage(reviewMessage);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        return updatePicture;
    }

    @Override
    public void  fillReviewParams(Picture picture, User loginUser){
        if (userService.isAdmin(loginUser)){
            //管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
            this.updateById(picture);
        }else {
            //非管理员默认待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }



}




