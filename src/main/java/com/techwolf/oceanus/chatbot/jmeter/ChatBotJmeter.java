package com.techwolf.oceanus.chatbot.jmeter;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.RpcException;
import com.techwolf.oceanus.chatbot.api.notice.ChatMessage;
import com.techwolf.oceanus.chatbot.api.notice.ChatNoticeService;
import com.techwolf.oceanus.chatbot.api.notice.Notice;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.thrift.transport.TTransportException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * User: Jacky
 * Date: 2018-05-21
 */
public class ChatBotJmeter extends AbstractJavaSamplerClient {

    private List<ChatNoticeService> clients=new ArrayList<>();

    private static Random random=new Random();

    public static void main (String args[]) {
        System.out.println("test in main");
        for (int i = 0; i < 20 ; i++) {
            ChatBotJmeter chatBotJmeter=new ChatBotJmeter();
            chatBotJmeter.setupTest(null);
            String filepath = "/home/wanglin/data/data/chat-eg-13.txt";
            File file = new File(filepath);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() == 0) {
                        continue;
                    } else {
                        Arguments arguments=new Arguments();
                        arguments.addArgument("message", line);
                        JavaSamplerContext javaSamplerContext = new JavaSamplerContext(arguments);
                        SampleResult sampleResult = chatBotJmeter.runTest(javaSamplerContext);
                        System.out.println("++++++++" + sampleResult.getResponseMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments=new Arguments();
        arguments.addArgument("message","chat-corpus    33810005-28714473   28714473    33810005    1522799362000   我公司正在招贤纳士，可否邀您聊聊呢？  0   1");
        return arguments;
    }

    public void setupTest(JavaSamplerContext arg0) {
        try {
            clients.add(create("zookeeper://192.168.1.42:2181"));
            clients.add(create("zookeeper://192.168.1.42:2181"));
        } catch (TTransportException e) {
            e.printStackTrace();
        }
    }

    private ChatNoticeService create(String host) throws TTransportException {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("oceanus-chat-consumer");
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress(host);
        ReferenceConfig<ChatNoticeService> reference = new ReferenceConfig<>();
        reference.setApplication(application);
        reference.setRegistry(registryConfig);
        reference.setInterface(ChatNoticeService.class);
        reference.setVersion("1.0.0");
        return reference.get();
    }

    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        int index=random.nextInt(2);
        SampleResult sr = new SampleResult();
        sr.setSampleLabel("dubbo_consumer");
        String message=javaSamplerContext.getParameter("message");
        if (message.trim().length() == 0){
            sr.setSuccessful(false);
            return sr;
        }
        String[] pair = message.trim().split("\t");
        long fromId = Long.parseLong(pair[2]);
        long toId = Long.parseLong(pair[3]);
        long timestamp = Long.parseLong(pair[4]);
        String content = pair[5];
        int type = Integer.parseInt(pair[7]);
        if (type != 1){
            sr.setSuccessful(false);
            return sr;
        }
        int fromIdentity = Integer.parseInt(pair[8]);
        int messageType =  1; //消息类型
        int actionType = 0; //消息类型不是0才有用，默认设置为0
        int chatStatus = 3; //单聊还是双聊，双聊是3
        sr.sampleStart();//用来统计执行时间--start--
        sr.setSuccessful(true);
        try {
            List<Notice> notices = clients.get(index).chatNotice(new ChatMessage(0, fromId, toId, fromIdentity, messageType, timestamp, content, actionType, chatStatus));
            if (null != notices && notices.size() > 0) {
               sr.setResponseMessage(notices.get(0).getContent());
            }
        } catch (RpcException e) {
            sr.setSuccessful(false);
            throw new RuntimeException(e);
        } catch (Exception e){

        } finally {
            sr.sampleEnd();
        }
        return sr;
    }

    public void teardownTest(JavaSamplerContext arg0) {
    }
}
