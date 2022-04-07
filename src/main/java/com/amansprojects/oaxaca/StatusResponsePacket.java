package com.amansprojects.oaxaca;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class StatusResponsePacket implements OutboundPacket {
    public JsonResponse jsonResponse;

    public StatusResponsePacket(int maxPlayers, int onlinePlayers, ArrayList<String> description) {
        jsonResponse = new JsonResponse(maxPlayers, onlinePlayers, description);
    }

    public static class JsonResponse {
        public static class version {
            public String name = "Oaxaca 1.8.8";
            public int protocol = 47;
        }
        public static class players {
            public int max;
            public int online;
            public players(int m, int o) { max = m; online = o; }
        }
        public static class description {
            public String text;
            public description(ArrayList<String> description) {
                text = String.join("\n", description);
            }
        }

        public version version;
        public players players;
        public description description;
        public JsonResponse(int maxPlayers, int onlinePlayers, ArrayList<String> description) {
            version = new version();
            players = new players(maxPlayers, onlinePlayers);
            this.description = new description(description);
        }
    }

    public void send(Socket socket) throws IOException {
        String json = Main.gson.toJson(jsonResponse);

        PacketWriter writer = new PacketWriter();
        writer.writeByte((byte) 0x00);
        writer.writeString(json);

        socket.getOutputStream().write(writer.finish());
    }
}