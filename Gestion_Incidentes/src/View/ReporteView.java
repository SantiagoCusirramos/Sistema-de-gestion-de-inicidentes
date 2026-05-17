package View;

import Controller.IncidenteController;
import Model.Incidente;
import Model.Usuario;
import Service.ReporteService;
import enums.EstadoIncidente;
import enums.Prioridad;
import enums.RolUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Correcciones aplicadas:
 *  [BUG-012] crearGraficos() usa métodos de ReporteService con usuarioId + rol.
 *  [BUG-012] crearTablaRecientes() usa getIncidentesRecientes(limite, id, rol).
 *  [FIX-REPORTE] Los datos ya no se cargan una sola vez al construir la vista.
 *               Ahora el contenido se construye con referencias a componentes
 *               mutables y un botón "Actualizar" llama a refrescarDatos() para
 *               repintar tarjetas, gráficos y tabla sin necesidad de re-login.
 */
public class ReporteView {

    private ReporteService reporteService;
    private IncidenteController incidenteController;
    private Usuario usuarioActual;

    // --- Componentes mutables del resumen ---
    private Label lblTotal;
    private Label lblPendientes;
    private Label lblEnProceso;
    private Label lblResueltos;
    private Label lblCerrados;

    // --- Componentes mutables de gráficos ---
    private PieChart pieChart;
    private BarChart<String, Number> barChart;

    // --- Tabla de recientes ---
    private TableView<Incidente> tablaRecientes;
    private ObservableList<Incidente> datosRecientes;

    public ReporteView(Usuario usuarioActual) {
        this.reporteService      = new ReporteService();
        this.incidenteController = new IncidenteController(usuarioActual);
        this.usuarioActual       = usuarioActual;
    }

    public Node getContenido() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // --- Encabezado ---
        Label lblTitulo = new Label("Reportes y Estadisticas");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label lblAlcance = new Label(obtenerTextoAlcance());
        lblAlcance.setStyle("-fx-text-fill: #555555; -fx-font-style: italic;");

        // --- Botón Actualizar ---
        Button btnActualizar = new Button("↻  Actualizar");
        btnActualizar.setStyle(
            "-fx-background-color: #1565c0; -fx-text-fill: white; " +
            "-fx-font-size: 13px; -fx-padding: 7 18; -fx-background-radius: 4; -fx-cursor: hand;");
        btnActualizar.setOnAction(e -> refrescarDatos());

        HBox barraTop = new HBox(12, lblAlcance, btnActualizar);
        barraTop.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(
            lblTitulo,
            barraTop,
            construirResumenGeneral(),   // inicializa las Labels mutables
            construirGraficos(),          // inicializa pieChart y barChart
            construirTablaRecientes()     // inicializa tablaRecientes y datosRecientes
        );

        // Carga inicial de datos
        refrescarDatos();

