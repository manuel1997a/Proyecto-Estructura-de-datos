import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/*
 * Incluye:
 * - Vector: arreglo con las calidades observadas.
 * - Matriz: arreglo bidimensional con las variables numericas.
 * - Lista simplemente enlazada: historial principal de muestras.
 * - Lista doblemente enlazada: correlaciones de variables con calidad.
 * - Lista circular: variedades registradas.
 * - Pila: deshacer el ultimo registro.
 * - Cola: procesar muestras pendientes.
 * - Regresion lineal multiple: temperatura, humedad, pH y lluvia.
 */
public class Main extends JFrame {

    // ---------- Modelo de datos ----------
    static class Muestra {
        final int id;
        final String fruta;
        final String variedad;
        final double temperatura;
        final double humedad;
        final double ph;
        final double lluvia;
        final double calidad;

        Muestra(int id, String fruta, String variedad, double temperatura,
                double humedad, double ph, double lluvia, double calidad) {
            this.id = id;
            this.fruta = fruta;
            this.variedad = variedad;
            this.temperatura = temperatura;
            this.humedad = humedad;
            this.ph = ph;
            this.lluvia = lluvia;
            this.calidad = calidad;
        }

        double[] variables() {
            return new double[]{temperatura, humedad, ph, lluvia};
        }
    }

    // ---------- Lista simplemente enlazada de muestras ----------
    static class ListaSimple {
        private static class Nodo {
            Muestra dato;
            Nodo siguiente;
            Nodo(Muestra dato) { this.dato = dato; }
        }

        private Nodo cabeza;
        private Nodo cola;
        private int tamanio;

        void agregar(Muestra m) {
            Nodo nuevo = new Nodo(m);
            if (cabeza == null) {
                cabeza = cola = nuevo;
            } else {
                cola.siguiente = nuevo;
                cola = nuevo;
            }
            tamanio++;
        }

        boolean eliminarPorId(int id) {
            Nodo actual = cabeza;
            Nodo anterior = null;
            while (actual != null) {
                if (actual.dato.id == id) {
                    if (anterior == null) cabeza = actual.siguiente;
                    else anterior.siguiente = actual.siguiente;
                    if (actual == cola) cola = anterior;
                    tamanio--;
                    return true;
                }
                anterior = actual;
                actual = actual.siguiente;
            }
            return false;
        }

        ArrayList<Muestra> obtenerTodos() {
            ArrayList<Muestra> resultado = new ArrayList<>();
            Nodo actual = cabeza;
            while (actual != null) {
                resultado.add(actual.dato);
                actual = actual.siguiente;
            }
            return resultado;
        }

        int tamanio() { return tamanio; }
    }

    // ---------- Lista doblemente enlazada de correlaciones ----------
    static class ListaDoble {
        private static class Nodo {
            String variable;
            double correlacion;
            Nodo anterior, siguiente;
            Nodo(String variable, double correlacion) {
                this.variable = variable;
                this.correlacion = correlacion;
            }
        }

        private Nodo cabeza, cola;
        private int tamanio;

        void limpiar() {
            cabeza = cola = null;
            tamanio = 0;
        }

        void agregar(String variable, double correlacion) {
            Nodo nuevo = new Nodo(variable, correlacion);
            if (cabeza == null) cabeza = cola = nuevo;
            else {
                cola.siguiente = nuevo;
                nuevo.anterior = cola;
                cola = nuevo;
            }
            tamanio++;
        }

        String comoTexto() {
            if (cabeza == null) return "Aún no se han calculado correlaciones.";
            StringBuilder sb = new StringBuilder();
            Nodo actual = cabeza;
            while (actual != null) {
                sb.append(actual.variable).append(": ")
                  .append(String.format("%.4f", actual.correlacion));
                if (actual.siguiente != null) sb.append("\n");
                actual = actual.siguiente;
            }
            return sb.toString();
        }
    }

    // ---------- Lista circular de variedades ----------
    static class ListaCircular {
        private static class Nodo {
            String variedad;
            Nodo siguiente;
            Nodo(String variedad) { this.variedad = variedad; }
        }

        private Nodo ultimo;
        private int tamanio;

        void agregarSiNoExiste(String variedad) {
            if (contiene(variedad)) return;
            Nodo nuevo = new Nodo(variedad);
            if (ultimo == null) {
                ultimo = nuevo;
                nuevo.siguiente = nuevo;
            } else {
                nuevo.siguiente = ultimo.siguiente;
                ultimo.siguiente = nuevo;
                ultimo = nuevo;
            }
            tamanio++;
        }

