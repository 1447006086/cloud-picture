package com.zyk.cloud.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zyk.cloud.model.dto.user.UserQueryRequest;
import com.zyk.cloud.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zyk.cloud.model.vo.LoginUserVO;
import com.zyk.cloud.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author mechrevo
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-07-02 15:53:04
*/
public interface UserService extends IService<User> {
    long userRegister(String username, String password,String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request httpRequest 请求方便设置 cookie
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);



    String getEncryptPassword(String userPassword);

    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     *
     * @param request request
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request request
     * @return  注销结果
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取用户脱敏信息
     * @param user 脱敏前的信息
     * @return 脱敏后的信息
     */
    UserVO getUserVO(User user);

    /**
     * 批量获取用户脱敏信息
     * @param userList 脱敏前的信息
     * @return 脱敏后的 List 列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest 查询条件
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    boolean isAdmin(User user);

}
