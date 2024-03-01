package helper;

import com.codec.ProxyMessageDecoder;
import com.codec.ProxyMessageEncoder;
import com.config.ConfigParser;
import handler.ClientHandler;
import handler.HeartBeatHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author aicoa
 * @date 2024/3/2 0:43
 */
public class ClientRunHelper {
    private static final int MAX_FRAME_LENGTH = Integer.MAX_VALUE;
    private static final int LENGTH_FIELD_OFFSET = 0;
    private static final int LENGTH_FIELD_LENGTH = 4;
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 4;

    private static final int READER_IDLE_TIME = 0;
    private static final int WRITER_IDLE_TIME = 30;
    private static final int ALL_IDLE_TIME = 0;

    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public void start(){
        ClientBootStrapHelper clientBootStrapHelper = new ClientBootStrapHelper();
        String serverHost = (String) ConfigParser.get("server-host");
        int serverPort = (Integer) ConfigParser.get("server-port");
        ChannelInitializer channelInitializer = new ChannelInitializer() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline().addLast(
                        new IdleStateHandler(READER_IDLE_TIME,WRITER_IDLE_TIME,ALL_IDLE_TIME, TimeUnit.SECONDS),
                        new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH,LENGTH_FIELD_OFFSET,LENGTH_FIELD_LENGTH,LENGTH_ADJUSTMENT,INITIAL_BYTES_TO_STRIP),
                        new ProxyMessageDecoder(),
                        new ProxyMessageEncoder(),
                        new ClientHandler(),
                        //�ͻ����������ƴ�����
                        new HeartBeatHandler(workerGroup)
                );
            }
        };
        clientBootStrapHelper.start(workerGroup,channelInitializer,serverHost,serverPort);
    }
}
