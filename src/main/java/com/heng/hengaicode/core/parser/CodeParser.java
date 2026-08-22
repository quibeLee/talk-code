package com.heng.hengaicode.core.parser;

/**
 * 代码解析接口,子类需要实现具体的解析方法
 * @param <T> 解析后的对象类型
 */
public interface CodeParser<T> {


    /**
     * 代码解析方法,将代码内容解析为指定类型的对象
     * @param codeContent 代码内容
     * @return 解析后的对象
     * @throws Exception 解析异常
     */
    T parseCode(String codeContent) throws Exception;
}
