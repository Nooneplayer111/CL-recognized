package com.elephantjava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elephantjava.entity.Camera;
import org.apache.ibatis.annotations.Mapper;

/**
 * 摄像头数据访问层
 */
@Mapper
public interface CameraMapper extends BaseMapper<Camera> {
}
