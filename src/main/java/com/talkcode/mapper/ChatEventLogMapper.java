package com.talkcode.mapper;

import com.mybatisflex.core.BaseMapper;
import com.talkcode.model.entity.ChatEventLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天事件日志 Mapper。
 */
@Mapper
public interface ChatEventLogMapper extends BaseMapper<ChatEventLog> {
}
