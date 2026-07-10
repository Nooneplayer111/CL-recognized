package com.elephantjava.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elephantjava.dto.RecognitionRecordSaveDTO;
import com.elephantjava.entity.RecognitionRecord;
import com.elephantjava.vo.RecognitionRecordPageResult;
import com.elephantjava.vo.RecognitionRecordVO;

/**
 * 识别记录业务接口
 */
public interface IRecognitionRecordService extends IService<RecognitionRecord> {

    /**
     * 分页查询识别记录
     */
    RecognitionRecordPageResult pageQuery(Integer page, Integer size, String keyword, String mediaType);

    /**
     * 根据ID查询详情
     */
    RecognitionRecordVO getDetail(Long id);

    /**
     * 保存识别记录（小程序/Web 调用）
     */
    RecognitionRecordVO saveRecord(RecognitionRecordSaveDTO saveDTO);

    /**
     * 删除识别记录
     */
    void deleteRecord(Long id);
}
