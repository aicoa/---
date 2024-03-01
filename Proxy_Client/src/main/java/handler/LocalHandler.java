package handler;

import com.config.ConfigParser;
import com.protocol.ProxyMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashMap;

/**
 * @author aicoa
 * @date 2024/3/1 22:54
 */
public class LocalHandler extends ChannelInboundHandlerAdapter {
    private  ClientHandler clientHandler = null;
    private  String remoteChannelId =null ;
    private ChannelHandlerContext localctx;

    public LocalHandler(ClientHandler clientHandler,String channelId){
        this.clientHandler=clientHandler;
        this.remoteChannelId=channelId;
    }

    public ChannelHandlerContext getLocalctx(){return localctx; }

    //��ʼ������

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.localctx=ctx;
        System.out.println(this.getClass()+"\r\n �뱾�ض˿ڽ����ɹ���"+ctx.channel().remoteAddress());
    }

    //��ȡ�������������������--��������


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] data =(byte[]) msg;
        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setType(ProxyMessage.type_data);
        HashMap<String,Object> metaData =new HashMap<>();       //ChannelId-ClientKey
        metaData.put("channelId",remoteChannelId);
        metaData.put("clientKey", ConfigParser.get("client-key"));
        proxyMessage.setMetaData(metaData);
        proxyMessage.setData(data);
        //������������Ӧ֮��Ӧ��������������
        this.clientHandler.getCtx().writeAndFlush(proxyMessage);
        System.out.println(this.getClass()+"\r\n �յ�����"+ctx.channel().remoteAddress()+"�����ݣ�������Ϊ"+data.length+"�ֽ�");
    }

    //�Ͽ�����

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setType(ProxyMessage.type_disconnect);
        HashMap<String,Object> metaData = new HashMap<>();
        metaData.put("channelId",remoteChannelId);
        proxyMessage.setMetaData(metaData);
        this.clientHandler.getCtx().writeAndFlush(proxyMessage);
        System.out.println(this.getClass()+"\r\n �뱾�ضϿ����ӣ�"+ctx.channel().remoteAddress());
    }
    //�����쳣


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        System.out.println(this.getClass()+"\r\n �����쳣�Ͽ�");
        cause.printStackTrace();
    }
}
