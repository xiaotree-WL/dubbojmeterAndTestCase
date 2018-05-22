import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.techwolf.oceanus.chatbot.api.notice.ChatMessage;
import com.techwolf.oceanus.chatbot.api.notice.ChatNoticeService;
import com.techwolf.oceanus.chatbot.api.notice.Notice;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.List;

/**
 * User: Jacky
 * Date: 2018-05-15
 */
public class ChatNoticeBotApplicationTest {

    private ChatNoticeService chatNoticeService;

    @Before
    public void init(){
        ApplicationConfig application = new ApplicationConfig();
        application.setName("oceanus-chat-consumer");
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://192.168.1.11:2181");

        ReferenceConfig<ChatNoticeService> reference = new ReferenceConfig<>();
        reference.setApplication(application);
        reference.setRegistry(registryConfig);
        reference.setInterface(ChatNoticeService.class);
        reference.setVersion("1.0.1");
        reference.setTimeout(16000000);
        chatNoticeService = reference.get();

    }

    @Test
    public void chatTest(){
        String filepath = "/workspace/boss/wave-bot/data/chat-2018-04-03.txt";
        File file = new File(filepath);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            int count=0;
            long start=System.currentTimeMillis();
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0)
                    continue;

                String[] pair = line.trim().split("\t");
                long fromId = Long.parseLong(pair[2]);
                long toId = Long.parseLong(pair[3]);
                long timestamp = Long.parseLong(pair[4]);
                String content = pair[5];
                int type = Integer.parseInt(pair[7]);
                if (type != 1)
                    continue;
                int fromIdentity = Integer.parseInt(pair[8]);
                int messageType =  1; //消息类型
                int actionType = 0; //消息类型不是0才有用，默认设置为0
                int chatStatus = 3; //单聊还是双聊，双聊是3

                if(count==50){
                    break;
                }

                ChatMessage chatMessage = new ChatMessage(0, fromId, toId, fromIdentity, messageType, timestamp, content, actionType, chatStatus);
                //ForkJoinPool.commonPool().execute(()->chatNoticeService.chatNotice(chatMessage);
                List<Notice> notices = chatNoticeService.chatNotice(chatMessage);
                count=count+1;
                System.out.println("================ "+count+"->"+ fromId + "-" + toId + " : " + content);
                if (null != notices && notices.size() > 0) {
                    System.out.println("+++++++++++++++++++++++++++++++++" + notices.get(0).getContent());
                }
            }
            System.out.println("================exe time "+(System.currentTimeMillis()-start));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
