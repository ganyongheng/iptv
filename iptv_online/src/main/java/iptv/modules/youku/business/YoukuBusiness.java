package iptv.modules.youku.business;

import iptv.modules.tx.business.BaseBusiness;
import iptv.modules.tx.factory.BaseBusinessFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Author wyy
 * Date 2022/3/17 17:40
 **/
@Component
public class YoukuBusiness extends BaseBusiness implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        BaseBusinessFactory.create("youku", this);
    }
}
