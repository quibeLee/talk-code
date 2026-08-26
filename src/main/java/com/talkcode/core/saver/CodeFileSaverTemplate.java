package com.talkcode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.talkcode.constant.AppConstant;
import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 代码文件保存器模板类,子类需要实现具体的保存逻辑
 *
 * @param <T> 代码结果类型
 * @author heng
 */
@Slf4j
public abstract class CodeFileSaverTemplate<T> {
    /**
     * 保存代码文件的根路径
     */
    protected static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;


    /**
     * 模板方法：保存代码的标准流程
     *
     * @param result 代码结果
     * @param appId  应用ID
     * @return 保存的文件对象
     */
    public File saveCode(T result, long appId) {
        // 1.校验输入参数
        validate(result);
        // 2.构建文件唯一路径
        String baseDirPath = buildUniqueDirPath(appId);
        // 3.执行保存文件
        saveFiles(result, baseDirPath);
        // 4.返回文件对象
        return new File(baseDirPath);
    }

    /**
     * 校验输入参数
     *
     * @param result 代码结果
     */
    protected void validate(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果不能为空");
        }
    }

    /**
     * 构建文件唯一路径
     *
     * @param appId 应用ID
     * @return 文件唯一路径
     */
    protected final String buildUniqueDirPath(long appId) {
        if (appId <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用ID不合法");
        }
        String uniqueDirFiilname = StrUtil.format("{}_{}", getCodeGenType().getValue(), appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirFiilname;
        //根据路径名创建文件目录
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入单个文件
     *
     * @param dirPath  目录路径
     * @param fileName 文件名
     * @param content  文件内容
     */
    protected final void writeToFile(String dirPath, String fileName, String content) {
        if (StrUtil.isNotBlank(fileName)) {
            String filePath = dirPath + File.separator + fileName;
            FileUtil.writeString(content, filePath, CharsetUtil.CHARSET_UTF_8);
            log.info("写入文件 : {} 成功", filePath);
        }
    }

    /**
     * 获取代码生成类型,子类需要实现该方法
     *
     * @return 代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeGenType();

    /**
     * 保存文件的具体实现,子类需要实现该方法
     *
     * @param result      代码结果
     * @param baseDirPath 基础目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
