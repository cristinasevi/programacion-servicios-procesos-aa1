package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket cliente;
    private MiningPoolServer servidor;
    private BufferedReader in;
    private PrintWriter out;
    private String clienteId;

    public ClientHandler(Socket cliente, MiningPoolServer servidor) {
        this.cliente = cliente;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            out = new PrintWriter(cliente.getOutputStream(), true);

            String msg;
            while ((msg = in.readLine()) != null) {
                // Conexion
                if (msg.startsWith("connect:")) {
                    clienteId = msg.split(":")[1];
                    servidor.agregarCliente(this);
                    out.println("ack:" + servidor.getTotalClientes());
                    System.out.println("Cliente conectado: " + clienteId);
                }

                // Confirmacion
                if (msg.equals("ack")) {
                    System.out.println("Cliente " + clienteId + " ack");
                }

                // Solucion
                if (msg.startsWith("sol:")) {
                    int solucion = Integer.parseInt(msg.split(":")[1]);
                    System.out.println("Cliente " + clienteId + " envio solucion: " + solucion);
                    servidor.procesarSolucion(clienteId, solucion);
                }
            }

        } catch (IOException e) {
            System.err.println(e);
        } finally {
            servidor.quitarCliente(this);
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (cliente != null) cliente.close();
            } catch (IOException e) {
                System.err.println("Error cerrando conexion");
            }
            System.out.println("Cliente desconectado: " + clienteId);
        }
    }

    public void enviarPeticionMinado(int min, int max, String datos) {
        out.println("new_request:" + min + "-" + max + ":" + datos);
        System.out.println("Enviado a " + clienteId + ": [" + min + "-" + max + "]");
    }

    public void enviarFin(String ganador, int solucion) {
        out.println("end:" + ganador + ":" + solucion);
    }
}
