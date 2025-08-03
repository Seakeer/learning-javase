package me.seakeer.learning.javase.network.websocket;

/**
 * WebSocketEnDecoder;
 * WebSocket数据帧编解码器
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
public class WebSocketEnDecoder {

    public static WebSocketFrame decode(byte[] data) {
        // 数据至少包含两个字节
        if (data == null || data.length < 2) {
            return null;
        }

        // 解析第一个字节
        int firstByte = data[0];
        boolean fin = (firstByte & 0x80) != 0;
        boolean rsv1 = (firstByte & 0x40) != 0;
        boolean rsv2 = (firstByte & 0x20) != 0;
        boolean rsv3 = (firstByte & 0x10) != 0;
        int opCodeValue = firstByte & 0x0F;
        WebSocketFrame.OpCode opCode = WebSocketFrame.OpCode.fromCode(opCodeValue);

        // 解析第二个字节
        int secondByte = data[1];
        boolean mask = (secondByte & 0x80) != 0;
        int payloadLen = secondByte & 0x7F;

        int headerLength = 2;
        int maskKey = 0;

        // 处理扩展 Payload 长度
        if (payloadLen == 126) {
            if (data.length < 4) {
                return null;
            }
            payloadLen = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
            headerLength = 4;
        } else if (payloadLen == 127) {
            if (data.length < 10) {
                return null;
            }
            payloadLen = (int) (((long) (data[2] & 0xFF) << 56) |
                    ((long) (data[3] & 0xFF) << 48) |
                    ((long) (data[4] & 0xFF) << 40) |
                    ((long) (data[5] & 0xFF) << 32) |
                    ((long) (data[6] & 0xFF) << 24) |
                    ((data[7] & 0xFF) << 16) |
                    ((data[8] & 0xFF) << 8) |
                    (data[9] & 0xFF));
            headerLength = 10;
        }

        // 处理掩码键
        if (mask) {
            if (data.length < headerLength + 4) {
                return null;
            }
            maskKey = ((data[headerLength] & 0xFF) << 24) |
                    ((data[headerLength + 1] & 0xFF) << 16) |
                    ((data[headerLength + 2] & 0xFF) << 8) |
                    (data[headerLength + 3] & 0xFF);
            headerLength += 4;
        }

        // 复制 Payload 数据
        byte[] payload = new byte[payloadLen];
        if (data.length < headerLength + payloadLen) {
            return null;
        }
        System.arraycopy(data, headerLength, payload, 0, payloadLen);

        return new WebSocketFrame(fin, rsv1, rsv2, rsv3, opCode, mask, payloadLen, maskKey, payload);
    }

    public static byte[] encode(WebSocketFrame frame) {
        int headerLength = 2;
        int payloadLen = frame.getPayloadLen();
        Integer maskKey = frame.getMaskKey();
        byte[] payload = frame.getPayload();

        // 计算扩展 Payload 长度
        if (payloadLen >= 126) {
            headerLength += 2;
            if (payloadLen > 65535) {
                headerLength += 6;
            }
        }

        // 处理掩码键
        if (frame.isMask()) {
            headerLength += 4;
        }

        byte[] frameData = new byte[headerLength + payloadLen];
        int index = 0;

        // 设置第一个字节
        int firstByte = (frame.isFin() ? 0x80 : 0) |
                (frame.isRsv1() ? 0x40 : 0) |
                (frame.isRsv2() ? 0x20 : 0) |
                (frame.isRsv3() ? 0x10 : 0) |
                frame.getOpCode().getCode();
        frameData[index++] = (byte) firstByte;

        // 设置第二个字节
        int secondByte = (frame.isMask() ? 0x80 : 0) |
                (payloadLen < 126 ? payloadLen : (payloadLen > 65535 ? 127 : 126));
        frameData[index++] = (byte) secondByte;

        // 设置扩展 Payload 长度
        if (payloadLen == 126) {
            frameData[index++] = (byte) ((payloadLen >> 8) & 0xFF);
            frameData[index++] = (byte) (payloadLen & 0xFF);
        } else if (payloadLen > 65535) {
            frameData[index++] = (byte) ((payloadLen >> 56) & 0xFF);
            frameData[index++] = (byte) ((payloadLen >> 48) & 0xFF);
            frameData[index++] = (byte) ((payloadLen >> 40) & 0xFF);
            frameData[index++] = (byte) ((payloadLen >> 32) & 0xFF);
            frameData[index++] = (byte) ((payloadLen >> 24) & 0xFF);
            frameData[index++] = (byte) ((payloadLen >> 16) & 0xFF);
            frameData[index++] = (byte) ((payloadLen >> 8) & 0xFF);
            frameData[index++] = (byte) (payloadLen & 0xFF);
        }

        // 设置掩码键
        if (frame.isMask()) {
            frameData[index++] = (byte) ((maskKey >> 24) & 0xFF);
            frameData[index++] = (byte) ((maskKey >> 16) & 0xFF);
            frameData[index++] = (byte) ((maskKey >> 8) & 0xFF);
            frameData[index++] = (byte) (maskKey & 0xFF);
        }

        if (payload != null) {
            System.arraycopy(payload, 0, frameData, index, payloadLen);
        }
        return frameData;
    }
}
