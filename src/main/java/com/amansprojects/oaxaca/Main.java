package com.amansprojects.oaxaca;

import com.google.gson.Gson;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class Main {
    private static ServerSocket serverSocket = null;
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        Logger.log("Starting Oaxaca server...");
        serverSocket = new ServerSocket(25565);

        new Thread(() -> {
            try { connect(); }
            catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    public static void connect() throws IOException {
        Socket socket = serverSocket.accept();
        new Thread(() -> {
            try { connect(); }
            catch (IOException e) { e.printStackTrace(); }
        }).start(); // Listen for a new connection on another thread

        InputStream input = socket.getInputStream();
        ConnectionState state = ConnectionState.HANDSHAKING;

        while (true) {
            try {
                int length = input.read(); // TODO: read this as a VarInt
                byte[] dat = new byte[length];
                input.read(dat);
                try {
                    if (dat[0] == 0x00) {
                        switch (state) {
                            case HANDSHAKING -> { // Client sent a handshake
                                switch (new HandshakePacket(dat).nextState) {
                                    case 1 -> state = ConnectionState.STATUS;
                                    case 2 -> state = ConnectionState.LOGIN;
                                }
                                continue;
                            }
                            case STATUS -> { // Client sent a status request, respond with a status response
                                new StatusRequestPacket(dat);
                                StatusResponsePacket statusResponsePacket = new StatusResponsePacket();

                                Yaml yaml = new Yaml(); // Yaml is not thread-safe so a new instance is needed for every connection
                                Map<String, Object> config = yaml.load(new FileInputStream("server.yml"));
                                statusResponsePacket.jsonResponse = new StatusResponsePacket.JsonResponse((int) config.get("maxPlayers"), 5, (ArrayList<String>) config.get("motd"));
                                String json = gson.toJson(statusResponsePacket.jsonResponse);
                                Logger.log(json);

                                PacketWriter writer = new PacketWriter();
                                writer.writeByte((byte) 0x00);
                                writer.writeString(json);

                                byte[] response = writer.finish();
                                for (byte b : response) System.out.print(Byte.toUnsignedInt(b) + " "); // debug
                                System.out.println(); // new line

                                socket.getOutputStream().write(response);
                                continue;
                            }
                            case LOGIN -> { // Client sent a Login Start packet, respond with a Login Success packet
                                LoginStartPacket loginStartPacket = new LoginStartPacket(dat);

                                PacketWriter writer = new PacketWriter();
                                writer.writeByte((byte) 0x02);

                                UUID uuid = UUID.nameUUIDFromBytes(loginStartPacket.name.getBytes(StandardCharsets.UTF_8));

                                ByteBuffer uuidBuffer = ByteBuffer.allocate(16);
                                uuidBuffer.putLong(uuid.getMostSignificantBits());
                                uuidBuffer.putLong(uuid.getLeastSignificantBits());
                                writer.writeByteArray(uuidBuffer.array());

                                writer.writeString(loginStartPacket.name);

                                byte[] response = writer.finish();
                                for (byte b : response) System.out.print(Byte.toUnsignedInt(b) + " ");
                                // socket.getOutputStream().write(writer.finish());
                                socket.getOutputStream().write(response);
                                continue;
                            }
                        }
                    } else if (dat[0] == 0x01) { // Ping packet, respond with pong
                        switch (state) {
                            case STATUS -> {
                                byte[] payload = new byte[dat.length - 1];
                                System.arraycopy(dat, 1, payload, 0, dat.length - 1);
                                byte[] metadata = new byte[]{(byte) (payload.length + 1), 1};

                                byte[] complete = new byte[payload.length + 2];
                                System.arraycopy(metadata, 0, complete, 0, metadata.length);
                                System.arraycopy(payload, 0, complete, metadata.length, payload.length);

                                socket.getOutputStream().write(complete);
                                Logger.log("Received a ping packet with payload " + Arrays.toString(payload));
                                socket.close();
                            }
                        }
                    } else {
                        Logger.log("Unknown packet received with data " + Arrays.toString(dat));
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Packet did not contain valid packet ID");
                    break;
                }
            } catch (IOException e) {
                break;
            } catch (NegativeArraySizeException e) {
                continue;
            }
        }

        // OutputStream output = socket.getOutputStream();
        socket.close();
    }
}