        return root;
    }

    // -------------------------------------------------------------------------
    // Refresco completo de datos (sin recrear los nodos)
    // -------------------------------------------------------------------------

    /**
     * Actualiza TODOS los datos de la vista en su lugar:
     * tarjetas de resumen, gráficos y tabla de recientes.
     * Se llama en la carga inicial y cada vez que el usuario pulsa "Actualizar".
     */
    private void refrescarDatos() {
        refrescarResumen();
        refrescarGraficos();
        refrescarTablaRecientes();
    }

    private void refrescarResumen() {
        int total       = incidenteController.getTotalIncidentes();
        long pendientes = incidenteController.getIncidentesPorEstado(EstadoIncidente.PENDIENTE);
        long enProceso  = incidenteController.getIncidentesPorEstado(EstadoIncidente.EN_PROCESO);
        long resueltos  = incidenteController.getIncidentesPorEstado(EstadoIncidente.RESUELTO);
        long cerrados   = incidenteController.getIncidentesPorEstado(EstadoIncidente.CERRADO);

        lblTotal.setText(String.valueOf(total));
        lblPendientes.setText(String.valueOf(pendientes));
        lblEnProceso.setText(String.valueOf(enProceso));
        lblResueltos.setText(String.valueOf(resueltos));
        lblCerrados.setText(String.valueOf(cerrados));
    }

    private void refrescarGraficos() {
        // --- Pie chart ---
        pieChart.getData().clear();
        Map<Prioridad, Integer> stats = reporteService.getIncidentesPorPrioridad(
            usuarioActual.getId(), usuarioActual.getRol());

        boolean hayDatos = false;
        for (Map.Entry<Prioridad, Integer> entry : stats.entrySet()) {
            if (entry.getValue() > 0) {
                pieChart.getData().add(new PieChart.Data(
                    entry.getKey().name() + " (" + entry.getValue() + ")",
                    entry.getValue()));
                hayDatos = true;
            }
        }
        if (!hayDatos) {
            pieChart.getData().add(new PieChart.Data("Sin datos", 1));
        }

        // --- Bar chart ---
        barChart.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        List<Incidente> todos = incidenteController.listarIncidentes();
        for (EstadoIncidente estado : EstadoIncidente.values()) {
            long count = todos.stream().filter(i -> i.getEstado() == estado).count();
            serie.getData().add(new XYChart.Data<>(estado.name(), count));
        }
        barChart.getData().add(serie);
    }

    private void refrescarTablaRecientes() {
        List<Incidente> recientes = reporteService.getIncidentesRecientes(
            15, usuarioActual.getId(), usuarioActual.getRol());
        datosRecientes.setAll(recientes);
    }

    // -------------------------------------------------------------------------
    // Construcción inicial de nodos (solo estructura, sin datos reales aún)
    // -------------------------------------------------------------------------

    private String obtenerTextoAlcance() {
        return usuarioActual.getRol() == RolUsuario.ADMIN
            ? "Mostrando estadísticas globales del sistema (todos los incidentes)"
            : "Mostrando estadísticas de tus incidentes asignados";
    }

    /** Crea las tarjetas de resumen e inicializa las Labels mutables. */
    private HBox construirResumenGeneral() {
        lblTotal      = crearLabelValor();
        lblPendientes = crearLabelValor();
        lblEnProceso  = crearLabelValor();
        lblResueltos  = crearLabelValor();
        lblCerrados   = crearLabelValor();

        VBox cardTotal      = crearCard("Total",      lblTotal,      "#1a237e");
        VBox cardPendientes = crearCard("Pendientes", lblPendientes, "#e65100");
        VBox cardProceso    = crearCard("En Proceso", lblEnProceso,  "#1565c0");
        VBox cardResueltos  = crearCard("Resueltos",  lblResueltos,  "#2e7d32");
        VBox cardCerrados   = crearCard("Cerrados",   lblCerrados,   "#6a1b9a");

        HBox cards = new HBox(12,
            cardTotal, cardPendientes, cardProceso, cardResueltos, cardCerrados);
        cards.setPadding(new Insets(10, 0, 10, 0));
        return cards;
    }

    private Label crearLabelValor() {
        Label lbl = new Label("0");
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lbl.setStyle("-fx-text-fill: white;");
        return lbl;
    }

    private VBox crearCard(String titulo, Label lblValor, String color) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        lblTitulo.setStyle("-fx-text-fill: white;");

        VBox card = new VBox(5, lblTitulo, lblValor);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");
        card.setPrefWidth(160);
        return card;
    }

    /** Crea los gráficos vacíos e inicializa pieChart y barChart. */
    private HBox construirGraficos() {
        pieChart = new PieChart();
        pieChart.setTitle("Incidentes por Prioridad");
        pieChart.setLegendSide(Side.BOTTOM);
        pieChart.setPrefWidth(350);
        pieChart.setPrefHeight(300);

        CategoryAxis ejeX = new CategoryAxis();
        ejeX.setLabel("Estado");

        NumberAxis ejeY = new NumberAxis();
        ejeY.setLabel("Cantidad");

        barChart = new BarChart<>(ejeX, ejeY);
        barChart.setTitle("Incidentes por Estado");
        barChart.setPrefWidth(400);
        barChart.setPrefHeight(300);
        barChart.setLegendVisible(false);

        HBox graficos = new HBox(20, pieChart, barChart);
        graficos.setPadding(new Insets(10, 0, 10, 0));
        return graficos;
    }

    /** Crea la tabla de recientes e inicializa tablaRecientes y datosRecientes. */
    @SuppressWarnings("unchecked")
    private VBox construirTablaRecientes() {
        Label lblTitulo = new Label("Incidentes Recientes");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        tablaRecientes = new TableView<>();
        tablaRecientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaRecientes.setPrefHeight(200);
        tablaRecientes.setPlaceholder(new Label("No hay incidentes recientes"));

        TableColumn<Incidente, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Incidente, String> colTituloCol = new TableColumn<>("Titulo");
        colTituloCol.setCellValueFactory(new PropertyValueFactory<>("titulo"));

        TableColumn<Incidente, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getEstado().name()));

        TableColumn<Incidente, String> colPrioridad = new TableColumn<>("Prioridad");
        colPrioridad.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getPrioridad().name()));

        TableColumn<Incidente, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(cd ->
            new SimpleObjectProperty<>(cd.getValue().getFechaCreacion().toLocalDate()));

        tablaRecientes.getColumns().addAll(colId, colTituloCol, colEstado, colPrioridad, colFecha);

        datosRecientes = FXCollections.observableArrayList();
        tablaRecientes.setItems(datosRecientes);

        VBox panel = new VBox(8, lblTitulo, tablaRecientes);
        panel.setPadding(new Insets(10, 0, 0, 0));
        return panel;
    }
}
