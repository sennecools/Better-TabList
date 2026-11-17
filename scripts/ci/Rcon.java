import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Minimal, dependency-free Source RCON client used by the CI smoke test.
 *
 * <pre>java scripts/ci/Rcon.java &lt;host&gt; &lt;port&gt; &lt;password&gt; &lt;command...&gt;</pre>
 *
 * The remaining arguments are joined with spaces into a single command. The server's reply is printed
 * with Minecraft formatting codes removed.
 *
 * <p>Exit codes: 0 success, 1 usage error, 2 connection or protocol error, 3 authentication failed.
 */
public final class Rcon {

    private static final int TYPE_RESPONSE_VALUE = 0;
    private static final int TYPE_EXEC_COMMAND = 2;
    private static final int TYPE_AUTH_RESPONSE = 2;
    private static final int TYPE_AUTH = 3;

    private static final int AUTH_ID = 1;
    private static final int COMMAND_ID = 2;
    private static final int END_MARKER_ID = 3;

    private static final int CONNECT_ATTEMPTS = 15;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int MAX_PACKET_LENGTH = 1 << 20;

    private Rcon() {
    }

    private static final class Packet {
        final int id;
        final int type;
        final String body;

        Packet(int id, int type, String body) {
            this.id = id;
            this.type = type;
            this.body = body;
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("usage: java Rcon.java <host> <port> <password> <command...>");
            System.exit(1);
        }
        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("rcon: invalid port '" + args[1] + "'");
            System.exit(1);
            return;
        }
        String password = args[2];
        String command = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
        boolean stopCommand = command.equals("stop") || command.startsWith("stop ");

        try (Socket socket = connect(host, port)) {
            socket.setSoTimeout(READ_TIMEOUT_MS);
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());

            if (!authenticate(in, out, password)) {
                System.err.println("rcon: authentication failed");
                System.exit(3);
            }

            send(out, COMMAND_ID, TYPE_EXEC_COMMAND, command);

            if (stopCommand) {
                // The server may shut down and close the socket before (or instead of) replying.
                try {
                    print(read(in).body);
                } catch (EOFException | SocketException | SocketTimeoutException e) {
                    System.err.println("rcon: connection closed by server");
                }
                return;
            }

            // Wait for the first reply packet before sending anything else: the vanilla server handles
            // exactly one packet per read() and drops the connection when two arrive in the same read.
            StringBuilder reply = new StringBuilder();
            Packet first;
            do {
                first = read(in);
            } while (first.id != COMMAND_ID);
            reply.append(first.body);

            // Replies longer than one packet are split; an unknown request type is answered after the
            // command output, so its echo marks the end of the reply. The server sends every fragment
            // before reading again, so this marker always arrives in a read of its own.
            send(out, END_MARKER_ID, TYPE_RESPONSE_VALUE, "");
            while (true) {
                Packet packet;
                try {
                    packet = read(in);
                } catch (SocketTimeoutException e) {
                    // The command was already answered; keep what arrived if the marker echo never does.
                    break;
                }
                if (packet.id == END_MARKER_ID) {
                    break;
                }
                if (packet.id == COMMAND_ID) {
                    reply.append(packet.body);
                }
            }
            print(reply.toString());
        } catch (IOException e) {
            System.err.println("rcon: " + e);
            System.exit(2);
        }
    }

    private static Socket connect(String host, int port) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= CONNECT_ATTEMPTS; attempt++) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                return socket;
            } catch (ConnectException | SocketTimeoutException e) {
                last = e;
                socket.close();
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last != null ? last : new IOException("could not connect to " + host + ":" + port);
    }

    private static boolean authenticate(DataInputStream in, OutputStream out, String password) throws IOException {
        send(out, AUTH_ID, TYPE_AUTH, password);
        while (true) {
            Packet packet = read(in);
            // Source servers send an empty RESPONSE_VALUE first; Minecraft only sends the AUTH_RESPONSE.
            if (packet.type == TYPE_AUTH_RESPONSE) {
                return packet.id == AUTH_ID;
            }
        }
    }

    private static void send(OutputStream out, int id, int type, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        int length = 4 + 4 + payload.length + 2;
        ByteBuffer buffer = ByteBuffer.allocate(4 + length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(length).putInt(id).putInt(type).put(payload).put((byte) 0).put((byte) 0);
        out.write(buffer.array());
        out.flush();
    }

    private static Packet read(DataInputStream in) throws IOException {
        byte[] header = new byte[4];
        in.readFully(header);
        int length = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (length < 10 || length > MAX_PACKET_LENGTH) {
            throw new IOException("invalid packet length " + length);
        }
        byte[] data = new byte[length];
        in.readFully(data);
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int id = buffer.getInt();
        int type = buffer.getInt();
        int end = length;
        while (end > 8 && data[end - 1] == 0) {
            end--;
        }
        return new Packet(id, type, new String(data, 8, end - 8, StandardCharsets.UTF_8));
    }

    private static void print(String body) {
        String text = body.replaceAll("\u00A7[0-9A-FK-ORXa-fk-orx]", "").trim();
        if (!text.isEmpty()) {
            System.out.println(text);
        }
    }
}
