package client;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Introduce tu ID de minero: ");
        String clienteId = scanner.nextLine();

        MinerClient cliente = new MinerClient(clienteId);
        cliente.conectar("localhost", 3000);

        System.out.println("\nPresiona ENTER para desconectar...");
        scanner.nextLine();

        cliente.desconectar();
    }
}
