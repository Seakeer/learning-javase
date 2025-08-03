package me.seakeer.learning.javase.network;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

/**
 * MsgEnDecoder;
 *
 * @author Seakeer;
 * @date 2024/12/30;
 */
public class MsgEnDecoder {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final CharsetDecoder DECODER = CHARSET.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

    /**
     * 将ByteBuffer中可解码的字节解码为字符串
     * 无法解码的字节保留在ByteBuffer中
     *
     * @param buffer
     * @return
     */
    public static String decodeMsg(ByteBuffer buffer) {

        StringBuilder decodedString = new StringBuilder();

        // 初始位置和限制
        int initPosition = buffer.position();
        int initLimit = buffer.limit();

        try {
            // 尝试解码整个缓冲区
            CharBuffer charBuffer = DECODER.decode(buffer);
            decodedString.append(charBuffer);
        } catch (CharacterCodingException e) {

            // 记录发生错误的位置
            // initPosition --- errorPosition 为可解码的区间
            // errorPosition --- limit 为未解码的区间
            int errorPosition = buffer.position();

            // 设置为可解码的区间
            buffer.position(initPosition);
            buffer.limit(errorPosition);
            // 处理可正常解码的字节
            decodedString.append(decodeMsg(buffer));

            // 设置为未解码的区间
            buffer.position(errorPosition);
            buffer.limit(initLimit);
        }

        if (buffer.hasRemaining()) {
            // 还有未成功解码的字节，则将缓冲区 compact，方便写入新的字节
            buffer.compact();
        } else {
            // 所有字节都解码完成，则清空缓冲区
            buffer.clear();
        }

        return decodedString.toString();
    }

    public static ByteBuffer encodeMsg(String msg) {
        byte[] bytes = msg.getBytes(CHARSET);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        return byteBuffer;
    }

    public static int encodeMsg(String msg, ByteBuffer targetBuffer) {
        byte[] bytes = msg.getBytes(CHARSET);
        if (targetBuffer.remaining() < bytes.length) {
            return bytes.length;
        }
        targetBuffer.put(bytes);
        targetBuffer.flip();
        return 0;
    }

}
