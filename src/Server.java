import javax.management.remote.JMXConnectionNotification;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            spegni();
        }
    }

    public void broadcast(String message) {
        for(ConnectionHandler ch : connections) {
            if(ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void spegni() {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for(ConnectionHandler ch : connections) {
                ch.spegni();
            }
        } catch (IOException e) {
            // ignora
        }
    }


    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                out.println("inserisci nickname: ");
                nickname = in.readLine();

                System.out.println(nickname + " si connette");

                broadcast(nickname + " si unisce alla chat");

                String message;
                while((message = in.readLine()) != null) {
                    if(message.startsWith("/nick")) {
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length == 2) {
                            broadcast(nickname + " si rinomina con " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("nickname rinominato con successo");
                        } else {
                            out.println("nessun nickname rilevato");
                        }
                    } else if(message.startsWith("/quit")) {
                        broadcast(nickname + " ha abbandonato la chat");
                        System.out.println(nickname + " si disconnette");
                        spegni();
                    } else {
                        broadcast(nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
                spegni();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void spegni() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignora
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