        boolean contiene(String variedad) {
            if (ultimo == null) return false;
            Nodo actual = ultimo.siguiente;
            do {
                if (actual.variedad.equalsIgnoreCase(variedad)) return true;
                actual = actual.siguiente;
            } while (actual != ultimo.siguiente);
            return false;
        }

        // Se reconstruye desde las muestras vigentes para evitar variedades huérfanas.
        void reconstruir(ArrayList<Muestra> muestras) {
            ultimo = null;
            tamanio = 0;
            for (Muestra m : muestras) agregarSiNoExiste(m.variedad);
        }

        String comoTexto() {
            if (ultimo == null) return "(sin variedades)";
            StringBuilder sb = new StringBuilder();
            Nodo inicio = ultimo.siguiente;
            Nodo actual = inicio;
            do {
                if (sb.length() > 0) sb.append(" -> ");
                sb.append(actual.variedad);
                actual = actual.siguiente;
            } while (actual != inicio);
            sb.append(" -> (inicio)");
            return sb.toString();
        }

        int tamanio() { return tamanio; }
    }

    // ---------- Pila LIFO para deshacer ----------
    static class PilaDeshacer {
        private final Stack<Muestra> pila = new Stack<>();
        void apilar(Muestra m) { pila.push(m); }
        Muestra desapilar() { return pila.empty() ? null : pila.pop(); }
        boolean estaVacia() { return pila.empty(); }
        void limpiar() { pila.clear(); }
    }

    // ---------- Cola FIFO para procesar registros pendientes ----------
    static class ColaMuestras {
        private final Queue<Muestra> cola = new LinkedList<>();
        void encolar(Muestra m) { cola.offer(m); }
        Muestra desencolar() { return cola.poll(); }
        boolean estaVacia() { return cola.isEmpty(); }
        int tamanio() { return cola.size(); }
        void eliminar(Muestra m) { cola.remove(m); }
    }

    // ---------- Componentes de interfaz ----------
    private final JTextField campoFruta = new JTextField();
    private final JTextField campoVariedad = new JTextField();
    private final JTextField campoTemperatura = new JTextField();
    private final JTextField campoHumedad = new JTextField();
    private final JTextField campoPH = new JTextField();
    private final JTextField campoLluvia = new JTextField();
    private final JTextField campoCalidad = new JTextField();

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
        new Object[]{"ID", "Fruta", "Variedad", "Temp. °C", "Humedad %",
                    "pH", "Lluvia mm", "Calidad"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tabla = new JTable(modeloTabla);
    private final JLabel estado = new JLabel("Ingrese una muestra para comenzar.");

    // ---------- Estructuras del proyecto ----------
    private final ListaSimple listaMuestras = new ListaSimple();
    private final ListaDoble listaCorrelaciones = new ListaDoble();
    private final ListaCircular listaVariedades = new ListaCircular();
    private final PilaDeshacer pilaDeshacer = new PilaDeshacer();
    private final ColaMuestras colaPendientes = new ColaMuestras();

    // Vector y matriz se reconstruyen a partir de las muestras vigentes.
    private double[] vectorCalidades = new double[0];
    // Columnas: temperatura, humedad, pH, lluvia, calidad.
    private double[][] matrizDatos = new double[0][5];

    private int siguienteId = 1;

    public Main() {
        setTitle("Analizador Estadístico de Calidad de Frutas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
        setSize(1150, 720);
        setLocationRelativeTo(null);

        JLabel titulo = new JLabel("ANALIZADOR ESTADÍSTICO DE CALIDAD DE FRUTAS",
                                   SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 21));
        titulo.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        add(titulo, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridLayout(0, 2, 7, 7));
        formulario.setBorder(BorderFactory.createTitledBorder("Datos de la muestra"));
        agregarCampo(formulario, "Fruta:", campoFruta);
        agregarCampo(formulario, "Variedad:", campoVariedad);
        agregarCampo(formulario, "Temperatura (°C):", campoTemperatura);
        agregarCampo(formulario, "Humedad (0-100 %):", campoHumedad);
        agregarCampo(formulario, "pH del suelo (0-14):", campoPH);
        agregarCampo(formulario, "Lluvia (mm):", campoLluvia);
        agregarCampo(formulario, "Calidad observada (0-100):", campoCalidad);

        JButton registrar = new JButton("Registrar muestra");
        JButton calcular = new JButton("Calcular regresión");
        JButton procesar = new JButton("Procesar cola");
        JButton deshacer = new JButton("Deshacer último registro");
        JButton verEstructuras = new JButton("Ver estructuras");
        JButton limpiar = new JButton("Limpiar campos");

        JPanel botones = new JPanel(new GridLayout(0, 1, 6, 6));
        botones.add(registrar);
        botones.add(calcular);
        botones.add(procesar);
        botones.add(deshacer);
        botones.add(verEstructuras);
        botones.add(limpiar);

        JPanel izquierda = new JPanel(new BorderLayout(8, 8));
        izquierda.setPreferredSize(new Dimension(330, 400));
        izquierda.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 8));
        izquierda.add(formulario, BorderLayout.CENTER);
        izquierda.add(botones, BorderLayout.SOUTH);

