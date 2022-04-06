package com.amansprojects.oaxaca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PacketWriter {
    public ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public void writeByte(byte b) {
        baos.write(b);
    }

    public void writeByteArray(byte[] bytes) {
        try {
            baos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeInt(int i) {
        ByteBuffer buffer = ByteBuffer.allocate(Math.round((float)i/128));
        ByteUtils.writeVarInt(i, buffer);
        writeByteArray(buffer.array());
    }

    public void writeString(String s) {
        int stringLength = s.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer buffer = ByteBuffer.allocate(Math.round((float)stringLength/128) + stringLength);
        ByteUtils.writeString(s, buffer);
        writeByteArray(buffer.array());
    }

    public byte[] finish() {
        byte[] contents = baos.toByteArray();

        int packetLengthVarIntSize = Math.round((float)contents.length/128);
        byte[] output = new byte[packetLengthVarIntSize + contents.length]; // The final packet array

        ByteBuffer lengthVarIntBuffer = ByteBuffer.allocate(packetLengthVarIntSize);
        ByteUtils.writeVarInt(contents.length, lengthVarIntBuffer);

        System.arraycopy(lengthVarIntBuffer.array(), 0, output, 0, 1);
        System.arraycopy(contents, 0, output, 1, contents.length);
        return output;
    }
}
