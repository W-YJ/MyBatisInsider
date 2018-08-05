/**
 * Copyright 2009-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 * @author Clinton Begin
 */
// 通用的字占位符解析器
public class GenericTokenParser {

    private final String openToken;//占位符的开始标记
    private final String closeToken;//占位符的结束标记
    //TokenHandler接口的实现会按照一定的逻辑解析占位符
    private final TokenHandler handler;

    public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
        this.openToken = openToken;
        this.closeToken = closeToken;
        this.handler = handler;
    }

    public String parse(String text) {
        //检测text是否为空
        if (text == null || text.isEmpty()) {
            return "";
        }
        //search open token
        int start = text.indexOf(openToken, 0);
        if (start == -1) {
            return text;
        }
        char[] src = text.toCharArray();
        int offset = 0;
        final StringBuilder builder = new StringBuilder();//用来记录解析后的字符串
        StringBuilder expression = null;//用来记录一个占位符的字面值
        while (start > -1) {
            if (start > 0 && src[start - 1] == '\\') {
                //this open token is escaped. remove the backslash and continue.
                //遇到转义的开始标记，则直接将前面的字符串以及开始标记追加到builder中
                builder.append(src, offset, start - offset - 1).append(openToken);
                //修改offset的位置
                offset = start + openToken.length();
            } else {
                //found open token. let's search close token.
                //查找到开始标记，且未转义
                if (expression == null) {
                    expression = new StringBuilder();
                } else {
                    expression.setLength(0);
                }
                //将前面的字符串追加到builder中
                builder.append(src, offset, start - offset);
                //修改offset位置
                offset = start + openToken.length();
                //从offset向后继续查找结束标志
                int end = text.indexOf(closeToken, offset);
                while (end > -1) {
                    if (end > offset && src[end - 1] == '\\') {
                        //this close token is escaped. remove the backslash and continue.
                        //处理转义的结束标志
                        expression.append(src, offset, end - offset - 1).append(closeToken);
                        offset = end + closeToken.length();
                        end = text.indexOf(closeToken, offset);
                    } else {
                        //将开始标记和技术标记之间的字符串追加到expression中保存
                        expression.append(src, offset, end - offset);
                        offset = end + closeToken.length();
                        break;
                    }
                }
                if (end == -1) {
                    //close token was not found.
                    builder.append(src, start, src.length - start);
                    offset = src.length;
                } else {
                    //将占位符的字面值交给TokenHandler处理，并将处理结果追加到builder中保存，最终拼凑出解析后的完整内容
                    builder.append(handler.handleToken(expression.toString()));
                    offset = end + closeToken.length();
                }
            }
            //移动start
            start = text.indexOf(openToken, offset);
        }
        if (offset < src.length) {
            builder.append(src, offset, src.length - offset);
        }
        return builder.toString();
    }
}