        tabla.setRowHeight(24);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane tablaScroll = new JScrollPane(tabla);
        tablaScroll.setBorder(BorderFactory.createTitledBorder("Muestras registradas"));

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(tablaScroll, BorderLayout.CENTER);
        estado.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        centro.add(estado, BorderLayout.SOUTH);

        add(izquierda, BorderLayout.WEST);
        add(centro, BorderLayout.CENTER);

        registrar.addActionListener(e -> registrarMuestra());
        calcular.addActionListener(e -> calcularRegresion());
        procesar.addActionListener(e -> procesarCola());
        deshacer.addActionListener(e -> deshacerUltimo());
        verEstructuras.addActionListener(e -> mostrarEstructuras());
        limpiar.addActionListener(e -> limpiarCampos());
    }

    private void agregarCampo(JPanel panel, String texto, JTextField campo) {
        panel.add(new JLabel(texto));
        panel.add(campo);
    }

    private void registrarMuestra() {
        try {
            String fruta = campoFruta.getText().trim();
            String variedad = campoVariedad.getText().trim();
            if (fruta.isEmpty() || variedad.isEmpty()) {
                throw new IllegalArgumentException("Escriba el nombre de la fruta y su variedad.");
            }

            double temp = leerNumero(campoTemperatura, "temperatura");
            double hum = leerNumero(campoHumedad, "humedad");
            double ph = leerNumero(campoPH, "pH");
            double lluvia = leerNumero(campoLluvia, "lluvia");
            double calidad = leerNumero(campoCalidad, "calidad");

            if (hum < 0 || hum > 100)
                throw new IllegalArgumentException("La humedad debe estar entre 0 y 100.");
            if (ph < 0 || ph > 14)
                throw new IllegalArgumentException("El pH debe estar entre 0 y 14.");
            if (lluvia < 0)
                throw new IllegalArgumentException("La lluvia no puede ser negativa.");
            if (calidad < 0 || calidad > 100)
                throw new IllegalArgumentException("La calidad debe estar entre 0 y 100.");

            Muestra m = new Muestra(siguienteId++, fruta, variedad, temp, hum, ph, lluvia, calidad);

            listaMuestras.agregar(m);
            pilaDeshacer.apilar(m);
            colaPendientes.encolar(m);
            listaVariedades.agregarSiNoExiste(variedad);

            actualizarTablaYDatos();
            estado.setText("Muestra #" + m.id + " registrada. Pendientes en cola: "
                            + colaPendientes.tamanio() + ".");
            limpiarCampos();
        } catch (NumberFormatException ex) {
            mostrarError("En los campos numéricos, escriba números válidos. Puede usar punto decimal.");
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private double leerNumero(JTextField campo, String nombre) {
        String texto = campo.getText().trim().replace(',', '.');
        if (texto.isEmpty()) throw new NumberFormatException("Campo vacío: " + nombre);
        return Double.parseDouble(texto);
    }

    private void actualizarTablaYDatos() {
        modeloTabla.setRowCount(0);
        ArrayList<Muestra> muestras = listaMuestras.obtenerTodos();

        vectorCalidades = new double[muestras.size()];
        matrizDatos = new double[muestras.size()][5];

        for (int i = 0; i < muestras.size(); i++) {
            Muestra m = muestras.get(i);
            modeloTabla.addRow(new Object[]{m.id, m.fruta, m.variedad,
                m.temperatura, m.humedad, m.ph, m.lluvia, m.calidad});

            vectorCalidades[i] = m.calidad;
            matrizDatos[i][0] = m.temperatura;
            matrizDatos[i][1] = m.humedad;
            matrizDatos[i][2] = m.ph;
            matrizDatos[i][3] = m.lluvia;
            matrizDatos[i][4] = m.calidad;
        }

        listaVariedades.reconstruir(muestras);
    }

    private void calcularRegresion() {
        ArrayList<Muestra> muestras = listaMuestras.obtenerTodos();
        if (muestras.size() < 5) {
            mostrarError("Registre al menos 5 muestras para calcular la regresión con 4 variables predictoras.");
            return;
        }

        try {
            // Modelo: calidad = b0 + b1*temperatura + b2*humedad + b3*pH + b4*lluvia
            double[] beta = ajustarRegresion(matrizDatos);
            actualizarCorrelaciones(matrizDatos);

            StringBuilder resultado = new StringBuilder();
            resultado.append("MODELO DE REGRESIÓN LINEAL MÚLTIPLE\n\n");
            resultado.append(String.format("Intercepto (b0): %.6f%n", beta[0]));
            resultado.append(String.format("Temperatura (b1): %.6f%n", beta[1]));
            resultado.append(String.format("Humedad (b2): %.6f%n", beta[2]));
            resultado.append(String.format("pH (b3): %.6f%n", beta[3]));
            resultado.append(String.format("Lluvia (b4): %.6f%n%n", beta[4]));
            resultado.append("Ecuación:\nCalidad = ")
                .append(String.format("%.4f", beta[0])).append(" + ")
                .append(String.format("%.4f", beta[1])).append("*Temperatura + ")
                .append(String.format("%.4f", beta[2])).append("*Humedad + ")
                .append(String.format("%.4f", beta[3])).append("*pH + ")
                .append(String.format("%.4f", beta[4])).append("*Lluvia\n\n");

            resultado.append("CALIDAD ESTIMADA PARA CADA MUESTRA\n");
            for (Muestra m : muestras) {
                double prediccion = beta[0] + beta[1] * m.temperatura
                    + beta[2] * m.humedad + beta[3] * m.ph + beta[4] * m.lluvia;
                resultado.append(String.format("#%d %s (%s): %.2f/100%n",
                    m.id, m.fruta, m.variedad, prediccion));
            }
            resultado.append("\nCORRELACIÓN DE PEARSON CON CALIDAD\n");
            resultado.append(listaCorrelaciones.comoTexto());
            resultado.append("\n\nNota: las predicciones dependen de la calidad y cantidad de los datos registrados.");

            JTextArea area = new JTextArea(resultado.toString(), 25, 55);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            area.setCaretPosition(0);
            JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Resultados del análisis", JOptionPane.INFORMATION_MESSAGE);
            estado.setText("Regresión calculada con " + muestras.size() + " muestras.");
        } catch (ArithmeticException ex) {
            mostrarError("No se pudo calcular la regresión. Los datos pueden producir una matriz singular. Ingrese muestras variadas.");
        }
    }

    // Resuelve las ecuaciones normales (X'X)b = X'y con eliminación Gauss-Jordan.
    // datos: temperatura, humedad, pH, lluvia, calidad.
    private double[] ajustarRegresion(double[][] datos) {
        int n = datos.length;
        int p = 5; // intercepto + cuatro predictores
        double[][] aumentado = new double[p][p + 1];

        for (int i = 0; i < n; i++) {
            double[] x = {1.0, datos[i][0], datos[i][1], datos[i][2], datos[i][3]};
            double y = datos[i][4];
            for (int fila = 0; fila < p; fila++) {
                for (int col = 0; col < p; col++) {
                    aumentado[fila][col] += x[fila] * x[col];
                }
                aumentado[fila][p] += x[fila] * y;
            }
        }

        for (int pivote = 0; pivote < p; pivote++) {
            int mejor = pivote;
            for (int fila = pivote + 1; fila < p; fila++) {
                if (Math.abs(aumentado[fila][pivote]) > Math.abs(aumentado[mejor][pivote])) {
                    mejor = fila;
                }
            }

            if (Math.abs(aumentado[mejor][pivote]) < 1e-10) {
                throw new ArithmeticException("Matriz singular");
            }

            double[] temporal = aumentado[pivote];
            aumentado[pivote] = aumentado[mejor];
            aumentado[mejor] = temporal;

            double divisor = aumentado[pivote][pivote];
            for (int col = pivote; col <= p; col++) {
                aumentado[pivote][col] /= divisor;
            }

            for (int fila = 0; fila < p; fila++) {
                if (fila == pivote) continue;
                double factor = aumentado[fila][pivote];
                for (int col = pivote; col <= p; col++) {
                    aumentado[fila][col] -= factor * aumentado[pivote][col];
                }
            }
        }

        double[] beta = new double[p];
        for (int i = 0; i < p; i++) beta[i] = aumentado[i][p];
        return beta;
    }

    private void actualizarCorrelaciones(double[][] datos) {
        listaCorrelaciones.limpiar();
        String[] nombres = {"Temperatura", "Humedad", "pH", "Lluvia"};
        for (int variable = 0; variable < 4; variable++) {
            double[] x = new double[datos.length];
            double[] y = new double[datos.length];
            for (int i = 0; i < datos.length; i++) {
                x[i] = datos[i][variable];
                y[i] = datos[i][4];
            }
            listaCorrelaciones.agregar(nombres[variable], correlacionPearson(x, y));
        }
    }

    private double correlacionPearson(double[] x, double[] y) {
        double mediaX = 0, mediaY = 0;
        for (int i = 0; i < x.length; i++) {
            mediaX += x[i];
            mediaY += y[i];
        }
        mediaX /= x.length;
        mediaY /= y.length;

        double numerador = 0, sumaX = 0, sumaY = 0;
        for (int i = 0; i < x.length; i++) {
            double dx = x[i] - mediaX;
            double dy = y[i] - mediaY;
            numerador += dx * dy;
            sumaX += dx * dx;
            sumaY += dy * dy;
        }
        double denominador = Math.sqrt(sumaX * sumaY);
        return denominador == 0 ? 0 : numerador / denominador;
    }

    private void procesarCola() {
        Muestra m = colaPendientes.desencolar();
        if (m == null) {
            mostrarError("No hay muestras pendientes por procesar.");
            return;
        }
        estado.setText("Procesada la muestra #" + m.id + " (" + m.fruta
                        + "). Pendientes: " + colaPendientes.tamanio() + ".");
        JOptionPane.showMessageDialog(this,
            "Se procesó la muestra:\n" + m.fruta + " - " + m.variedad
            + "\nCalidad observada: " + m.calidad
            + "\nPendientes en cola: " + colaPendientes.tamanio(),
            "Procesamiento de cola", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deshacerUltimo() {
        Muestra ultima = pilaDeshacer.desapilar();
        if (ultima == null) {
            mostrarError("No hay registros para deshacer.");
            return;
        }

        listaMuestras.eliminarPorId(ultima.id);
        colaPendientes.eliminar(ultima);
        actualizarTablaYDatos();
        listaCorrelaciones.limpiar();

        estado.setText("Se deshizo el registro #" + ultima.id
                       + ". Muestras actuales: " + listaMuestras.tamanio() + ".");
    }

    private void mostrarEstructuras() {
        String texto = "RESUMEN DE ESTRUCTURAS DE DATOS\n\n"
            + "Vector de calidades: " + vectorCalidades.length + " elementos\n"
            + "Matriz de datos: " + matrizDatos.length + " filas x 5 columnas\n"
            + "Lista simplemente enlazada: " + listaMuestras.tamanio() + " muestras\n"
            + "Lista doblemente enlazada (correlaciones):\n"
            + listaCorrelaciones.comoTexto() + "\n\n"
            + "Lista circular de variedades (" + listaVariedades.tamanio() + "):\n"
            + listaVariedades.comoTexto() + "\n\n"
            + "Pila de deshacer: " + (pilaDeshacer.estaVacia() ? "vacía" : "contiene registros") + "\n"
            + "Cola de pendientes: " + colaPendientes.tamanio() + " muestras";

        JTextArea area = new JTextArea(texto, 22, 48);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
            "Estructuras de datos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void limpiarCampos() {
        campoFruta.setText("");
        campoVariedad.setText("");
        campoTemperatura.setText("");
        campoHumedad.setText("");
        campoPH.setText("");
        campoLluvia.setText("");
        campoCalidad.setText("");
        campoFruta.requestFocus();
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso",
                                      JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
