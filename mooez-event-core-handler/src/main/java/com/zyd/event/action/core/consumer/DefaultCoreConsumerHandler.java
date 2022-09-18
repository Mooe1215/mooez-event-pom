package com.zyd.event.action.core.consumer;

import com.zyd.event.action.apply.consumer.IMooezEventHandler;
import com.zyd.event.action.apply.consumer.MooezEvent;
import com.zyd.event.standard.entity.MooezMessage;
import com.zyd.event.standard.inter.CoreProducerStandard;
import com.zyd.event.standard.interceptor.MooezConsumerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Description:
 * 消费者的默认核心服务层实现类
 * Author: Mooez
 * Date: 2022/9/19 0:04
 */
@Slf4j
public class DefaultCoreConsumerHandler implements CoreProducerStandard {

    /**
     * 注入消费者拦截器的集合
     */
    @Autowired(required = false)
    private List<MooezConsumerInterceptor> consumerInterceptors;

    /**
     * 注入消费者的实现类
     */
    @Autowired
    private List<IMooezEventHandler> mooezEventHandlers;

    /**
     * 核心服务层处理消息
     *
     * @param mooezMessage
     */
    @Override
    public void sendMessage(MooezMessage mooezMessage) {
        //进行消费者拦截器的相关处理
        if (consumerInterceptors != null) {
            for (MooezConsumerInterceptor consumerInterceptor : consumerInterceptors) {
                try {
                    //依次判断拦截器是否可以执行
                    if (!consumerInterceptor.isSupport(mooezMessage)) continue;

                    //调用拦截器处理当前消息
                    mooezMessage = consumerInterceptor.consumerInterceptor(mooezMessage);
                    //如果返回空，则消费端不会再消费到这条消息
                    if (mooezMessage == null) return;
                } catch (Throwable t) {
                    log.error("[consumer event interceptor] 消费端拦截器异常", t);
                }
            }
        }

        //调用应用层的代码进消息的实际处理 - 提供接口给开发者进行调用后处理消息
        for (IMooezEventHandler eventHandler : mooezEventHandlers) {
            //消息的事件类型
            String eventType = mooezMessage.getEventType();
            //处理器需要处理的事件类型
            MooezEvent mooezEvent = eventHandler.getClass().getAnnotation(MooezEvent.class);
            if (mooezEvent == null) continue;

            //判断事件类型是否相匹配
            if (eventType.equals(mooezEvent.value())) {
                //当前eventHandler 就是需要处理消息的方法的处理器
                eventHandler.eventHandler(mooezMessage.getMsg(), mooezMessage);
                break;
            }
        }
    }
}
