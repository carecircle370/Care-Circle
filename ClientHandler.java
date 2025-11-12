import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable{

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader br;
    private BufferedWriter bw;

    private String clientUsername;
    private String clientPassword;

    public ClientHandler(Socket socket){
        try{

            this.socket = socket;
            this.bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = br.readLine();
            clientHandlers.add(this);
            //broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");

        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
    }



    @Override
    public void run() {

        //broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");
        String messageFromClient;

        while(socket.isConnected()){
            try{
                messageFromClient = br.readLine();
                if (messageFromClient == null) {
                    closeEverything(socket, br, bw);
                    break;
                }
                dispatchMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket, br, bw);
                break;
            }
        }
    }

    public void broadcastMessage(String opcode, String sender, String data) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.send(opcode, sender, data);
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER", "SERVER", clientUsername + " has disconnected");
    }

    public void send(String opcode, String sender, String data) {
        try {
            String message = opcode + "|" + sender + "|" + data;
            bw.write(message);
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
    }

    private void dispatchMessage(String message) { // OPCODE|SENDER|DATA
        String[] parts = message.split("\\|", 3);
        if (parts.length < 3) return;

        String opcode = parts[0];
        String sender = parts[1];
        String data = parts[2];

        switch (opcode) {
            case "CHAT":
                broadcastMessage("CHAT", sender, data);
                break;

            case "LOGIN":
                System.out.println(sender + " logged in.");
                break;


            default:
                System.out.println("Unknown opcode: " + opcode);
        }
    }

    public void closeEverything(Socket socket, BufferedReader br, BufferedWriter bw){
        removeClientHandler();
        try{
            if(br != null){
                br.close();
            }
            if(bw != null){
                bw.close();
            }
            if(socket != null){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
