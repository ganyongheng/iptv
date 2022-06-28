package iptv.common;


import org.apache.commons.lang3.StringUtils;

public class CheckUtils {

    //--后续可改造成，抛出自定义异常，以支持自定义异常代码
    public static boolean checkEmpty(String s, String msg) throws Exception {
        if (StringUtils.isEmpty(s))
            throw new BusinessException(msg);
        return true;
    }

    public static boolean checkNull(Object obj, String msg) throws Exception {
        if (obj == null)
            throw new BusinessException(msg);
        return true;
    }

    public static int arraysIndexOf(String[] array, String obj) throws Exception {
        int index = -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(obj)) {
                index = i;
                break;
            }
        }
        return index;
    }

    public static boolean checkEmpty(String s, String msg, String retcode) throws Exception {
        if (StringUtils.isEmpty(s))
            throw new BusinessException(msg, retcode);
        return true;
    }

    public static boolean checkEmpty(Integer s, String msg, String retcode) throws Exception {
        if (null == s)
            throw new BusinessException(msg, retcode);
        return true;
    }

}
