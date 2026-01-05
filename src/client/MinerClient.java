package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MinerClient {
    private String clienteId;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean pararMinado = false;
    private MiningThread hiloMinado;
    private volatile boolean solucionEncontrada = false;
    private List<MiningThread> threadsConcurrentes;

    public MinerClient(String clienteId) {
        this.clienteId = clienteId;
    }

    public void conectar(String host, int port) throws IOException {
        System.out.println("Conectando a " + host + ":" + port);

        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        out.println("connect:" + clienteId);

        new Thread(this::escucharServidor).start();

        System.out.println("Conectado al mining pool\n");
    }

    private void escucharServidor() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                // ACK conexion
                if (msg.startsWith("ack:")) {
                    int totalClientes = Integer.parseInt(msg.split(":")[1]);
                    System.out.println("ACK de conexion. Total clientes: " + totalClientes);
                }

                // Nueva peticion de minado
                if (msg.startsWith("new_request:")) {
                    String[] partes = msg.split(":");
                    String[] rango = partes[1].split("-");
                    int min = Integer.parseInt(rango[0]);
                    int max = Integer.parseInt(rango[1]);
                    String datos = partes[2];
                    int dificultad = Integer.parseInt(partes[3]);

                    System.out.println("\n========================================");
                    System.out.println("NUEVA PETICION DE MINADO");
                    System.out.println("Rango: [" + min + "-" + max + "]");
                    System.out.println("Dificultad: " + dificultad + " ceros");
                    System.out.println("Datos: " + datos);
                    System.out.println("========================================\n");

                    out.println("ack");

                    // Parar threads anteriores si existen
                    pararMinado = true;
                    solucionEncontrada = false;

                    if (threadsConcurrentes != null) {
                        for (MiningThread t : threadsConcurrentes) {
                            try {
                                t.join(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    pararMinado = false;

                    // Crear búsqueda concurrente con 5 threads
                    empezarMinadoConcurrente(datos, min, max, dificultad);
                }

                // Fin del minado
                if (msg.startsWith("end:")) {
                    String[] partes = msg.split(":");
                    String ganador = partes[1];
                    int solucion = Integer.parseInt(partes[2]);

                    System.out.println("\n========================================");
                    System.out.println("MINADO COMPLETO");
                    System.out.println(ganador.equals(clienteId) ? "HAS GANADO!" : "Ganador: " + ganador);
                    System.out.println("Solucion: " + solucion);
                    System.out.println("========================================\n");

                    pararMinado = true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error de conexion: " + e.getMessage());
        }
    }

    private void empezarMinadoConcurrente(String datos, int min, int max, int dificultad) {
        int NUM_THREADS = 5;
        int rangoTotal = max - min + 1;
        int rangoPorThread = rangoTotal / NUM_THREADS;

        threadsConcurrentes = new ArrayList<>();

        System.out.println("Iniciando minado concurrente con " + NUM_THREADS + " threads");

        for (int i = 0; i < NUM_THREADS; i++) {
            int threadMin = min + (i * rangoPorThread);
            int threadMax;

            if (i == NUM_THREADS - 1) {
                threadMax = max;  // El último thread toma todo lo que queda
            } else {
                threadMax = threadMin + rangoPorThread - 1;
            }

            MiningThread thread = new MiningThread(datos, threadMin, threadMax, this, dificultad);
            threadsConcurrentes.add(thread);
            thread.start();

            System.out.println("  Thread " + (i + 1) + " minando [" + threadMin + "-" + threadMax + "]");
        }
    }

    public void enviarSolucion(int solucion) {
        System.out.println("Enviando solucion: " + solucion);
        out.println("sol:" + solucion);
    }

    public boolean debeParar() {
        return pararMinado || solucionEncontrada;
    }

    public synchronized void notificarSolucionEncontrada() {
        solucionEncontrada = true;
    }

    public void desconectar() {
        pararMinado = true;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando conexion");
        }
    }
}
