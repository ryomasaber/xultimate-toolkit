package org.danielli.xultimate.context.format;

import java.text.MessageFormat;

/**
 * 格式化工具类。
 * 
 * @author Daniel Li
 * @since 18 Jun 2013
 */
public class FormatterUtils {

	/**
	 * 格式化。
	 * 
	 * @param source 原始源。
	 * @param parameter 源始源参数。
	 * @return 目标源。
	 * @throws FormatException 格式化异常，会对格式化过程中出现的异常封装并抛出。
	 */
	public static String format(String source, Object... parameter) throws FormatException {
		try {
			return MessageFormat.format(source, parameter);
		} catch (Exception e) {
			throw new FormatException(e.getMessage(), e);
		}
	}
}
