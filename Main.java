import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Main extends JFrame {

    private JTextField fruta;
    private JTextField variedad;
    private JTextField temperatura;
    private JTextField humedad;
    private JTextField ph;
    private JTextField lluvia;
    private JTextField calidad;

    private DefaultTableModel modelo;
    private JLabel estado;

    public Main() {
        setTitle("Analizador Estadístico de Calidad de Frutas");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel titulo = new JLabel(
            "ANALIZADOR DE CALIDAD DE FRUTAS",
            SwingConstants.CENTER
        );
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setBorder(BorderFactory.createEmptyBorder(15, 5, 10, 5));
        add(titulo, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
        formulario.setBorder(BorderFactory.createTitledBorder(
            "Datos de la muestra"
        ));

        fruta = agregarCampo(formulario, "Nombre de la fruta:");
        variedad = agregarCampo(formulario, "Variedad:");
        temperatura = agregarCampo(formulario, "Temperatura (°C):");
        humedad = agregarCampo(formulario, "Humedad (%):");
        ph = agregarCampo(formulario, "pH del suelo:");
        lluvia = agregarCampo(formulario, "Lluvia (mm):");
        calidad = agregarCampo(formulario, "Calidad observada (0-100):");

        JButton registrar = new JButton("Registrar muestra");
        JButton limpiar = new JButton("Limpiar campos");
        JButton calcular = new JButton("Calcular promedio");
        JButton eliminar = new JButton("Eliminar seleccionada");

        JPanel botones = new JPanel(new GridLayout(0, 1, 5, 5));
        botones.add(registrar);
        botones.add(limpiar);
        botones.add(calcular);
        botones.add(eliminar);

        JPanel panelIzquierdo = new JPanel(new BorderLayout(5, 10));
        panelIzquierdo.setBorder(
            BorderFactory.createEmptyBorder(5, 10, 5, 5)
        );
        panelIzquierdo.add(formulario, BorderLayout.CENTER);
        panelIzquierdo.add(botones, BorderLayout.SOUTH);

        String[] columnas = {
            "Fruta", "Variedad", "Temperatura",
            "Humedad", "pH", "Lluvia", "Calidad"
        };

        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(25);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder(
            "Muestras registradas"
        ));

        estado = new JLabel("Ingrese los datos de una fruta.");
        estado.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        add(panelIzquierdo, BorderLayout.WEST);
        add(scroll, BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);

        registrar.addActionListener(e -> registrarMuestra());
        limpiar.addActionListener(e -> limpiarCampos());
        calcular.addActionListener(e -> calcularPromedio());
        eliminar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();

            if (fila == -1) {
                mostrarMensaje("Selecciona una muestra de la tabla.");
            } else {
                modelo.removeRow(fila);
                estado.setText("Muestra eliminada.");
            }
        });
    }

    private JTextField agregarCampo(JPanel panel, String etiqueta) {
        panel.add(new JLabel(etiqueta));
        JTextField campo = new JTextField(12);
        panel.add(campo);
        return campo;
    }

    private void registrarMuestra() {
        try {
            String nombre = fruta.getText().trim();
            String tipo = variedad.getText().trim();

            if (nombre.isEmpty() || tipo.isEmpty()) {
                throw new IllegalArgumentException(
                    "Escribe el nombre de la fruta y la variedad."
                );
            }

            double temp = Double.parseDouble(temperatura.getText().trim());
            double hum = Double.parseDouble(humedad.getText().trim());
            double valorPH = Double.parseDouble(ph.getText().trim());
            double agua = Double.parseDouble(lluvia.getText().trim());
            double valorCalidad =
                Double.parseDouble(calidad.getText().trim());

            if (hum < 0 || hum > 100) {
                throw new IllegalArgumentException(
                    "La humedad debe estar entre 0 y 100."
                );
            }

            if (valorPH < 0 || valorPH > 14) {
                throw new IllegalArgumentException(
                    "El pH debe estar entre 0 y 14."
                );
            }

            if (agua < 0) {
                throw new IllegalArgumentException(
                    "La lluvia no puede ser negativa."
                );
            }

            if (valorCalidad < 0 || valorCalidad > 100) {
                throw new IllegalArgumentException(
                    "La calidad debe estar entre 0 y 100."
                );
            }

            modelo.addRow(new Object[] {
                nombre, tipo, temp, hum, valorPH, agua, valorCalidad
            });

            estado.setText(
                "Muestra registrada. Total: " + modelo.getRowCount()
            );

            limpiarCampos();

        } catch (NumberFormatException ex) {
            mostrarMensaje(
                "Revisa los campos numéricos. Debes escribir números válidos."
            );
        } catch (IllegalArgumentException ex) {
            mostrarMensaje(ex.getMessage());
        }
    }

    private void calcularPromedio() {
        int total = modelo.getRowCount();

        if (total == 0) {
            mostrarMensaje("Primero registra al menos una fruta.");
            return;
        }

        double suma = 0;

        for (int i = 0; i < total; i++) {
            suma += Double.parseDouble(
                modelo.getValueAt(i, 6).toString()
            );
        }

        double promedio = suma / total;

        JOptionPane.showMessageDialog(
            this,
            String.format(
                "Muestras registradas: %d%nCalidad promedio: %.2f/100",
                total, promedio
            ),
            "Resultado estadístico",
            JOptionPane.INFORMATION_MESSAGE
        );

        estado.setText(
            String.format("Calidad promedio: %.2f/100", promedio)
        );
    }

    private void limpiarCampos() {
        fruta.setText("");
        variedad.setText("");
        temperatura.setText("");
        humedad.setText("");
        ph.setText("");
        lluvia.setText("");
        calidad.setText("");
        fruta.requestFocus();
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(
            this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}