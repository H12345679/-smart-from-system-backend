package com.edu.smartfarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.smartfarm.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT r.role_code FROM t_role r INNER JOIN t_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    String findRoleByUserId(Long userId);
}
