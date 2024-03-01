package handler;

import com.config.ConfigParser;
import com.protocol.ProxyMessage;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import handler.LocalHandler;
import helper.ClientBootStrapHelper;
/**
 * @author aicoa
 * @date 2024/3/1 0:41
 * @difficut ����м����ѵ㣬һ����channelread�ĸ����ԣ�Channel �� EventLoopGroup������һ��Ǳ�ڵ��ѵ㡣ȷ��������Դ�����ǲ�����Ҫʱ���ʵ��عرպ��ͷ�
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    //��channelsȫ�ֹ���
    private ChannelGroup channels =new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    //ServerPort ��LocalPortӳ��map
    private ConcurrentHashMap<Integer,Integer> portMap = new ConcurrentHashMap<>();
    //����localChannel���������̵߳��л�,�����ṩ���ַ��������ǲ�����
    private EventLoopGroup localGroup = new NioEventLoopGroup();
    //ÿ���ⲿ����channelid�����Ӧ������handler��ӳ���ϵ
    private ConcurrentHashMap<String,LocalHandler> localHandlerMap = new ConcurrentHashMap<>();

    private ChannelHandlerContext ctx =null;
    public  ChannelHandlerContext getCtx(){return ctx;}

    //�������ӣ���ʼ��
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx =ctx;
        //���ӽ���������ע��
        ProxyMessage ms =new ProxyMessage();
        ms.setType(ProxyMessage.type_register);
        HashMap<String,Object> metaData =new HashMap<>();
        metaData.put("clientKey", ConfigParser.get("client-key"));
        //��ȡ�����еķ������˿�
        ArrayList<Integer> ServerPortArr =new ArrayList<>();
        for(Map<String,Object> item: ConfigParser.getPortArr()){
            ServerPortArr.add((Integer) item.get("server-port"));
            //����ӳ���ϵ
            portMap.put((Integer) item.get("server-port"),(Integer) item.get("client-port"));
        }
        metaData.put("ports",ServerPortArr);
        ms.setMetaData(metaData);
        ctx.writeAndFlush(ms);
        System.out.println(this.getClass()+"\r\n ����������ӳɹ���ע����...");
    }

    //��ȡ����
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ProxyMessage ms =(ProxyMessage) msg;
        switch (ms.getType()){
            //��Ȩ
            case ProxyMessage.type_auth:
                processAuth(ms);
                break;
            //�ⲿ�룬�������������ӣ�
            case ProxyMessage.type_connect:
                processConnect(ms);
                break;
            //�Ͽ�����
            case ProxyMessage.type_disconnect:
                processDisConnect(ms);
                break;
            case ProxyMessage.type_keeplive:
                break;
            case ProxyMessage.type_data:
                processData(ms);
                break;
        }
    }

    //��д�����ж�

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            // �ر�����channel
            channels.close();

            // ���û���ʾ�Ͽ����ӵ���Ϣ
            String message = this.getClass().getName() + "\r\n ����������ӶϿ�";
            System.out.println(message); // ����̨������û��ɼ�
            // ���ŵعرձ���group
            localGroup.shutdownGracefully();

        } catch (Exception e) {
            System.err.println("Error occurred during channel inactive handling"); // ������Ϣ���������̨
        } finally {
            // ���ø��ദ��ȷ�����б�Ҫ��Netty�ڲ�������ִ��
            super.channelInactive(ctx);
        }
    }

    //�쳣����
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(this.getClass()+"\r\n �����쳣");
        cause.printStackTrace();
        //�����쳣
        ctx.fireExceptionCaught(cause);
        ctx.channel().close();
    }
    /**
     * @Description ��Ȩ����
    **/
    public void processAuth(ProxyMessage proxyMessage){
        if ((Boolean) proxyMessage.getMetaData().get("isSuccess")){
            System.out.println(this.getClass()+"\r\n ע��ɹ�");
        }
        else{
            ctx.fireExceptionCaught(new Throwable());
            ctx.channel().close();
            System.out.println(this.getClass()+"\r\n ע��ʧ�ܣ�ԭ��"+proxyMessage.getMetaData().get("reason"));
        }
    }

    /**
     * @Description ���ӽ���
     **/
    public void processConnect(ProxyMessage proxyMessage){
        ClientHandler clientHandler =this;
        ClientBootStrapHelper localHelper = new ClientBootStrapHelper();
        ChannelInitializer channelInitializer = new ChannelInitializer() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
            LocalHandler localHandler = new LocalHandler(clientHandler,proxyMessage.getMetaData().get("channelId").toString());

            //��pipline�����handler������֤��������˳��
            channel.pipeline().addLast(
                new ByteArrayEncoder(),
                new ByteArrayDecoder(),
                localHandler
            );
            channels.add(channel);
                localHandlerMap.put(proxyMessage.getMetaData().get("channelId").toString(),localHandler);
            }
        };
        String localhost =(String) ConfigParser.get("local-host");
        //����portMap��Զ�̷���˿���Ϊkey��ȡ��Ӧ�ı��ض˿�
        int remotePort = (Integer) proxyMessage.getMetaData().get("remotePort");
        int localPort = portMap.get(remotePort);
        localHelper.start(localGroup,channelInitializer,localhost,localPort);
        System.out.println(this.getClass()+"\r\n ������"+remotePort+"�˿ڽ������ӣ������򱾵�"+localPort+"�˿ڽ�������");
    }

    /**
     * @Description ����Ͽ�
     * **/
    public void processDisConnect(ProxyMessage proxyMessage){
        String channelID = proxyMessage.getMetaData().get("channelId").toString();
        LocalHandler handler =localHandlerMap.get(channelID);
        if (handler !=null){
            handler.getLocalctx().close();
            localHandlerMap.remove(channelID);
        }
    }

    public void processData(ProxyMessage msg){
        if (msg.getData()==null || msg.getData().length<=0) return;

        String channelID = msg.getMetaData().get("channelId").toString();
        LocalHandler localHandler=localHandlerMap.get(channelID);

        if (localHandler != null) localHandler.getLocalctx().writeAndFlush(msg.getData());

        System.out.println(this.getClass()+"\r\n �յ����������ݣ�������Ϊ"+msg.getData().length+"�ֽ�");

    }






    }


