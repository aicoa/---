package handler;

import com.protocol.ProxyMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;

/**
 * @author aicoa
 * @date 2024/3/1 23:38
 * @description �ͻ�����������
 */
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    private EventLoopGroup workerGroup = null;
    public HeartBeatHandler() {
    }
    public HeartBeatHandler(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
    }

    //������
    private  static final ProxyMessage heart_beat = new ProxyMessage(ProxyMessage.type_keeplive);

    //�ͻ���д��ʱʱ�䴥��
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println(this.getClass()+"\r\n д��ʱ������������");
        ctx.writeAndFlush(heart_beat);
    }
    //�쳣���������쳣ʱֱ�ӹرշ���������(���緢������ʧ�ܣ����ܷ�����������)
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(this.getClass()+"\r\n �����쳣�����ж�");
        workerGroup.shutdownGracefully();
        ctx.fireExceptionCaught(cause);
        ctx.channel().close();
    }
}
