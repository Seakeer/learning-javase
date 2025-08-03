package me.seakeer.learning.javase.network.websocket;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * WebSocketFrame;
 * WebSocket数据帧结构类；
 * 数据帧的标志位使用boolean表示
 * opcode定义为枚举类
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-------+-+-------------+-------------------------------+
 * |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
 * |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
 * |N|V|V|V|       |S|             |   (if payload len==126/127)   |
 * | |1|2|3|       |K|             |                               |
 * +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 * |     Extended payload length continued, if payload len == 127  |
 * + - - - - - - - - - - - - - - - +-------------------------------+
 * |                               |Masking-key, if MASK set to 1  |
 * +-------------------------------+-------------------------------+
 * | Masking-key (continued)       |          Payload Data         |
 * +-------------------------------- - - - - - - - - - - - - - - - +
 * :                     Payload Data continued ...                :
 * +---------------------------------------------------------------+
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
public class WebSocketFrame {

    private final boolean fin;

    private final boolean rsv1;

    private final boolean rsv2;

    private final boolean rsv3;

    private final OpCode opCode;

    private final boolean mask;

    /**
     * 数据帧的数据部分长度，单位为字节
     */
    private final int payloadLen;

    /**
     * 掩码，可为null(服务端发给客户端的数据帧)
     */
    private final Integer maskKey;

    /**
     * 数据帧的数据部分，可能是掩码覆盖后的内容(客户端发送给服务端的数据)
     * 获取解码后的可使用 deMaskPayload() 方法
     */
    private final byte[] payload;

    public WebSocketFrame(boolean fin, OpCode opCode, boolean mask,
                          int payloadLen, Integer maskKey, byte[] payload) {
        this.fin = fin;
        this.rsv1 = false;
        this.rsv2 = false;
        this.rsv3 = false;
        this.opCode = opCode;
        this.mask = mask;
        this.payloadLen = payloadLen;
        this.maskKey = maskKey;
        this.payload = payload;
    }

    public WebSocketFrame(boolean fin, boolean rsv1, boolean rsv2, boolean rsv3,
                          OpCode opCode, boolean mask,
                          int payloadLen, Integer maskKey, byte[] payload) {
        this.rsv1 = rsv1;
        this.rsv2 = rsv2;
        this.rsv3 = rsv3;
        this.fin = fin;
        this.opCode = opCode;
        this.mask = mask;
        this.payloadLen = payloadLen;
        this.maskKey = maskKey;
        this.payload = payload;
    }

    public boolean isFin() {
        return fin;
    }

    public boolean isRsv1() {
        return rsv1;
    }

    public boolean isRsv2() {
        return rsv2;
    }

    public boolean isRsv3() {
        return rsv3;
    }

    public OpCode getOpCode() {
        return opCode;
    }

    public boolean isMask() {
        return mask;
    }

    public int getPayloadLen() {
        return payloadLen;
    }

    public Integer getMaskKey() {
        return maskKey;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * 解码掩码数据
     */
    public byte[] deMaskPayload() {
        if (payloadLen <= 0) {
            return null;
        }
        byte[] deMaskPayload = new byte[payloadLen];
        System.arraycopy(payload, 0, deMaskPayload, 0, payloadLen);
        if (mask) {
            for (int i = 0; i < payloadLen; i++) {
                deMaskPayload[i] = (byte) (payload[i] ^ (maskKey >> ((i % 4) * 8) & 0xFF));
            }
        }
        return deMaskPayload;
    }

    public static int genMaskKey() {
        return SECURE_RANDOM.nextInt();
    }

    /**
     * 对数据进行掩码覆盖
     *
     * @param bytes   原始数据
     * @param maskKey 掩码
     * @return 掩码覆盖后的数据
     */
    public static byte[] mask(byte[] bytes, int maskKey) {
        byte[] maskedBytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            maskedBytes[i] = (byte) (bytes[i] ^ ((maskKey >> ((i % 4) * 8)) & 0xFF));
        }
        return maskedBytes;
    }

    /**
     * 获取整个数据帧长度，单位为字节
     */
    public int getFrameLengthBytes() {

        // 至少 2 字节的帧头
        int frameLength = 2;

        if (payloadLen >= 126) {
            if (payloadLen <= 65535) {
                // 2 字节的 Extended Payload length
                frameLength += 2;
            } else {
                // 8 字节的 Extended Payload length
                frameLength += 8;
            }
        }

        if (mask) {
            // 4 字节的 Masking Key
            frameLength += 4;
        }

        // Payload 数据长度
        frameLength += payloadLen;

        return frameLength;
    }

    public enum OpCode {

        CONTINUATION_FRAME(0),

        TEXT_FRAME(1),

        BINARY_FRAME(2),

        CLOSE_FRAME(8),
        PING_FRAME(9),
        PONG_FRAME(10),

        RSV_CTL_1(3),
        RSV_CTL_2(4),
        RSV_CTL_3(5),
        RSV_CTL_4(6),
        RSV_CTL_5(7),
        RSV_CTL_6(11),
        RSV_CTL_7(12),
        RSV_CTL_8(13),
        RSV_CTL_9(14),
        RSV_CTL_10(15);

        private final int code;

        OpCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static OpCode fromCode(int code) {
            for (OpCode opCode : OpCode.values()) {
                if (opCode.code == code) {
                    return opCode;
                }
            }
            return null;
        }
    }

    public static WebSocketFrame serverTextFrame(String payload) {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        return new WebSocketFrame(true, OpCode.TEXT_FRAME, false, payloadBytes.length, null, payloadBytes);
    }

    public static WebSocketFrame serverCloseFrame() {
        return new WebSocketFrame(true, OpCode.CLOSE_FRAME, false, 0, null, null);
    }

    public static WebSocketFrame serverPingFrame() {
        return new WebSocketFrame(true, OpCode.PING_FRAME, false, 0, null, null);
    }

    public static WebSocketFrame serverPongFrame() {
        return new WebSocketFrame(true, OpCode.PONG_FRAME, false, 0, null, null);
    }

    public static WebSocketFrame clientTextFrame(String payload) {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        int maskKey = genMaskKey();
        return new WebSocketFrame(true, OpCode.TEXT_FRAME, true, payloadBytes.length, maskKey, mask(payloadBytes, maskKey));
    }

    public static WebSocketFrame clientCloseFrame() {
        return new WebSocketFrame(true, OpCode.CLOSE_FRAME, true, 0, genMaskKey(), null);
    }

    public static WebSocketFrame clientPingFrame() {
        return new WebSocketFrame(true, OpCode.PING_FRAME, true, 0, genMaskKey(), null);
    }

    public static WebSocketFrame clientPongFrame() {
        return new WebSocketFrame(true, OpCode.PONG_FRAME, true, 0, genMaskKey(), null);
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

}