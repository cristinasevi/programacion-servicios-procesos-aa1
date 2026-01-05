package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MiningPoolServer {
    private List<ClientHandler> clientes;
    private String datosActuales;
    private int contadorBloques = 0;
    private boolean minandoEnProgreso = false;
    private int solucionActual = -1;
    private int dificultad = 2;

    public MiningPoolServer() {
        this.clientes = new ArrayList<>();
    }

    public void start(int port) throws IOException {
        System.out.println("MiningPoolServer start on port " + port);
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket cliente = serverSocket.accept();
            System.out.println("MiningPoolServer accept");

            ClientHandler handler = new ClientHandler(cliente, this);
            handler.start();
        }
    }

    public synchronized void agregarCliente(ClientHandler cliente) {
        clientes.add(cliente);
        System.out.println("Cliente agregado. Total: " + clientes.size());

        if (clientes.size() == 1 && !minandoEnProgreso) {
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    nuevoBloque();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public synchronized void quitarCliente(ClientHandler cliente) {
        clientes.remove(cliente);
        System.out.println("Cliente quitado. Total: " + clientes.size());
    }

    public synchronized int getTotalClientes() {
        return clientes.size();
    }

    public synchronized void nuevoBloque() {
        if (clientes.isEmpty()) {
            System.out.println("No hay clientes conectados");
            return;
        }

        minandoEnProgreso = true;
        contadorBloques++;
        solucionActual = -1;

        // Generar datos aleatorios
        Random rand = new Random();
        String[] cuentas = {"ACC001", "ACC002", "ACC003", "ACC004", "ACC005"};
        datosActuales = "";

        for (int i = 0; i < 3; i++) {
            String origen = cuentas[rand.nextInt(cuentas.length)];
            String destino;
            do {
                destino = cuentas[rand.nextInt(cuentas.length)];
            } while (destino.equals(origen));

            double cantidad = Math.round(rand.nextDouble() * 10000 * 100.0) / 100.0;
            datosActuales += origen + "|" + destino + "|" + cantidad + ";";
        }

        System.out.println("\n========================================");
        System.out.println("NUEVO BLOQUE #" + contadorBloques);
        System.out.println("========================================");
        System.out.println("Datos: " + datosActuales);
        System.out.println("========================================\n");

        // Distribuir trabajo
        int rangoTotal = 1000;
        int numClientes = clientes.size();
        int rangoPorCliente = rangoTotal / numClientes;

        System.out.println("Distribuyendo trabajo entre " + numClientes + " clientes");

        for (int i = 0; i < clientes.size(); i++) {
            ClientHandler cliente = clientes.get(i);
            int min = i * rangoPorCliente;
            int max = (i == clientes.size() - 1) ? rangoTotal - 1 : (i + 1) * rangoPorCliente - 1;

            cliente.enviarPeticionMinado(min, max, datosActuales, dificultad);
        }
    }

    public synchronized void procesarSolucion(String clienteId, int solucion) {
        if (solucionActual != -1) {
            System.out.println("Solucion rechazada: bloque ya resuelto");
            return;
        }

        try {
            boolean esValida = utils.HashCalculator.validate(datosActuales, solucion, dificultad);

            if (esValida) {
                solucionActual = solucion;
                String hash = utils.HashCalculator.getHash(datosActuales, solucion);

                System.out.println("\n========================================");
                System.out.println("SOLUCION VALIDA ENCONTRADA!");
                System.out.println("========================================");
                System.out.println("Bloque #" + contadorBloques);
                System.out.println("Ganador: " + clienteId);
                System.out.println("Solucion: " + solucion);
                System.out.println("Hash: " + hash);
                System.out.println("========================================\n");

                // Notificar a todos
                for (ClientHandler cliente : clientes) {
                    cliente.enviarFin(clienteId, solucion);
                }

                minandoEnProgreso = false;

                new Thread(() -> {
                    try {
                        Thread.sleep(300000);  // 5 minutos
                        nuevoBloque();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.out.println("Solucion invalida de " + clienteId + ": " + solucion);
            }
        } catch (Exception e) {
            System.err.println("Error validando solucion: " + e.getMessage());
        }
    }

    public int getDificultad() {
        return dificultad;
    }
}
