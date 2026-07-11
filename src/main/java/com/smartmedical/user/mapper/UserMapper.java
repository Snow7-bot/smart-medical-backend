package com.smartmedical.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmedical.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
