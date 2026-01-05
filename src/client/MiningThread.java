package client;

import utils.HashCalculator;

public class MiningThread extends Thread {
    private String datos;
    private int min;
    private int max;
    private MinerClient cliente;
    private int dificultad;

    public MiningThread(String datos, int min, int max, MinerClient cliente, int dificultad) {
        this.datos = datos;
        this.min = min;
        this.max = max;
        this.cliente = cliente;
        this.dificultad = dificultad;
    }

    @Override
    public void run() {
        try {
            System.out.println("Hilo de minado iniciado para rango [" + min + "-" + max + "]");

            String ceros = "0".repeat(dificultad);

            for (int i = min; i <= max && !cliente.debeParar(); i++) {
                String hash = HashCalculator.getHash(datos, i);

                if (hash.startsWith(ceros)) {
                    cliente.notificarSolucionEncontrada();

                    System.out.println("\nSOLUCION ENCONTRADA: " + i + " (Thread: " + Thread.currentThread().getName() + ")");
                    System.out.println("Hash: " + hash);
                    cliente.enviarSolucion(i);
                    return;
                }

                if ((i - min) % 100 == 0) {
                    int progreso = (int) (((i - min + 1) * 100.0) / (max - min + 1));
                    System.out.println("[" + Thread.currentThread().getName() + "] Progreso: " + progreso + "%");
                }
            }

            if (cliente.debeParar()) {
                System.out.println("Minado detenido");
            } else {
                System.out.println("Minado completado. No se encontro solucion en el rango");
            }

        } catch (Exception e) {
            System.err.println("Error durante el minado: " + e.getMessage());
        }
    }
}
