package iptv.common;

/**
 * 自定义异常方法
 * @author DerekLee
 *
 */
public class BossException extends Exception {
    private static final long serialVersionUID = 223325160097525296L;


    /**
     * 参考 ReturnUtil
     */
    private String retCode;

    public String getRetCode() {
        return retCode;
    }

    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    public BossException() {
    }

    public BossException(String content) {
        super(content);
    }

    public BossException(String content,String retCode) {
        super(content);
        this.retCode = retCode;
    }
}
